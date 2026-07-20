package com.smbtech.serviceframework.starter.errorhandling.adapter.in.web;

import com.smbtech.serviceframework.commons.notification.Notification;
import com.smbtech.serviceframework.commons.notification.NotificationSeverity;
import com.smbtech.serviceframework.error.DefaultNotificationSanitizer;
import com.smbtech.serviceframework.error.ErrorExposure;
import com.smbtech.serviceframework.error.FallbackThrowableErrorResolver;
import com.smbtech.serviceframework.error.FieldViolation;
import com.smbtech.serviceframework.error.NotificationSanitizer;
import com.smbtech.serviceframework.error.ResolvedError;
import com.smbtech.serviceframework.error.metadata.StandardErrorMetadata;
import com.smbtech.serviceframework.error.metadata.StandardErrorMetadataKeys;
import com.smbtech.serviceframework.starter.errorhandling.api.NotificationHttpStatusResolver;
import com.smbtech.serviceframework.starter.errorhandling.api.NotificationResponseFactory;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/** Creates sanitized JSON notification responses using category-based statuses. */
public final class DefaultNotificationResponseFactory implements NotificationResponseFactory {

    private static final Set<String> INTERNAL_METADATA_ALLOWLIST =
            Set.of(
                    StandardErrorMetadataKeys.SCHEMA_VERSION,
                    StandardErrorMetadataKeys.CATEGORY,
                    StandardErrorMetadataKeys.CORRELATION_ID,
                    StandardErrorMetadataKeys.RETRYABLE,
                    StandardErrorMetadataKeys.REQUEST);

    private static final NotificationSanitizer INTERNAL_NOTIFICATION_SANITIZER =
            new DefaultNotificationSanitizer(INTERNAL_METADATA_ALLOWLIST);

    private final NotificationHttpStatusResolver statusResolver;

    private final NotificationSanitizer notificationSanitizer;

    private final boolean includeFieldViolations;

    /** Creates a response factory with safe framework defaults. */
    public DefaultNotificationResponseFactory() {
        this(new DefaultNotificationHttpStatusResolver(), new DefaultNotificationSanitizer(), true);
    }

    /**
     * Creates a response factory with replaceable policies.
     *
     * @param statusResolver HTTP status resolver
     * @param notificationSanitizer public notification sanitizer
     */
    public DefaultNotificationResponseFactory(
            NotificationHttpStatusResolver statusResolver,
            NotificationSanitizer notificationSanitizer) {
        this(statusResolver, notificationSanitizer, true);
    }

    /**
     * Creates a response factory with replaceable policies.
     *
     * @param statusResolver HTTP status resolver
     * @param notificationSanitizer public notification sanitizer
     * @param includeFieldViolations whether validation violations are included
     */
    public DefaultNotificationResponseFactory(
            NotificationHttpStatusResolver statusResolver,
            NotificationSanitizer notificationSanitizer,
            boolean includeFieldViolations) {
        this.statusResolver =
                Objects.requireNonNull(statusResolver, "statusResolver must not be null");
        this.notificationSanitizer =
                Objects.requireNonNull(
                        notificationSanitizer, "notificationSanitizer must not be null");
        this.includeFieldViolations = includeFieldViolations;
    }

    @Override
    public ResponseEntity<Notification> create(ResolvedError resolvedError) {
        ResolvedError source =
                Objects.requireNonNull(resolvedError, "resolvedError must not be null");
        Notification responseNotification =
                source.exposure() == ErrorExposure.PUBLIC
                        ? publicNotification(source, includeFieldViolations)
                        : internalNotification(source);
        Notification sanitized =
                Objects.requireNonNull(
                        notificationSanitizer.sanitize(responseNotification),
                        "notificationSanitizer must not return null");
        Notification safeNotification =
                source.exposure() == ErrorExposure.PUBLIC
                        ? redactPublicNotification(sanitized)
                        : internalNotification(
                                new ResolvedError(
                                        sanitized,
                                        source.category(),
                                        ErrorExposure.INTERNAL,
                                        source.diagnosticMessage()));
        return ResponseEntity.status(
                        Objects.requireNonNull(
                                statusResolver.resolve(source),
                                "statusResolver must not return null"))
                .contentType(MediaType.APPLICATION_JSON)
                .body(safeNotification);
    }

    /**
     * Returns the configured HTTP status resolver.
     *
     * @return HTTP status resolver
     */
    public NotificationHttpStatusResolver statusResolver() {
        return statusResolver;
    }

    /**
     * Returns the configured notification sanitizer.
     *
     * @return notification sanitizer
     */
    public NotificationSanitizer notificationSanitizer() {
        return notificationSanitizer;
    }

    private static Notification publicNotification(
            ResolvedError resolvedError, boolean includeFieldViolations) {
        if (!includeFieldViolations || !resolvedError.hasFieldViolations()) {
            return resolvedError.notification();
        }
        Notification source = resolvedError.notification();
        Map<String, Object> metadata = new LinkedHashMap<>(source.metadata());
        metadata.put(
                "violations",
                resolvedError.fieldViolations().stream()
                        .map(DefaultNotificationResponseFactory::violationMetadata)
                        .toList());
        return copy(
                source,
                source.code(),
                source.message(),
                source.severity(),
                source.fieldName(),
                metadata);
    }

    private static Notification internalNotification(ResolvedError resolvedError) {
        Notification source = resolvedError.notification();
        Notification internal =
                copy(
                        source,
                        FallbackThrowableErrorResolver.DEFAULT_ERROR_CODE,
                        FallbackThrowableErrorResolver.DEFAULT_PUBLIC_MESSAGE,
                        NotificationSeverity.ERROR,
                        "",
                        internalMetadata(resolvedError));
        return INTERNAL_NOTIFICATION_SANITIZER.sanitize(internal);
    }

    private static Notification redactPublicNotification(Notification notification) {
        return new DefaultNotificationSanitizer(notification.metadata().keySet())
                .sanitize(notification);
    }

    private static Map<String, Object> internalMetadata(ResolvedError resolvedError) {
        Map<String, Object> source = resolvedError.notification().metadata();
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put(
                StandardErrorMetadataKeys.SCHEMA_VERSION,
                StandardErrorMetadata.CURRENT_SCHEMA_VERSION);
        metadata.put(StandardErrorMetadataKeys.CATEGORY, resolvedError.category().name());
        copyText(source, metadata, StandardErrorMetadataKeys.CORRELATION_ID);
        if (source.get(StandardErrorMetadataKeys.RETRYABLE) instanceof Boolean retryable) {
            metadata.put(StandardErrorMetadataKeys.RETRYABLE, retryable);
        }
        Map<String, Object> request =
                safeRequestMetadata(source.get(StandardErrorMetadataKeys.REQUEST));
        if (!request.isEmpty()) {
            metadata.put(StandardErrorMetadataKeys.REQUEST, request);
        }
        return Map.copyOf(metadata);
    }

    private static Map<String, Object> safeRequestMetadata(Object value) {
        if (!(value instanceof Map<?, ?> source)) {
            return Map.of();
        }
        Map<String, Object> request = new LinkedHashMap<>();
        copyText(source, request, StandardErrorMetadataKeys.Request.METHOD);
        copyText(source, request, StandardErrorMetadataKeys.Request.ROUTE);
        copyText(source, request, StandardErrorMetadataKeys.Request.OPERATION_ID);
        return Map.copyOf(request);
    }

    private static void copyText(Map<?, ?> source, Map<String, Object> target, String key) {
        Object value = source.get(key);
        if (value instanceof CharSequence text && !text.isEmpty()) {
            target.put(key, text.toString());
        }
    }

    private static Notification copy(
            Notification source,
            String code,
            String message,
            NotificationSeverity severity,
            String fieldName,
            Map<String, Object> metadata) {
        return new Notification(
                code, message, severity, fieldName, metadata, source.id(), source.timestamp());
    }

    private static Map<String, Object> violationMetadata(FieldViolation violation) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("fieldName", violation.fieldName());
        metadata.put("code", violation.code());
        metadata.put("message", violation.message());
        return Map.copyOf(metadata);
    }
}
