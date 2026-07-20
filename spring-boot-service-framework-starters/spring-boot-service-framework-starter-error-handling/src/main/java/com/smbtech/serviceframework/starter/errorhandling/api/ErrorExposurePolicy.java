package com.smbtech.serviceframework.starter.errorhandling.api;

import com.smbtech.serviceframework.error.ErrorExposure;
import com.smbtech.serviceframework.error.ResolvedError;

/** Selects the exposure applied to a resolved error before response creation. */
@FunctionalInterface
public interface ErrorExposurePolicy {

    /**
     * Resolves the effective exposure.
     *
     * @param resolvedError resolved error
     * @return effective exposure
     */
    ErrorExposure resolve(ResolvedError resolvedError);
}
