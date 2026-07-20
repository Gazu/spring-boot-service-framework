package com.smbtech.serviceframework.commons.notification;

import java.util.Locale;

/** Describes how a notification should be interpreted by callers. */
public enum NotificationSeverity {
    /** Error that prevents the requested operation from completing. */
    ERROR('E'),
    /** Warning that callers may handle without treating the operation as failed. */
    WARNING('W'),
    /** Informational notification. */
    INFO('I'),
    /** Severity used when the notification code has no recognized prefix. */
    UNSPECIFIED('U');

    private final char codePrefix;

    NotificationSeverity(char codePrefix) {
        this.codePrefix = codePrefix;
    }

    /**
     * Returns the single-character prefix commonly used in structured error codes.
     *
     * @return code prefix
     */
    public char codePrefix() {
        return codePrefix;
    }

    /**
     * Infers severity from the first character of a notification code.
     *
     * @param code notification code
     * @return matching severity, or {@link #UNSPECIFIED} when the prefix is not known
     */
    public static NotificationSeverity fromCode(String code) {
        if (code == null || code.isBlank()) {
            return UNSPECIFIED;
        }
        return fromPrefix(code.trim().toUpperCase(Locale.ROOT).charAt(0));
    }

    /**
     * Resolves severity from a single-character prefix.
     *
     * @param prefix severity prefix
     * @return matching severity, or {@link #UNSPECIFIED} when the prefix is not known
     */
    public static NotificationSeverity fromPrefix(char prefix) {
        char normalized = Character.toUpperCase(prefix);
        for (NotificationSeverity severity : values()) {
            if (severity.codePrefix == normalized) {
                return severity;
            }
        }
        return UNSPECIFIED;
    }
}
