package com.smbtech.serviceframework.starter.errorhandling.internal;

import com.smbtech.serviceframework.error.ResolvedError;
import jakarta.servlet.http.HttpServletRequest;

/** Exposes internal pipeline behavior to tests without widening production visibility. */
public final class ErrorPipelineTestFixtures {

    private ErrorPipelineTestFixtures() {}

    public static Class<?> customizationPipelineType() {
        return ErrorCustomizationPipeline.class;
    }

    public static ResolvedError customize(
            Object pipeline,
            Throwable cause,
            ResolvedError resolvedError,
            HttpServletRequest request) {
        return ((ErrorCustomizationPipeline) pipeline).customize(cause, resolvedError, request);
    }
}
