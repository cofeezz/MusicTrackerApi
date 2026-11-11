package musictracker.RateLimit;

import jakarta.servlet.http.HttpServletRequest;

public final class ClientIp {

    private ClientIp() {
    }

    public static String of(HttpServletRequest request, boolean trustForwardedFor) {
        if (trustForwardedFor) {
            String header = request.getHeader("X-Forwarded-For");
            if (header != null && !header.isBlank()) {
                String first = header.split(",")[0].trim();
                if (!first.isEmpty()) {
                    return first;
                }
            }
        }
        return request.getRemoteAddr();
    }
}
