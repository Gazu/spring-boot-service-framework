package com.smbtech.serviceframework.starter.errorhandling.api;

import com.smbtech.serviceframework.error.ResolvedError;
import jakarta.servlet.http.HttpServletRequest;

/** Reports resolved request failures without changing their HTTP response. */
@FunctionalInterface
public interface ErrorReporter {

    /**
     * Reports a resolved request failure.
     *
     * @param cause original request failure
     * @param resolvedError resolved framework error
     * @param request current HTTP request
     */
    void report(Throwable cause, ResolvedError resolvedError, HttpServletRequest request);

    /**
     * Reports a resolved request failure with its final HTTP status. Implementations created for
     * the original three-argument contract remain compatible and receive the same event through
     * {@link #report(Throwable, ResolvedError, HttpServletRequest)}.
     *
     * @param cause original request failure
     * @param resolvedError resolved framework error
     * @param request current HTTP request
     * @param statusCode final HTTP response status code
     */
    default void report(
            Throwable cause,
            ResolvedError resolvedError,
            HttpServletRequest request,
            int statusCode) {
        report(cause, resolvedError, request);
    }

    /**
     * Returns reporter precedence. Lower values run first.
     *
     * @return reporter order
     */
    default int order() {
        return 0;
    }

    /**
     * Returns a reporter that intentionally discards errors.
     *
     * @return no-op reporter
     */
    static ErrorReporter noop() {
        return (cause, resolvedError, request) -> {};
    }
}
