package com.smbtech.serviceframework.httpclient.domain;

/**
 * Carries immutable error handling policy data.
 *
 * @param enabled enabled value
 * @param includeBody include body value
 * @param maxBodySize max body size value
 * @param includeHeaders include headers value
 * @param includeNotificationMetadata include notification metadata value
 * @param notificationCodePrefix notification code prefix value
 */
public record ErrorHandlingPolicy(
        boolean enabled,
        boolean includeBody,
        int maxBodySize,
        boolean includeHeaders,
        boolean includeNotificationMetadata,
        String notificationCodePrefix) {
    private static final int DEFAULT_MAX_BODY_SIZE = 4096;
    public static final String DEFAULT_NOTIFICATION_CODE_PREFIX =
            "E_SERVICE_FRAMEWORK_HTTP_CLIENT_";

    /**
     * Performs the defaults operation.
     *
     * @return defaults result
     */
    public static ErrorHandlingPolicy defaults() {
        return new ErrorHandlingPolicy(
                true, true, DEFAULT_MAX_BODY_SIZE, true, true, DEFAULT_NOTIFICATION_CODE_PREFIX);
    }

    /**
     * Creates a error handling policy instance.
     *
     * @param enabled enabled value
     * @param includeBody include body value
     * @param maxBodySize max body size value
     */
    public ErrorHandlingPolicy(boolean enabled, boolean includeBody, int maxBodySize) {
        this(enabled, includeBody, maxBodySize, true, true, DEFAULT_NOTIFICATION_CODE_PREFIX);
    }

    /** Creates and validates the record components. */
    public ErrorHandlingPolicy {
        maxBodySize = maxBodySize <= 0 ? DEFAULT_MAX_BODY_SIZE : maxBodySize;
        notificationCodePrefix = normalizePrefix(notificationCodePrefix);
    }

    private static String normalizePrefix(String notificationCodePrefix) {
        if (notificationCodePrefix == null || notificationCodePrefix.isBlank()) {
            return DEFAULT_NOTIFICATION_CODE_PREFIX;
        }
        String trimmed = notificationCodePrefix.trim();
        return trimmed.endsWith("_") ? trimmed : trimmed + "_";
    }
}
