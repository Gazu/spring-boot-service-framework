package com.smbtech.serviceframework.starter.errorhandling.autoconfigure;

import com.smbtech.serviceframework.error.ErrorExposure;
import com.smbtech.serviceframework.error.ResolvedError;
import com.smbtech.serviceframework.starter.errorhandling.api.ErrorExposurePolicy;
import java.util.Objects;

/** Applies the configured response audience and detail level to every resolved error. */
final class ConfiguredErrorExposurePolicy implements ErrorExposurePolicy {

    private final ErrorExposure exposure;

    /**
     * Creates a policy from the error handling configuration.
     *
     * @param properties error handling configuration
     */
    ConfiguredErrorExposurePolicy(ErrorHandlingProperties properties) {
        ErrorHandlingProperties source =
                Objects.requireNonNull(properties, "properties must not be null");
        this.exposure =
                Objects.requireNonNull(
                        source.getResponse().getExposure(), "response exposure must not be null");
    }

    /**
     * Returns the configured response audience and detail level regardless of the resolver
     * decision.
     *
     * @param resolvedError resolved error
     * @return configured response audience and detail level
     */
    @Override
    public ErrorExposure resolve(ResolvedError resolvedError) {
        Objects.requireNonNull(resolvedError, "resolvedError must not be null");
        return exposure;
    }
}
