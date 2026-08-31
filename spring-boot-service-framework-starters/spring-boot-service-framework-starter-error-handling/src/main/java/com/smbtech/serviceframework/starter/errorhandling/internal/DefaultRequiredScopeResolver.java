package com.smbtech.serviceframework.starter.errorhandling.internal;

import com.smbtech.serviceframework.starter.errorhandling.api.security.RequiredScopeResolver;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Objects;
import java.util.Set;
import org.springframework.security.core.Authentication;

/**
 * Safe default that omits required scopes when the authorization policy does not expose them
 * explicitly.
 */
final class DefaultRequiredScopeResolver implements RequiredScopeResolver {

    /** Creates the safe default required-scope resolver. */
    public DefaultRequiredScopeResolver() {}

    /** Returns no required scopes. Granted authorities are deliberately ignored. */
    @Override
    public Set<String> resolve(HttpServletRequest request, Authentication authentication) {
        Objects.requireNonNull(request, "request must not be null");
        return Set.of();
    }
}
