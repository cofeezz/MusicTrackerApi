package musictracker.Security;

import musictracker.Exception.UnauthorizedException;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

public final class AuthenticatedUser {

    public static final String ATTRIBUTE = "authenticatedUserId";

    private AuthenticatedUser() {
    }

    public static Long requireId() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        Object id = attributes == null ? null : attributes.getAttribute(ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
        if (id instanceof Long userId) {
            return userId;
        }
        throw new UnauthorizedException("Autenticação necessária: envie uma chave válida no cabeçalho X-API-Key.");
    }
}
