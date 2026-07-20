package com.smbtech.serviceframework.starter.errorhandling.api.security;

/**
 * Resolves an authentication failure into the framework's public security contract while preserving
 * internal diagnostics separately.
 */
@FunctionalInterface
public interface SecurityAuthenticationFailureResolver {

    /**
     * Resolves an authentication failure.
     *
     * @param context safe authentication failure context
     * @return security failure resolution
     */
    SecurityFailureResolution resolve(SecurityFailureContext context);
}
