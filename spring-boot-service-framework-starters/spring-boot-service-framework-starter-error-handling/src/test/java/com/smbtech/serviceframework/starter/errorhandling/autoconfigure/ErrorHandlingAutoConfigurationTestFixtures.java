package com.smbtech.serviceframework.starter.errorhandling.autoconfigure;

import com.smbtech.serviceframework.starter.errorhandling.api.ErrorExposurePolicy;

/** Creates package-owned auto-configuration defaults for tests outside this package. */
public final class ErrorHandlingAutoConfigurationTestFixtures {

    private ErrorHandlingAutoConfigurationTestFixtures() {}

    public static ErrorExposurePolicy exposurePolicy(ErrorHandlingProperties properties) {
        return new ConfiguredErrorExposurePolicy(properties);
    }
}
