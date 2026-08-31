package com.smbtech.serviceframework.error;

import java.util.Objects;

/**
 * Resolves {@link ServiceException} instances using a configurable notification aggregation policy.
 */
final class ServiceExceptionThrowableErrorResolver implements ThrowableErrorResolver {

    /** Gives explicit service errors precedence over framework fallbacks. */
    static final int DEFAULT_ORDER = ThrowableErrorResolver.SERVICE_EXCEPTION_ORDER;

    private final NotificationAggregationPolicy aggregationPolicy;

    /** Creates a resolver using the default notification aggregation policy. */
    ServiceExceptionThrowableErrorResolver() {
        this(new DefaultNotificationAggregationPolicy());
    }

    /**
     * Creates a resolver using a custom aggregation policy.
     *
     * @param aggregationPolicy notification aggregation policy
     */
    ServiceExceptionThrowableErrorResolver(NotificationAggregationPolicy aggregationPolicy) {
        this.aggregationPolicy =
                Objects.requireNonNull(aggregationPolicy, "aggregationPolicy must not be null");
    }

    @Override
    public boolean supports(Throwable throwable) {
        return throwable instanceof ServiceException;
    }

    @Override
    public ResolvedError resolve(Throwable throwable) {
        if (!(throwable instanceof ServiceException serviceException)) {
            throw new IllegalArgumentException("throwable must be a ServiceException");
        }
        return aggregationPolicy.aggregate(
                serviceException.notifications(),
                serviceException.category(),
                ErrorExposure.PUBLIC,
                serviceException.diagnosticMessage());
    }

    @Override
    public int order() {
        return DEFAULT_ORDER;
    }

    /**
     * Returns the configured aggregation policy.
     *
     * @return aggregation policy
     */
    NotificationAggregationPolicy aggregationPolicy() {
        return aggregationPolicy;
    }
}
