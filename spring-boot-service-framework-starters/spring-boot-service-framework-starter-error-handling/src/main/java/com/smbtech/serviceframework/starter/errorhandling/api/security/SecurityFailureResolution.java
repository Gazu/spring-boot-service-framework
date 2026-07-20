package com.smbtech.serviceframework.starter.errorhandling.api.security;

import com.smbtech.serviceframework.error.ResolvedError;
import java.util.Objects;

/**
 * Immutable result returned by a security failure resolver.
 *
 * @param resolvedError notification, category, exposure, and internal diagnostic
 * @param reason stable security failure classification
 * @param oauth2Error optional RFC 6750 error data
 * @param bearerChallenge whether a Bearer {@code WWW-Authenticate} challenge is required
 */
public record SecurityFailureResolution(
        ResolvedError resolvedError,
        SecurityFailureReason reason,
        OAuth2SecurityError oauth2Error,
        boolean bearerChallenge) {

    /** Creates and validates an immutable security failure resolution. */
    public SecurityFailureResolution {
        resolvedError = Objects.requireNonNull(resolvedError, "resolvedError must not be null");
        reason = Objects.requireNonNull(reason, "security failure reason must not be null");
        oauth2Error = Objects.requireNonNullElseGet(oauth2Error, OAuth2SecurityError::none);
    }

    /**
     * Creates a resolution without an OAuth2 error or Bearer challenge.
     *
     * @param resolvedError resolved public and diagnostic error
     * @param reason normalized security failure reason
     */
    public SecurityFailureResolution(ResolvedError resolvedError, SecurityFailureReason reason) {
        this(resolvedError, reason, OAuth2SecurityError.none(), false);
    }

    /**
     * Returns whether this resolution exposes an OAuth2 error.
     *
     * @return result
     */
    public boolean hasOAuth2Error() {
        return oauth2Error.isPresent();
    }
}
