package com.smbtech.serviceframework.starter.errorhandling.api.security;

/** Stable response classification selected by the security failure resolvers. */
public enum SecurityFailureReason {
    /** Represents authentication required. */
    AUTHENTICATION_REQUIRED(
            SecurityErrorCatalog.AUTHENTICATION_REQUIRED, "authentication_required"),
    /** Represents bearer request invalid. */
    BEARER_REQUEST_INVALID(SecurityErrorCatalog.BEARER_REQUEST_INVALID, "invalid_request"),
    /** Represents bearer token invalid. */
    BEARER_TOKEN_INVALID(SecurityErrorCatalog.BEARER_TOKEN_INVALID, "invalid_token"),
    /** Represents authentication provider failure. */
    AUTHENTICATION_PROVIDER_FAILURE(
            SecurityErrorCatalog.AUTHENTICATION_PROVIDER_FAILURE, "provider_failure"),
    /** Represents access denied. */
    ACCESS_DENIED(SecurityErrorCatalog.ACCESS_DENIED, "access_denied"),
    /** Represents insufficient scope. */
    INSUFFICIENT_SCOPE(SecurityErrorCatalog.INSUFFICIENT_SCOPE, "insufficient_scope"),
    /** Represents csrf access denied. */
    CSRF_ACCESS_DENIED(SecurityErrorCatalog.CSRF_ACCESS_DENIED, "csrf_rejected");

    private final SecurityErrorCatalog errorDefinition;
    private final String metadataValue;

    SecurityFailureReason(SecurityErrorCatalog errorDefinition, String metadataValue) {
        this.errorDefinition = errorDefinition;
        this.metadataValue = metadataValue;
    }

    /**
     * Returns the public catalog entry associated with this reason.
     *
     * @return result
     */
    public SecurityErrorCatalog errorDefinition() {
        return errorDefinition;
    }

    /**
     * Returns the stable value used in detailed security metadata.
     *
     * @return result
     */
    public String metadataValue() {
        return metadataValue;
    }
}
