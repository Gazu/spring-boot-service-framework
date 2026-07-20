package com.smbtech.serviceframework.starter.errorhandling.api.security;

import com.smbtech.serviceframework.error.metadata.OAuth2ErrorMetadata;
import java.util.Collection;
import java.util.Objects;
import java.util.TreeSet;

/**
 * OAuth2 error selected during security resolution. Public descriptions and documentation URIs are
 * intentionally added later by the metadata factory.
 *
 * @param error RFC 6750 error code, or an empty string when no error is exposed
 * @param scope space-delimited required scopes for {@code insufficient_scope}
 */
public record OAuth2SecurityError(String error, String scope) {

    private static final OAuth2SecurityError NONE = new OAuth2SecurityError("", "");

    /** Creates and validates an OAuth2 security error. */
    public OAuth2SecurityError {
        error = optionalText(error);
        scope = optionalText(scope);
        if (error.isEmpty()) {
            if (!scope.isEmpty()) {
                throw new IllegalArgumentException("OAuth2 scope requires an error code");
            }
        } else if (!OAuth2ErrorMetadata.SUPPORTED_ERROR_CODES.contains(error)) {
            throw new IllegalArgumentException("Unsupported public OAuth2 error: " + error);
        } else if (!"insufficient_scope".equals(error) && !scope.isEmpty()) {
            throw new IllegalArgumentException("OAuth2 scope is only valid for insufficient_scope");
        } else if (!scope.isEmpty()) {
            scope = normalizeScope(scope);
        }
    }

    /**
     * Returns the value representing an omitted OAuth2 error.
     *
     * @return result
     */
    public static OAuth2SecurityError none() {
        return NONE;
    }

    /**
     * Returns an RFC 6750 invalid request error.
     *
     * @return result
     */
    public static OAuth2SecurityError invalidRequest() {
        return new OAuth2SecurityError("invalid_request", "");
    }

    /**
     * Returns an RFC 6750 invalid token error.
     *
     * @return result
     */
    public static OAuth2SecurityError invalidToken() {
        return new OAuth2SecurityError("invalid_token", "");
    }

    /**
     * Returns an RFC 6750 insufficient scope error without a known required scope.
     *
     * @return result
     */
    public static OAuth2SecurityError insufficientScope() {
        return new OAuth2SecurityError("insufficient_scope", "");
    }

    /**
     * Returns an RFC 6750 insufficient scope error with normalized required scopes.
     *
     * @param requiredScopes scopes required by the protected operation
     * @return insufficient-scope error
     */
    public static OAuth2SecurityError insufficientScope(Collection<String> requiredScopes) {
        if (requiredScopes == null || requiredScopes.isEmpty()) {
            return insufficientScope();
        }
        TreeSet<String> normalized = new TreeSet<>();
        for (String scope : requiredScopes) {
            String value = optionalText(scope);
            if (value.isEmpty()) {
                throw new IllegalArgumentException("required scope must not be blank");
            }
            validateScopeToken(value);
            normalized.add(value);
        }
        return new OAuth2SecurityError("insufficient_scope", String.join(" ", normalized));
    }

    /**
     * Returns whether an OAuth2 error should be exposed.
     *
     * @return result
     */
    public boolean isPresent() {
        return !error.isEmpty();
    }

    private static String optionalText(String value) {
        return Objects.requireNonNullElse(value, "").trim();
    }

    private static void validateScopeToken(String scope) {
        for (int index = 0; index < scope.length(); index++) {
            char character = scope.charAt(index);
            boolean valid =
                    character == 0x21
                            || character >= 0x23 && character <= 0x5B
                            || character >= 0x5D && character <= 0x7E;
            if (!valid) {
                throw new IllegalArgumentException(
                        "required scope contains an invalid OAuth2 character");
            }
        }
    }

    private static String normalizeScope(String scope) {
        TreeSet<String> normalized = new TreeSet<>();
        for (String token : scope.split(" ", -1)) {
            if (token.isEmpty()) {
                throw new IllegalArgumentException(
                        "OAuth2 scope must use single spaces between values");
            }
            validateScopeToken(token);
            normalized.add(token);
        }
        return String.join(" ", normalized);
    }
}
