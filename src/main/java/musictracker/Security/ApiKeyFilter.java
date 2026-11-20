package musictracker.Security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import musictracker.Exception.FilterErrorWriter;
import musictracker.Repository.ApiKeyRepository;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
@Order(2)
public class ApiKeyFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-API-Key";

    private final ApiKeyRepository apiKeyRepository;

    public ApiKeyFilter(ApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !ProtectedRoutes.requiresKey(request.getMethod(), request.getServletPath());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String rawKey = request.getHeader(HEADER);
        if (rawKey == null || rawKey.isBlank()) {
            reject(request, response, "Esta rota exige uma chave de API no cabeçalho " + HEADER + ".");
            return;
        }

        Optional<Long> userId = apiKeyRepository.findActiveUserIdByKeyHash(ApiKeys.hash(rawKey.trim()));
        if (userId.isEmpty()) {
            reject(request, response, "Chave de API inválida ou revogada.");
            return;
        }

        request.setAttribute(AuthenticatedUser.ATTRIBUTE, userId.get());
        filterChain.doFilter(request, response);
    }

    private void reject(HttpServletRequest request, HttpServletResponse response, String message)
            throws IOException {
        response.setHeader("WWW-Authenticate", "ApiKey");
        FilterErrorWriter.write(request, response, HttpStatus.UNAUTHORIZED, message);
    }
}
