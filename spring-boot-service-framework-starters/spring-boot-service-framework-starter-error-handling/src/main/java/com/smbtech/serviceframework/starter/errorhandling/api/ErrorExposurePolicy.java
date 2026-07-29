package com.smbtech.serviceframework.starter.errorhandling.api;

import com.smbtech.serviceframework.error.ErrorExposure;
import com.smbtech.serviceframework.error.ResolvedError;

/** Selects the response audience and detail level before response creation. */
@FunctionalInterface
public interface ErrorExposurePolicy {

    /**
     * Resolves the effective response audience and detail level.
     *
     * @param resolvedError resolved error
     * @return effective response audience and detail level
     */
    ErrorExposure resolve(ResolvedError resolvedError);
}
