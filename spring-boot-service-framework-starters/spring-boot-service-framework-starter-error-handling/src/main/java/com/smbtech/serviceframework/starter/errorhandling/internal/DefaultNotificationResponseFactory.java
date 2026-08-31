package com.smbtech.serviceframework.starter.errorhandling.internal;

import com.smbtech.serviceframework.commons.notification.Notification;
import com.smbtech.serviceframework.commons.notification.NotificationSeverity;
import com.smbtech.serviceframework.error.ErrorExposure;
import com.smbtech.serviceframework.error.FieldViolation;
import com.smbtech.serviceframework.error.NotificationSanitizer;
import com.smbtech.serviceframework.error.ResolvedError;
import com.smbtech.serviceframework.error.ThrowableErrorResolver;
import com.smbtech.serviceframework.error.metadata.StandardErrorMetadata;
import com.smbtech.serviceframework.error.metadata.StandardErrorMetadataKeys;
import com.smbtech.serviceframework.starter.errorhandling.api.NotificationHttpStatusResolver;
import com.smbtech.serviceframework.starter.errorhandling.api.NotificationResponseFactory;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/** Creates sanitized JSON notification responses using category-based statuses. */
final class DefaultNotificationResponseFactory implements NotificationResponseFactory {

    private static final Set<String> PUBLIC_METADATA_ALLOWLIST =
            Set.of(StandardErrorMetadataKeys.CATEGORY, StandardErrorMetadataKeys.CORRELATION_ID);

    private static final NotificationSanitizer PUBLIC_NOTIFICATION_SANITIZER =
            NotificationSanitizer.withMetadataAllowlist(PUBLIC_METADATA_ALLOWLIST);

    private static final Set<String> NON_RESPONSE_METADATA_KEYS =
            Set.of("diagnostic", "diagnosticmessage");

    private final NotificationHttpStatusResolver statusResolver;

    private final NotificationSanitizer notificationSanitizer;

    private final boolean includeFieldViolations;

    /** Creates a response factory with safe framework defaults. */
    DefaultNotificationResponseFactory() {
        this(
                new DefaultNotificationHttpStatusResolver(),
                NotificationSanitizer.defaultSanitizer(),
                true);
    }

    /**
     * Creates a response factory with replaceable policies.
     *
     * @param statusResolver HTTP status resolver
     * @param notificationSanitizer response notification sanitizer
     */
    DefaultNotificationResponseFactory(
            NotificationHttpStatusResolver statusResolver,
            NotificationSanitizer notificationSanitizer) {
        this(statusResolver, notificationSanitizer, true);
    }

    /**
     * Creates a response factory with replaceable policies.
     *
     * @param statusResolver HTTP status resolver
     * @param notificationSanitizer response notification sanitizer
     * @param includeFieldViolations whether validation violations are included
     */
    DefaultNotificationResponseFactory(
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
                        ? publicNotification(source)
                        : internalNotification(
                                source, source.notification(), includeFieldViolations);
        Notification sanitized =
                Objects.requireNonNull(
                        notificationSanitizer.sanitize(responseNotification),
                        "notificationSanitizer must not return null");
        Notification safeNotification =
                source.exposure() == ErrorExposure.PUBLIC
                        ? publicNotification(source)
                        : internalNotification(source, sanitized, includeFieldViolations);
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

    private static Notification publicNotification(ResolvedError resolvedError) {
        Notification source = resolvedError.notification();
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put(StandardErrorMetadataKeys.CATEGORY, resolvedError.category().name());
        copyText(source.metadata(), metadata, StandardErrorMetadataKeys.CORRELATION_ID);
        Notification minimal =
                copy(
                        source,
                        source.code(),
                        ThrowableErrorResolver.DEFAULT_PUBLIC_MESSAGE,
                        source.severity(),
                        "",
                        metadata);
        return PUBLIC_NOTIFICATION_SANITIZER.sanitize(minimal);
    }

    private static Notification internalNotification(
            ResolvedError resolvedError,
            Notification sanitizedNotification,
            boolean includeFieldViolations) {
        Notification source = resolvedError.notification();
        Map<String, Object> metadata = new LinkedHashMap<>(sanitizedNotification.metadata());
        removeNonResponseMetadata(metadata);
        metadata.put(
                StandardErrorMetadataKeys.SCHEMA_VERSION,
                StandardErrorMetadata.CURRENT_SCHEMA_VERSION);
        metadata.put(StandardErrorMetadataKeys.CATEGORY, resolvedError.category().name());
        metadata.remove(StandardErrorMetadataKeys.CORRELATION_ID);
        copyText(source.metadata(), metadata, StandardErrorMetadataKeys.CORRELATION_ID);
        metadata.remove(StandardErrorMetadataKeys.VIOLATIONS);
        if (includeFieldViolations && resolvedError.hasFieldViolations()) {
            metadata.put(
                    StandardErrorMetadataKeys.VIOLATIONS,
                    resolvedError.fieldViolations().stream()
                            .map(DefaultNotificationResponseFactory::violationMetadata)
                            .toList());
        }
        Notification detailed =
                copy(
                        source,
                        source.code(),
                        sanitizedNotification.message(),
                        source.severity(),
                        source.fieldName(),
                        metadata);
        return NotificationSanitizer.withMetadataAllowlist(detailed.metadata().keySet())
                .sanitize(detailed);
    }

    private static void removeNonResponseMetadata(Map<String, Object> metadata) {
        metadata.keySet().removeIf(key -> NON_RESPONSE_METADATA_KEYS.contains(normalizeKey(key)));
    }

    private static String normalizeKey(String key) {
        return Objects.requireNonNullElse(key, "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]", "");
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
        metadata.put(StandardErrorMetadataKeys.Violation.FIELD_NAME, violation.fieldName());
        metadata.put(StandardErrorMetadataKeys.Violation.CODE, violation.code());
        metadata.put(StandardErrorMetadataKeys.Violation.MESSAGE, violation.message());
        return Map.copyOf(metadata);
    }
}
