package com.smbtech.serviceframework.error.metadata;

import com.smbtech.serviceframework.error.ErrorCategory;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Framework-neutral standard metadata attached to a public error notification. Only namespaces
 * relevant to the resolved error are included in {@link #toMap()}.
 *
 * @param schemaVersion metadata contract version
 * @param category resolved error category
 * @param correlationId public correlation identifier
 * @param retryable whether retrying is known to be appropriate
 * @param request safe request context
 * @param validation validation failure type
 * @param violations field violations kept at the existing top-level location
 * @param security security failure context
 * @param oauth2 RFC 6750 public metadata
 * @param resource logical resource context
 * @param conflict conflict context
 * @param dependency downstream dependency context
 * @param rateLimit retry timing context
 * @param http HTTP method or content-negotiation context
 */
public record StandardErrorMetadata(
        String schemaVersion,
        ErrorCategory category,
        @Nullable String correlationId,
        @Nullable Boolean retryable,
        @Nullable RequestErrorMetadata request,
        @Nullable ValidationErrorMetadata validation,
        List<FieldViolationMetadata> violations,
        @Nullable SecurityErrorMetadata security,
        @Nullable OAuth2ErrorMetadata oauth2,
        @Nullable ResourceErrorMetadata resource,
        @Nullable ConflictErrorMetadata conflict,
        @Nullable DependencyErrorMetadata dependency,
        @Nullable RateLimitErrorMetadata rateLimit,
        @Nullable HttpErrorMetadata http) {

    /** Value written to identify the shape of generated metadata. */
    public static final String CURRENT_SCHEMA_VERSION = "1";

    /**
     * Creates normalized standard error metadata.
     *
     * @param schemaVersion metadata schema version
     * @param category error category
     * @param correlationId correlation identifier
     * @param retryable whether retrying can succeed
     * @param request request metadata
     * @param validation validation metadata
     * @param violations field violations
     * @param security security metadata
     * @param oauth2 OAuth2 metadata
     * @param resource resource metadata
     * @param conflict conflict metadata
     * @param dependency dependency metadata
     * @param rateLimit rate-limit metadata
     * @param http HTTP metadata
     */
    public StandardErrorMetadata {
        schemaVersion = MetadataValues.requireText(schemaVersion, "metadata schema version");
        category = Objects.requireNonNull(category, "error category must not be null");
        correlationId = MetadataValues.optionalText(correlationId);
        List<FieldViolationMetadata> sourceViolations = violations == null ? List.of() : violations;
        if (sourceViolations.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("metadata violations must not contain null values");
        }
        violations = List.copyOf(sourceViolations);
    }

    /**
     * Starts a builder for the supplied error category.
     *
     * @param category error category
     * @return metadata builder
     */
    public static StandardErrorMetadataBuilder builder(ErrorCategory category) {
        return new StandardErrorMetadataBuilder(category);
    }

    /**
     * Returns the complete standard as deeply immutable notification metadata.
     *
     * @return result
     */
    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(StandardErrorMetadataKeys.SCHEMA_VERSION, schemaVersion);
        values.put(StandardErrorMetadataKeys.CATEGORY, category.name());
        putIfPresent(values, StandardErrorMetadataKeys.CORRELATION_ID, correlationId);
        if (retryable != null) {
            values.put(StandardErrorMetadataKeys.RETRYABLE, retryable);
        }
        putSection(
                values,
                StandardErrorMetadataKeys.REQUEST,
                request == null ? null : request.toMap());
        putSection(
                values,
                StandardErrorMetadataKeys.VALIDATION,
                validation == null ? null : validation.toMap());
        if (!violations.isEmpty()) {
            values.put(
                    StandardErrorMetadataKeys.VIOLATIONS,
                    violations.stream().map(FieldViolationMetadata::toMap).toList());
        }
        putSection(
                values,
                StandardErrorMetadataKeys.SECURITY,
                security == null ? null : security.toMap());
        putSection(
                values, StandardErrorMetadataKeys.OAUTH2, oauth2 == null ? null : oauth2.toMap());
        putSection(
                values,
                StandardErrorMetadataKeys.RESOURCE,
                resource == null ? null : resource.toMap());
        putSection(
                values,
                StandardErrorMetadataKeys.CONFLICT,
                conflict == null ? null : conflict.toMap());
        putSection(
                values,
                StandardErrorMetadataKeys.DEPENDENCY,
                dependency == null ? null : dependency.toMap());
        putSection(
                values,
                StandardErrorMetadataKeys.RATE_LIMIT,
                rateLimit == null ? null : rateLimit.toMap());
        putSection(values, StandardErrorMetadataKeys.HTTP, http == null ? null : http.toMap());
        return Collections.unmodifiableMap(values);
    }

    private static void putIfPresent(Map<String, Object> values, String key, String value) {
        if (!value.isEmpty()) {
            values.put(key, value);
        }
    }

    private static void putSection(
            Map<String, Object> values, String key, Map<String, Object> section) {
        if (section != null && !section.isEmpty()) {
            values.put(key, section);
        }
    }
}
