package musictracker.Security;

import java.util.List;

public final class ProtectedRoutes {

    private static final List<String> DOCS_PREFIXES = List.of("/swagger-ui", "/v3/api-docs", "/favicon");
    private static final List<String> OPEN_READ_PREFIXES = List.of("/artists", "/tracks", "/playlists");

    private ProtectedRoutes() {
    }

    public static boolean requiresKey(String method, String path) {
        if ("OPTIONS".equalsIgnoreCase(method) || startsWithAny(path, DOCS_PREFIXES)) {
            return false;
        }
        if ("POST".equalsIgnoreCase(method) && (path.equals("/users") || path.equals("/api-keys"))) {
            return false;
        }
        boolean read = "GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method);
        return !(read && startsWithAny(path, OPEN_READ_PREFIXES));
    }

    private static boolean startsWithAny(String path, List<String> prefixes) {
        return prefixes.stream().anyMatch(path::startsWith);
    }
}
