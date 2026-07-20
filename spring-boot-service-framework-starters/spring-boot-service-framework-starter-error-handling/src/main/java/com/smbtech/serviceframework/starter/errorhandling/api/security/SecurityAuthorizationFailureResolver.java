package com.smbtech.serviceframework.starter.errorhandling.api.security;

/**
 * Resolves an authorization failure into the framework's public security contract while preserving
 * internal diagnostics separately.
 */
@FunctionalInterface
public interface SecurityAuthorizationFailureResolver {

    /**
     * Resolves an authorization failure.
     *
     * @param context safe authorization failure context
     * @return security failure resolution
     */
    SecurityFailureResolution resolve(SecurityFailureContext context);
}
