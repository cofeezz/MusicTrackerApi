package musictracker.RateLimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

// Filtro que aplica o rate limit (token bucket) por IP em todas as requisições.
//
// Ele roda ANTES do DispatcherServlet, então as exceções dele não passam pelo
// GlobalExceptionHandler — por isso o corpo do 429 é escrito aqui mesmo, no
// mesmo formato do APIError (timestamp, status, error, message, path).
//
// Cabeçalhos devolvidos em toda resposta:
//   X-RateLimit-Limit      -> capacidade do balde (rajada máxima)
//   X-RateLimit-Remaining  -> fichas que ainda restam
// E, quando bloqueia (429):
//   Retry-After            -> segundos até a próxima ficha estar disponível
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final long CLEANUP_INTERVAL_NANOS = TimeUnit.MINUTES.toNanos(1);

    private final ConcurrentHashMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();
    private final AtomicLong lastCleanup = new AtomicLong(System.nanoTime());

    private final boolean enabled;
    private final long capacity;
    private final double refillPerSecond;

    public RateLimitFilter(
            @Value("${ratelimit.enabled:true}") boolean enabled,
            @Value("${ratelimit.capacity:20}") long capacity,
            @Value("${ratelimit.refill-per-second:1}") double refillPerSecond) {
        this.enabled = enabled;
        this.capacity = capacity;
        this.refillPerSecond = refillPerSecond;
    }

    // A documentação (Swagger UI e o JSON do OpenAPI) não entra na contagem
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !enabled || path.startsWith("/swagger-ui") || path.startsWith("/v3/api-docs");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        removeIdleBucketsIfDue();

        TokenBucket bucket = buckets.computeIfAbsent(request.getRemoteAddr(),
                ip -> new TokenBucket(capacity, refillPerSecond, System::nanoTime));
        TokenBucket.Result result = bucket.tryConsume();

        response.setHeader("X-RateLimit-Limit", String.valueOf(capacity));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(result.remaining()));

        if (result.allowed()) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setHeader("Retry-After", String.valueOf(result.retryAfterSeconds()));
        writeTooManyRequests(request, response, result.retryAfterSeconds());
    }

    private void writeTooManyRequests(HttpServletRequest request, HttpServletResponse response,
                                      long retryAfterSeconds) throws IOException {
        HttpStatus status = HttpStatus.TOO_MANY_REQUESTS;
        String message = "Limite de requisições excedido. Tente novamente em "
                + retryAfterSeconds + " segundo(s).";

        String body = """
                {"timestamp":"%s","status":%d,"error":"%s","message":"%s","path":"%s"}"""
                .formatted(
                        LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS),
                        status.value(),
                        status.name(),
                        escapeJson(message),
                        escapeJson(request.getRequestURI()));

        response.setStatus(status.value());
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(body);
    }

    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    // Faxina: de tempos em tempos descarta os baldes de clientes que já voltaram
    // ao estado cheio, para o mapa não crescer para sempre com IPs antigos.
    private void removeIdleBucketsIfDue() {
        long now = System.nanoTime();
        long last = lastCleanup.get();
        if (now - last >= CLEANUP_INTERVAL_NANOS && lastCleanup.compareAndSet(last, now)) {
            buckets.values().removeIf(TokenBucket::isFull);
        }
    }
}