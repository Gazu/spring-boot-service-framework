package com.smbtech.serviceframework.error.metadata;

import com.smbtech.serviceframework.error.ErrorCategory;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Builder for the standard public error metadata contract. */
public final class StandardErrorMetadataBuilder {

    private String schemaVersion = StandardErrorMetadata.CURRENT_SCHEMA_VERSION;
    private final ErrorCategory category;
    private String correlationId;
    private Boolean retryable;
    private RequestErrorMetadata request;
    private ValidationErrorMetadata validation;
    private final List<FieldViolationMetadata> violations = new ArrayList<>();
    private SecurityErrorMetadata security;
    private OAuth2ErrorMetadata oauth2;
    private ResourceErrorMetadata resource;
    private ConflictErrorMetadata conflict;
    private DependencyErrorMetadata dependency;
    private RateLimitErrorMetadata rateLimit;
    private HttpErrorMetadata http;

    /**
     * Creates a builder for the supplied error category.
     *
     * @param category error category
     */
    public StandardErrorMetadataBuilder(ErrorCategory category) {
        this.category = Objects.requireNonNull(category, "error category must not be null");
    }

    /**
     * Performs the schema version operation.
     *
     * @param schemaVersion schema version value
     * @return schema version result
     */
    public StandardErrorMetadataBuilder schemaVersion(String schemaVersion) {
        this.schemaVersion = schemaVersion;
        return this;
    }

    /**
     * Performs the correlation id operation.
     *
     * @param correlationId correlation id value
     * @return correlation id result
     */
    public StandardErrorMetadataBuilder correlationId(String correlationId) {
        this.correlationId = correlationId;
        return this;
    }

    /**
     * Performs the retryable operation.
     *
     * @param retryable retryable value
     * @return retryable result
     */
    public StandardErrorMetadataBuilder retryable(Boolean retryable) {
        this.retryable = retryable;
        return this;
    }

    /**
     * Performs the request operation.
     *
     * @param request request value
     * @return request result
     */
    public StandardErrorMetadataBuilder request(RequestErrorMetadata request) {
        this.request = request;
        return this;
    }

    /**
     * Performs the validation operation.
     *
     * @param validation validation value
     * @return validation result
     */
    public StandardErrorMetadataBuilder validation(ValidationErrorMetadata validation) {
        this.validation = validation;
        return this;
    }

    /**
     * Performs the violations operation.
     *
     * @param violations violations value
     * @return violations result
     */
    public StandardErrorMetadataBuilder violations(List<FieldViolationMetadata> violations) {
        this.violations.clear();
        if (violations != null) {
            this.violations.addAll(violations);
        }
        return this;
    }

    /**
     * Adds violation.
     *
     * @param violation violation value
     * @return add violation result
     */
    public StandardErrorMetadataBuilder addViolation(FieldViolationMetadata violation) {
        this.violations.add(Objects.requireNonNull(violation, "violation must not be null"));
        return this;
    }

    /**
     * Performs the security operation.
     *
     * @param security security value
     * @return security result
     */
    public StandardErrorMetadataBuilder security(SecurityErrorMetadata security) {
        this.security = security;
        return this;
    }

    /**
     * Sets the OAuth2 error metadata.
     *
     * @param oauth2 OAuth2 metadata
     * @return this builder
     */
    public StandardErrorMetadataBuilder oauth2(OAuth2ErrorMetadata oauth2) {
        this.oauth2 = oauth2;
        return this;
    }

    /**
     * Performs the resource operation.
     *
     * @param resource resource value
     * @return resource result
     */
    public StandardErrorMetadataBuilder resource(ResourceErrorMetadata resource) {
        this.resource = resource;
        return this;
    }

    /**
     * Performs the conflict operation.
     *
     * @param conflict conflict value
     * @return conflict result
     */
    public StandardErrorMetadataBuilder conflict(ConflictErrorMetadata conflict) {
        this.conflict = conflict;
        return this;
    }

    /**
     * Performs the dependency operation.
     *
     * @param dependency dependency value
     * @return dependency result
     */
    public StandardErrorMetadataBuilder dependency(DependencyErrorMetadata dependency) {
        this.dependency = dependency;
        return this;
    }

    /**
     * Performs the rate limit operation.
     *
     * @param rateLimit rate limit value
     * @return rate limit result
     */
    public StandardErrorMetadataBuilder rateLimit(RateLimitErrorMetadata rateLimit) {
        this.rateLimit = rateLimit;
        return this;
    }

    /**
     * Performs the http operation.
     *
     * @param http http value
     * @return http result
     */
    public StandardErrorMetadataBuilder http(HttpErrorMetadata http) {
        this.http = http;
        return this;
    }

    /**
     * Builds an immutable standard metadata value.
     *
     * @return result
     */
    public StandardErrorMetadata build() {
        return new StandardErrorMetadata(
                schemaVersion,
                category,
                correlationId,
                retryable,
                request,
                validation,
                violations,
                security,
                oauth2,
                resource,
                conflict,
                dependency,
                rateLimit,
                http);
    }

    /**
     * Builds the immutable metadata map accepted by a notification.
     *
     * @return result
     */
    public java.util.Map<String, Object> buildMap() {
        return build().toMap();
    }
}
