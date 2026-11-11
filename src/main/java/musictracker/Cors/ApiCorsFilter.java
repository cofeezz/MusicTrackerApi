package musictracker.Cors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

@Component
@Order(0)
public class ApiCorsFilter extends CorsFilter {

    public ApiCorsFilter(
            @Value("${cors.allowed-origins}") List<String> allowedOrigins,
            @Value("${cors.max-age-seconds:3600}") long maxAgeSeconds) {
        super(source(allowedOrigins, maxAgeSeconds));
    }

    private static UrlBasedCorsConfigurationSource source(List<String> allowedOrigins, long maxAgeSeconds) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins.stream().map(String::trim).toList());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of(
                "Content-Type", "Accept", "X-API-Key", "X-Idempotency-Key", "X-API-Version"));
        configuration.setExposedHeaders(List.of(
                "Location", "Retry-After", "X-RateLimit-Limit", "X-RateLimit-Remaining",
                "Idempotent-Replayed", "WWW-Authenticate"));
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(maxAgeSeconds);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}