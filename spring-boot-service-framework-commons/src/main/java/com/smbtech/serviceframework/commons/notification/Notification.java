package com.smbtech.serviceframework.commons.notification;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Framework-neutral description of an error, warning, or informational event that can be attached
 * to exceptions or API responses.
 *
 * @param code stable machine-readable notification code
 * @param message human-readable message
 * @param severity notification severity
 * @param fieldName related request or payload field, when applicable
 * @param metadata additional structured context
 * @param id unique notification instance identifier
 * @param timestamp creation timestamp
 */
public record Notification(
        String code,
        String message,
        NotificationSeverity severity,
        String fieldName,
        Map<String, Object> metadata,
        UUID id,
        Instant timestamp)
        implements Serializable {

    @Serial private static final long serialVersionUID = 4205965973373139208L;

    /**
     * Creates and normalizes a notification.
     *
     * @param code stable machine-readable notification code
     * @param message human-readable message; {@code null} becomes empty
     * @param severity notification severity; inferred from {@code code} when {@code null}
     * @param fieldName related field; {@code null} becomes empty
     * @param metadata structured metadata; {@code null} becomes empty
     * @param id notification identifier; generated when {@code null}
     * @param timestamp creation time; generated when {@code null}
     */
    public Notification {
        code = requireText(code, "code");
        message = Objects.requireNonNullElse(message, "");
        severity = Objects.requireNonNullElse(severity, NotificationSeverity.fromCode(code));
        fieldName = Objects.requireNonNullElse(fieldName, "");
        metadata = immutableMetadata(metadata);
        id = Objects.requireNonNullElseGet(id, UUID::randomUUID);
        timestamp = Objects.requireNonNullElseGet(timestamp, Instant::now);
    }

    /**
     * Creates an error notification.
     *
     * @param code stable notification code
     * @param message human-readable message
     * @return notification
     */
    public static Notification error(String code, String message) {
        return builder().code(code).message(message).severity(NotificationSeverity.ERROR).build();
    }

    /**
     * Creates a warning notification.
     *
     * @param code stable notification code
     * @param message human-readable message
     * @return notification
     */
    public static Notification warning(String code, String message) {
        return builder().code(code).message(message).severity(NotificationSeverity.WARNING).build();
    }

    /**
     * Creates an informational notification.
     *
     * @param code stable notification code
     * @param message human-readable message
     * @return notification
     */
    public static Notification info(String code, String message) {
        return builder().code(code).message(message).severity(NotificationSeverity.INFO).build();
    }

    /**
     * Starts a notification builder.
     *
     * @return builder
     */
    public static Builder builder() {
        return new Builder();
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Notification " + field + " must not be blank");
        }
        return value;
    }

    private static Map<String, Object> immutableMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return Map.of();
        }

        Map<String, Object> copy = new LinkedHashMap<>();
        metadata.forEach(
                (key, value) -> {
                    if (key == null || key.isBlank()) {
                        throw new IllegalArgumentException(
                                "Notification metadata keys must not be blank");
                    }
                    copy.put(
                            key,
                            Objects.requireNonNull(
                                    value, "Notification metadata value must not be null"));
                });
        return ImmutableNotificationMetadata.copy(copy);
    }

    /**
     * Returns a notification with replacement metadata while preserving its identity and timestamp.
     *
     * @param metadata replacement structured metadata
     * @return updated immutable notification
     */
    public Notification withMetadata(Map<String, Object> metadata) {
        return new Notification(code, message, severity, fieldName, metadata, id, timestamp);
    }

    /** Builder for {@link Notification}. */
    public static final class Builder {
        private String code;
        private String message;
        private NotificationSeverity severity;
        private String fieldName;
        private Map<String, Object> metadata = new LinkedHashMap<>();
        private UUID id;
        private Instant timestamp;

        private Builder() {}

        /**
         * Sets the stable notification code.
         *
         * @param code notification code
         * @return this builder
         */
        public Builder code(String code) {
            this.code = code;
            return this;
        }

        /**
         * Sets the public message.
         *
         * @param message notification message
         * @return this builder
         */
        public Builder message(String message) {
            this.message = message;
            return this;
        }

        /**
         * Sets the notification severity.
         *
         * @param severity severity to use
         * @return this builder
         */
        public Builder severity(NotificationSeverity severity) {
            this.severity = severity;
            return this;
        }

        /**
         * Associates the notification with a field.
         *
         * @param fieldName related field name
         * @return this builder
         */
        public Builder fieldName(String fieldName) {
            this.fieldName = fieldName;
            return this;
        }

        /**
         * Replaces all structured metadata.
         *
         * @param metadata metadata to copy
         * @return this builder
         */
        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = new LinkedHashMap<>(Objects.requireNonNullElse(metadata, Map.of()));
            return this;
        }

        /**
         * Adds or replaces one metadata entry.
         *
         * @param key metadata key
         * @param value metadata value
         * @return this builder
         */
        public Builder metadataEntry(String key, Object value) {
            this.metadata.put(key, value);
            return this;
        }

        /**
         * Sets the notification instance identifier.
         *
         * @param id notification identifier
         * @return this builder
         */
        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        /**
         * Sets the notification creation time.
         *
         * @param timestamp creation time
         * @return this builder
         */
        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        /**
         * Builds an immutable notification.
         *
         * @return constructed notification
         */
        public Notification build() {
            return new Notification(code, message, severity, fieldName, metadata, id, timestamp);
        }
    }
}
