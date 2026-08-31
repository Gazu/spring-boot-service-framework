package com.smbtech.serviceframework.error;

import com.smbtech.serviceframework.commons.notification.Notification;
import java.util.List;
import java.util.Objects;

/**
 * Uses the first notification as primary and converts notifications associated with fields into
 * ordered validation violations.
 */
final class DefaultNotificationAggregationPolicy implements NotificationAggregationPolicy {

    /** Creates the default aggregation policy. */
    DefaultNotificationAggregationPolicy() {}

    @Override
    public ResolvedError aggregate(
            List<Notification> notifications,
            ErrorCategory category,
            ErrorExposure exposure,
            String diagnosticMessage) {
        List<Notification> safeNotifications = requireNotifications(notifications);
        List<FieldViolation> fieldViolations =
                safeNotifications.stream()
                        .filter(notification -> !notification.fieldName().isBlank())
                        .map(DefaultNotificationAggregationPolicy::toFieldViolation)
                        .toList();
        return new ResolvedError(
                safeNotifications.getFirst(),
                Objects.requireNonNull(category, "category must not be null"),
                exposure,
                diagnosticMessage,
                fieldViolations);
    }

    private static List<Notification> requireNotifications(List<Notification> notifications) {
        if (notifications == null || notifications.isEmpty()) {
            throw new IllegalArgumentException("notifications must not be empty");
        }
        return List.copyOf(notifications);
    }

    private static FieldViolation toFieldViolation(Notification notification) {
        return new FieldViolation(
                notification.fieldName(), notification.code(), notification.message());
    }
}
