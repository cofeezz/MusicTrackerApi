package musictracker.RateLimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import musictracker.Exception.FilterErrorWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

// Filtro que aplica o rate limit (token bucket) por IP em todas as requisições.
//
// Ele roda ANTES do DispatcherServlet, então as recusas dele não passam pelo
// GlobalExceptionHandler — por isso o 429 é escrito pelo FilterErrorWriter, no
// mesmo formato do APIError (timestamp, status, error, message, path).
//
// Cabeçalhos devolvidos em toda resposta:
//   X-RateLimit-Limit      -> capacidade do balde (rajada máxima)
//   X-RateLimit-Remaining  -> fichas que ainda restam
// E, quando bloqueia (429):
//   Retry-After            -> segundos até a próxima ficha estar disponível
// Ordem dos filtros da API: 1) rate limit, depois (nas próximas etapas)
// 2) chave de API e 3) idempotência. O rate limit vem primeiro para barrar
// excesso de requisições antes de gastar qualquer outro processamento.
@Component
@Order(1)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final long CLEANUP_INTERVAL_NANOS = TimeUnit.MINUTES.toNanos(1);

    private final ConcurrentHashMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();
    private final AtomicLong lastCleanup = new AtomicLong(System.nanoTime());

    private final boolean enabled;
    private final long capacity;
    private final double refillPerSecond;
    private final boolean trustForwardedFor;

    public RateLimitFilter(
            @Value("${ratelimit.enabled:true}") boolean enabled,
            @Value("${ratelimit.capacity:20}") long capacity,
            @Value("${ratelimit.refill-per-second:1}") double refillPerSecond,
            @Value("${ratelimit.trust-forwarded-for:false}") boolean trustForwardedFor) {
        this.enabled = enabled;
        this.capacity = capacity;
        this.refillPerSecond = refillPerSecond;
        this.trustForwardedFor = trustForwardedFor;
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

        TokenBucket bucket = buckets.computeIfAbsent(ClientIp.of(request, trustForwardedFor),
                ip -> new TokenBucket(capacity, refillPerSecond, System::nanoTime));
        TokenBucket.Result result = bucket.tryConsume();

        response.setHeader("X-RateLimit-Limit", String.valueOf(capacity));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(result.remaining()));

        if (result.allowed()) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setHeader("Retry-After", String.valueOf(result.retryAfterSeconds()));
        FilterErrorWriter.write(request, response, HttpStatus.TOO_MANY_REQUESTS,
                "Limite de requisições excedido. Tente novamente em "
                        + result.retryAfterSeconds() + " segundo(s).");
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
