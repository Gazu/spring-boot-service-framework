package com.smbtech.serviceframework.starter.errorhandling.api.security;

import com.smbtech.serviceframework.error.metadata.StandardErrorMetadata;

/** Creates safe standard metadata for a resolved Spring Security failure. */
@FunctionalInterface
public interface OAuth2SecurityMetadataFactory {

    /**
     * Creates public metadata from safe context and resolved classification.
     *
     * @param context safe security failure context
     * @param resolution resolved public security failure
     * @return standard public error metadata
     */
    StandardErrorMetadata create(
            SecurityFailureContext context, SecurityFailureResolution resolution);
}
