package musictracker.Idempotency;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import musictracker.Exception.FilterErrorWriter;
import musictracker.RateLimit.ClientIp;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Order(3)
public class IdempotencyFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Idempotency-Key";
    private static final int MAX_KEY_LENGTH = 100;
    private static final long CLEANUP_INTERVAL_NANOS = TimeUnit.MINUTES.toNanos(1);

    // Resposta guardada (ou "em processamento") de uma chave
    private static final class Entry {
        final String fingerprint;
        final long createdAt = System.nanoTime();

        int status;
        String contentType;
        String location;
        byte[] body;
        volatile boolean completed; // escrito por último: publica os campos acima

        Entry(String fingerprint) {
            this.fingerprint = fingerprint;
        }

        void complete(int status, String contentType, String location, byte[] body) {
            this.status = status;
            this.contentType = contentType;
            this.location = location;
            this.body = body;
            this.completed = true;
        }
    }

    private final ConcurrentHashMap<String, Entry> entries = new ConcurrentHashMap<>();
    private final AtomicLong lastCleanup = new AtomicLong(System.nanoTime());

    private final boolean enabled;
    private final long ttlNanos;
    private final boolean trustForwardedFor;

    public IdempotencyFilter(
            @Value("${idempotency.enabled:true}") boolean enabled,
            @Value("${idempotency.ttl-hours:24}") long ttlHours,
            @Value("${ratelimit.trust-forwarded-for:false}") boolean trustForwardedFor) {
        this.enabled = enabled;
        this.ttlNanos = TimeUnit.HOURS.toNanos(ttlHours);
        this.trustForwardedFor = trustForwardedFor;
    }

    // Só POST com o cabeçalho presente entra na lógica de idempotência
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !enabled
                || !"POST".equalsIgnoreCase(request.getMethod())
                || request.getHeader(HEADER) == null;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        removeExpiredIfDue();

        String key = request.getHeader(HEADER).trim();
        if (key.isEmpty() || key.length() > MAX_KEY_LENGTH) {
            FilterErrorWriter.write(request, response, HttpStatus.BAD_REQUEST,
                    "O cabeçalho " + HEADER + " deve ter entre 1 e " + MAX_KEY_LENGTH + " caracteres.");
            return;
        }

        // Lê o corpo uma vez para poder compará-lo e ainda entregá-lo ao Controller
        byte[] body = request.getInputStream().readAllBytes();
        String fingerprint = fingerprint(request.getRequestURI(), body);

        // A chave é por cliente (IP + chave): dois clientes podem usar a mesma chave
        String scopedKey = ClientIp.of(request, trustForwardedFor) + "|" + key;

        Entry fresh = new Entry(fingerprint);
        Entry existing = entries.putIfAbsent(scopedKey, fresh);
        if (existing != null && isExpired(existing)) {
            entries.remove(scopedKey, existing);
            existing = entries.putIfAbsent(scopedKey, fresh);
        }

        if (existing != null) {
            handleRepeated(request, response, existing, fingerprint);
            return;
        }

        // Chave nova: processa e captura a resposta
        ContentCachingResponseWrapper wrapped = new ContentCachingResponseWrapper(response);
        boolean stored = false;
        try {
            filterChain.doFilter(new CachedBodyRequest(request, body), wrapped);

            int status = wrapped.getStatus();
            if (status >= 200 && status < 300) {
                fresh.complete(status, wrapped.getContentType(), wrapped.getHeader("Location"),
                        wrapped.getContentAsByteArray());
                stored = true;
            }
        } finally {
            if (!stored) {
                entries.remove(scopedKey, fresh);
            }
            wrapped.copyBodyToResponse();
        }
    }

    private void handleRepeated(HttpServletRequest request, HttpServletResponse response,
                                Entry existing, String fingerprint) throws IOException {
        if (!existing.fingerprint.equals(fingerprint)) {
            FilterErrorWriter.write(request, response, HttpStatus.UNPROCESSABLE_ENTITY,
                    "Esta chave de idempotência já foi usada em outra requisição (rota ou corpo diferente). "
                            + "Use uma chave nova para uma operação nova.");
            return;
        }

        if (!existing.completed) {
            FilterErrorWriter.write(request, response, HttpStatus.CONFLICT,
                    "Já existe uma requisição em processamento com esta chave de idempotência. "
                            + "Aguarde e tente novamente.");
            return;
        }

        // Repetição legítima: devolve exatamente a resposta da primeira vez
        response.setStatus(existing.status);
        if (existing.contentType != null) {
            response.setContentType(existing.contentType);
        }
        if (existing.location != null) {
            response.setHeader("Location", existing.location);
        }
        response.setHeader("Idempotent-Replayed", "true");
        response.getOutputStream().write(existing.body);
    }

    // "Impressão digital" da requisição: rota + corpo. Se a mesma chave chegar
    // com outra impressão, não é uma repetição — é um uso indevido da chave.
    private static String fingerprint(String uri, byte[] body) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(uri.getBytes(StandardCharsets.UTF_8));
            digest.update((byte) 0);
            digest.update(body);
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }

    private boolean isExpired(Entry entry) {
        return System.nanoTime() - entry.createdAt >= ttlNanos;
    }

    // Faxina periódica das chaves vencidas, para o mapa não crescer para sempre
    private void removeExpiredIfDue() {
        long now = System.nanoTime();
        long last = lastCleanup.get();
        if (now - last >= CLEANUP_INTERVAL_NANOS && lastCleanup.compareAndSet(last, now)) {
            entries.values().removeIf(this::isExpired);
        }
    }

    // O corpo de uma requisição só pode ser lido uma vez. Como o filtro já o leu
    // (para a impressão digital), este wrapper entrega a cópia ao Controller.
    private static final class CachedBodyRequest extends HttpServletRequestWrapper {

        private final byte[] body;

        CachedBodyRequest(HttpServletRequest request, byte[] body) {
            super(request);
            this.body = body;
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream source = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override
                public boolean isFinished() {
                    return source.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener listener) {
                    throw new UnsupportedOperationException("Leitura assíncrona não suportada");
                }

                @Override
                public int read() {
                    return source.read();
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            String encoding = getCharacterEncoding();
            Charset charset = encoding != null ? Charset.forName(encoding) : StandardCharsets.UTF_8;
            return new BufferedReader(new InputStreamReader(getInputStream(), charset));
        }
    }
}
