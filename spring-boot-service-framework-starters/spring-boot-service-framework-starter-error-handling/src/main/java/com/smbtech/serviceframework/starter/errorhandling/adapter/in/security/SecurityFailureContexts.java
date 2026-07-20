package com.smbtech.serviceframework.starter.errorhandling.adapter.in.security;

import com.smbtech.serviceframework.starter.errorhandling.api.security.RequiredScopeResolver;
import com.smbtech.serviceframework.starter.errorhandling.api.security.SecurityFailureContext;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Objects;
import java.util.Set;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerMapping;

final class SecurityFailureContexts {

    private SecurityFailureContexts() {}

    static SecurityFailureContext authentication(HttpServletRequest request, Throwable failure) {
        return create(request, failure, Set.of());
    }

    static SecurityFailureContext authorization(
            HttpServletRequest request,
            Throwable failure,
            RequiredScopeResolver requiredScopeResolver) {
        Authentication authentication = currentAuthentication();
        Set<String> requiredScopes =
                Objects.requireNonNull(
                                requiredScopeResolver, "requiredScopeResolver must not be null")
                        .resolve(request, authentication);
        return create(request, failure, requiredScopes, authentication);
    }

    private static SecurityFailureContext create(
            HttpServletRequest request, Throwable failure, Set<String> requiredScopes) {
        return create(request, failure, requiredScopes, currentAuthentication());
    }

    private static SecurityFailureContext create(
            HttpServletRequest request,
            Throwable failure,
            Set<String> requiredScopes,
            Authentication authentication) {
        boolean bearerCredentialsPresent = hasBearerCredentials(request);
        return new SecurityFailureContext(
                failure,
                request.getMethod(),
                routeTemplate(request),
                "",
                bearerCredentialsPresent,
                authenticationType(request, authentication, bearerCredentialsPresent),
                requiredScopes);
    }

    private static Authentication currentAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    private static String authenticationType(
            HttpServletRequest request,
            Authentication authentication,
            boolean bearerCredentialsPresent) {
        if (bearerCredentialsPresent || isBearerAuthentication(authentication)) {
            return "bearer";
        }
        if (hasAuthorizationScheme(request, "Basic")) {
            return "basic";
        }
        if (authentication instanceof AnonymousAuthenticationToken) {
            return "anonymous";
        }
        if (authentication != null && authentication.isAuthenticated()) {
            return "session";
        }
        return "";
    }

    private static boolean isBearerAuthentication(Authentication authentication) {
        for (Class<?> type = authentication == null ? null : authentication.getClass();
                type != null;
                type = type.getSuperclass()) {
            String name = type.getName();
            if (name.equals(
                            "org.springframework.security.oauth2.server.resource.authentication."
                                    + "AbstractOAuth2TokenAuthenticationToken")
                    || name.equals(
                            "org.springframework.security.oauth2.server.resource.authentication."
                                    + "BearerTokenAuthenticationToken")) {
                return true;
            }
        }
        return false;
    }

    private static String routeTemplate(HttpServletRequest request) {
        Object route = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        return route == null ? "" : route.toString();
    }

    private static boolean hasBearerCredentials(HttpServletRequest request) {
        return hasAuthorizationScheme(request, "Bearer");
    }

    private static boolean hasAuthorizationScheme(HttpServletRequest request, String scheme) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null) {
            return false;
        }
        String value = authorization.stripLeading();
        int schemeLength = scheme.length();
        return value.length() >= schemeLength
                && value.regionMatches(true, 0, scheme, 0, schemeLength)
                && (value.length() == schemeLength
                        || Character.isWhitespace(value.charAt(schemeLength)));
    }
}
