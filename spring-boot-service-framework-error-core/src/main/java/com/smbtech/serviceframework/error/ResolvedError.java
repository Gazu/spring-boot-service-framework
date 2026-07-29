package com.smbtech.serviceframework.error;

import com.smbtech.serviceframework.commons.notification.Notification;
import java.util.List;
import java.util.Objects;

/**
 * Framework-neutral result produced after resolving an application failure. Response data and
 * internal diagnostics remain explicitly separated; the diagnostic message is never part of the
 * HTTP notification.
 *
 * @param notification primary notification
 * @param category error category used by response policies
 * @param exposure target response audience and detail level
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
     * @param exposure target response audience and detail level
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

    /**
     * Returns a copy with a replacement notification.
     *
     * @param notification replacement notification
     * @return updated immutable error
     */
    public ResolvedError withNotification(Notification notification) {
        return new ResolvedError(
                notification, category, exposure, diagnosticMessage, fieldViolations);
    }

    /**
     * Returns a copy with a replacement exposure.
     *
     * @param exposure replacement exposure
     * @return updated immutable error
     */
    public ResolvedError withExposure(ErrorExposure exposure) {
        return new ResolvedError(
                notification, category, exposure, diagnosticMessage, fieldViolations);
    }

    /**
     * Returns a copy with replacement field violations.
     *
     * @param fieldViolations replacement violations
     * @return updated immutable error
     */
    public ResolvedError withFieldViolations(List<FieldViolation> fieldViolations) {
        return new ResolvedError(
                notification, category, exposure, diagnosticMessage, fieldViolations);
    }
}
