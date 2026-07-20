package com.smbtech.serviceframework.error;

/** Resolves supported failures into framework-neutral error information. */
public interface ThrowableErrorResolver {

    /** Selects a resolver before every other ordered implementation. */
    int HIGHEST_PRECEDENCE = Integer.MIN_VALUE;

    /** Selects a resolver after every other ordered implementation. */
    int LOWEST_PRECEDENCE = Integer.MAX_VALUE;

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
