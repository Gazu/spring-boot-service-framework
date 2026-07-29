package com.smbtech.serviceframework.starter.errorhandling.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.smbtech.serviceframework.commons.notification.Notification;
import com.smbtech.serviceframework.error.ErrorCategory;
import com.smbtech.serviceframework.error.ErrorExposure;
import com.smbtech.serviceframework.error.ResolvedError;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ConfiguredErrorExposurePolicyTest {

    @ParameterizedTest(name = "configured={0}, resolver={1}")
    @CsvSource({"PUBLIC, PUBLIC", "PUBLIC, INTERNAL", "INTERNAL, PUBLIC", "INTERNAL, INTERNAL"})
    void alwaysReturnsTheConfiguredExposure(
            ErrorExposure configuredExposure, ErrorExposure resolverExposure) {
        ErrorHandlingProperties properties = new ErrorHandlingProperties();
        properties.getResponse().setExposure(configuredExposure);
        ConfiguredErrorExposurePolicy policy = new ConfiguredErrorExposurePolicy(properties);

        assertThat(policy.resolve(resolvedError(resolverExposure))).isEqualTo(configuredExposure);
    }

    @Test
    void usesTheSafeDefaultExposure() {
        ConfiguredErrorExposurePolicy policy =
                new ConfiguredErrorExposurePolicy(new ErrorHandlingProperties());

        assertThat(policy.resolve(resolvedError(ErrorExposure.INTERNAL)))
                .isEqualTo(ErrorExposure.PUBLIC);
    }

    @Test
    void rejectsMissingConfigurationAndResolvedErrors() {
        assertThatThrownBy(() -> new ConfiguredErrorExposurePolicy(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("properties must not be null");

        ConfiguredErrorExposurePolicy policy =
                new ConfiguredErrorExposurePolicy(new ErrorHandlingProperties());
        assertThatThrownBy(() -> policy.resolve(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("resolvedError must not be null");
    }

    private static ResolvedError resolvedError(ErrorExposure exposure) {
        return new ResolvedError(
                Notification.error("E_TEST", "Test failure"),
                ErrorCategory.INTERNAL,
                exposure,
                "diagnostic");
    }
}
