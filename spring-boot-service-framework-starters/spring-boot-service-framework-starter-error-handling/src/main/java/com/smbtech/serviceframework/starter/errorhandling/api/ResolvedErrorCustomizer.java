package com.smbtech.serviceframework.starter.errorhandling.api;

import com.smbtech.serviceframework.error.ResolvedError;
import jakarta.servlet.http.HttpServletRequest;

/** Applies an ordered, request-aware customization to a resolved error. */
@FunctionalInterface
public interface ResolvedErrorCustomizer {

    /**
     * Customizes a resolved error before reporting and response creation.
     *
     * @param cause original request failure
     * @param resolvedError current resolved error
     * @param request current HTTP request
     * @return customized resolved error
     */
    ResolvedError customize(
            Throwable cause, ResolvedError resolvedError, HttpServletRequest request);

    /**
     * Returns customizer precedence. Lower values run first.
     *
     * @return customizer order
     */
    default int order() {
        return 0;
    }
}
