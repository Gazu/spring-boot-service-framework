package com.smbtech.serviceframework.error;

import java.util.List;

/** Resolves supported failures into framework-neutral error information. */
public interface ThrowableErrorResolver {

    /** Selects a resolver before every other ordered implementation. */
    int HIGHEST_PRECEDENCE = Integer.MIN_VALUE;

    /** Selects a resolver after every other ordered implementation. */
    int LOWEST_PRECEDENCE = Integer.MAX_VALUE;

    /** Stable notification code used by the safe fallback resolver. */
    String DEFAULT_ERROR_CODE = "E_SERVICE_FRAMEWORK_INTERNAL_0001";

    /** Safe public message used when no specialized resolver supports a failure. */
    String DEFAULT_PUBLIC_MESSAGE = "The request could not be completed";

    /** Default precedence assigned to service exception resolution. */
    int SERVICE_EXCEPTION_ORDER = -1_000;

    /**
     * Creates the safe fallback resolver.
     *
     * @return fallback resolver
     */
    static ThrowableErrorResolver fallback() {
        return new FallbackThrowableErrorResolver();
    }

    /**
     * Creates a resolver for {@link ServiceException} using the default aggregation policy.
     *
     * @return service exception resolver
     */
    static ThrowableErrorResolver serviceExceptions() {
        return new ServiceExceptionThrowableErrorResolver();
    }

    /**
     * Creates a resolver for {@link ServiceException} using a custom aggregation policy.
     *
     * @param aggregationPolicy notification aggregation policy
     * @return service exception resolver
     */
    static ThrowableErrorResolver serviceExceptions(
            NotificationAggregationPolicy aggregationPolicy) {
        return new ServiceExceptionThrowableErrorResolver(aggregationPolicy);
    }

    /**
     * Creates an ordered resolver composition with the safe fallback.
     *
     * @param resolvers specialized resolvers
     * @return resolver composition
     */
    static ThrowableErrorResolver composite(List<? extends ThrowableErrorResolver> resolvers) {
        return new ThrowableErrorResolutionPipeline(resolvers);
    }

    /**
     * Creates an ordered resolver composition with a custom fallback.
     *
     * @param resolvers specialized resolvers
     * @param fallbackResolver resolver used when no specialized resolver supports the failure
     * @return resolver composition
     */
    static ThrowableErrorResolver composite(
            List<? extends ThrowableErrorResolver> resolvers,
            ThrowableErrorResolver fallbackResolver) {
        return new ThrowableErrorResolutionPipeline(resolvers, fallbackResolver);
    }

    /**
     * Indicates whether this resolver can process the supplied failure.
     *
     * @param throwable failure to inspect
     * @return {@code true} when this resolver can process the failure
     */
    boolean supports(Throwable throwable);

    /**
     * Resolves a supported failure.
     *
     * @param throwable supported failure
     * @return resolved error
     */
    ResolvedError resolve(Throwable throwable);

    /**
     * Returns resolver precedence. Lower values run first.
     *
     * @return resolver order
     */
    default int order() {
        return 0;
    }
}
