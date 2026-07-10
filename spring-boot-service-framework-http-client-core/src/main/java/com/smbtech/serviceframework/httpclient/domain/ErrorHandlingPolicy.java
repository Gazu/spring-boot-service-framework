package com.smbtech.serviceframework.httpclient.domain;

public record ErrorHandlingPolicy(
        boolean enabled,
        boolean includeBody,
        int maxBodySize,
        boolean includeHeaders,
        boolean includeNotificationMetadata,
        String notificationCodePrefix
) {
    private static final int DEFAULT_MAX_BODY_SIZE = 4096;
    public static final String DEFAULT_NOTIFICATION_CODE_PREFIX = "E_SERVICE_FRAMEWORK_HTTP_CLIENT_";

    public static ErrorHandlingPolicy defaults() {
        return new ErrorHandlingPolicy(
                true,
                true,
                DEFAULT_MAX_BODY_SIZE,
                true,
                true,
                DEFAULT_NOTIFICATION_CODE_PREFIX
        );
    }

    public ErrorHandlingPolicy(
            boolean enabled,
            boolean includeBody,
            int maxBodySize
    ) {
        this(
                enabled,
                includeBody,
                maxBodySize,
                true,
                true,
                DEFAULT_NOTIFICATION_CODE_PREFIX
        );
    }

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
