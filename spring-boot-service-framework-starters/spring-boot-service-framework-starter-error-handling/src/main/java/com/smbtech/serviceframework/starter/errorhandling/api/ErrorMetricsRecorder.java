package com.smbtech.serviceframework.starter.errorhandling.api;

import com.smbtech.serviceframework.error.ResolvedError;

/** Records bounded-cardinality metrics for resolved HTTP errors. */
@FunctionalInterface
public interface ErrorMetricsRecorder {

    /**
     * Records an error response.
     *
     * @param resolvedError resolved framework error
     * @param statusCode HTTP response status code
     */
    void record(ResolvedError resolvedError, int statusCode);

    /**
     * Returns a recorder that intentionally discards metrics.
     *
     * @return no-op recorder
     */
    static ErrorMetricsRecorder noop() {
        return (resolvedError, statusCode) -> {};
    }
}
