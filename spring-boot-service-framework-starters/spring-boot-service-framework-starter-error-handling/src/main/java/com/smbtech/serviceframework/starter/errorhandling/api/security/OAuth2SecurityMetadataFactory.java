package com.smbtech.serviceframework.starter.errorhandling.api.security;

import com.smbtech.serviceframework.error.metadata.StandardErrorMetadata;

/** Creates safe standard metadata for a resolved Spring Security failure. */
@FunctionalInterface
public interface OAuth2SecurityMetadataFactory {

    /**
     * Creates safe metadata for the response and Bearer challenge.
     *
     * @param context safe security failure context
     * @param resolution resolved security failure
     * @return standard safe error metadata
     */
    StandardErrorMetadata create(
            SecurityFailureContext context, SecurityFailureResolution resolution);
}
