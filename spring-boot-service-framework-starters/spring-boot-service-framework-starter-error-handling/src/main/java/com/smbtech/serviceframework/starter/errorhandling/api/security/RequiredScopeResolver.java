package com.smbtech.serviceframework.starter.errorhandling.api.security;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Set;
import org.springframework.security.core.Authentication;

/**
 * Resolves scopes required by the protected operation. Implementations must derive them from
 * authorization policy, route, or operation metadata and must never return scopes granted to the
 * current authentication as a substitute.
 */
@FunctionalInterface
public interface RequiredScopeResolver {

    /**
     * Resolves required scopes for the current request.
     *
     * @param request current HTTP request
     * @param authentication current authentication, which may be {@code null}
     * @return required scopes, or an empty set when they cannot be determined
     */
    Set<String> resolve(HttpServletRequest request, Authentication authentication);
}
