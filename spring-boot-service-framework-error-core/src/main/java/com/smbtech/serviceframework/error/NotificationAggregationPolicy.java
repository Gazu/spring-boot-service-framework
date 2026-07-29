package com.smbtech.serviceframework.error;

import com.smbtech.serviceframework.commons.notification.Notification;
import java.util.List;

/** Selects the primary notification and aggregates related validation failures. */
@FunctionalInterface
public interface NotificationAggregationPolicy {

    /**
     * Aggregates ordered notifications into one resolved error.
     *
     * @param notifications notifications ordered by application priority
     * @param category category associated with the primary error
     * @param exposure target response audience and detail level
     * @param diagnosticMessage internal diagnostic message
     * @return aggregated resolved error
     */
    ResolvedError aggregate(
            List<Notification> notifications,
            ErrorCategory category,
            ErrorExposure exposure,
            String diagnosticMessage);
}
