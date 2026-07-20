package com.smbtech.serviceframework.error;

import com.smbtech.serviceframework.commons.notification.Notification;
import java.util.List;
import java.util.Objects;

/**
 * Framework-neutral result produced after resolving an application failure. Public response data
 * and internal diagnostics remain explicitly separated.
 *
 * @param notification primary notification
 * @param category error category used by response policies
 * @param exposure whether the notification is safe for public exposure
 * @param diagnosticMessage internal diagnostic message
 * @param fieldViolations validation failures associated with the error
 */
public record ResolvedError(
        Notification notification,
        ErrorCategory category,
        ErrorExposure exposure,
        String diagnosticMessage,
        List<FieldViolation> fieldViolations) {

    /** Validates required resolution data and defensively copies field violations. */
    public ResolvedError {
        notification = Objects.requireNonNull(notification, "notification must not be null");
        category = Objects.requireNonNull(category, "category must not be null");
        exposure = Objects.requireNonNullElse(exposure, ErrorExposure.INTERNAL);
        diagnosticMessage = Objects.requireNonNullElse(diagnosticMessage, "");
        fieldViolations = fieldViolations == null ? List.of() : List.copyOf(fieldViolations);
    }

    /**
     * Creates a resolved error without field violations.
     *
     * @param notification primary notification
     * @param category error category
     * @param exposure error exposure
     * @param diagnosticMessage internal diagnostic message
     */
    public ResolvedError(
            Notification notification,
            ErrorCategory category,
            ErrorExposure exposure,
            String diagnosticMessage) {
        this(notification, category, exposure, diagnosticMessage, List.of());
    }

    /**
     * Indicates whether this error contains field-level validation failures.
     *
     * @return {@code true} when field violations are present
     */
    public boolean hasFieldViolations() {
        return !fieldViolations.isEmpty();
    }
}
