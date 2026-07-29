package com.smbtech.serviceframework.starter.mock.adapter.in.openapi;

import com.smbtech.serviceframework.starter.mock.autoconfigure.MockProperties;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.core.env.Environment;

final class OpenApiMockEnvironmentGuard {

    private final Environment environment;

    OpenApiMockEnvironmentGuard(Environment environment) {
        this.environment = Objects.requireNonNull(environment, "environment must not be null");
    }

    void validate(MockProperties.OpenApi properties) {
        MockProperties.OpenApi configuration =
                Objects.requireNonNull(properties, "properties must not be null");
        if (configuration.isAllowInProduction()) {
            return;
        }
        Set<String> productionProfiles = normalize(configuration.getProductionProfiles());
        String activeProductionProfile =
                Arrays.stream(environment.getActiveProfiles())
                        .map(OpenApiMockEnvironmentGuard::normalize)
                        .filter(productionProfiles::contains)
                        .findFirst()
                        .orElse(null);
        if (activeProductionProfile != null) {
            throw new IllegalStateException(
                    "OpenAPI mock server is blocked for production profile '"
                            + activeProductionProfile
                            + "'; set smbtech.mocks.openapi.allow-in-production=true only when explicitly approved");
        }
    }

    private static Set<String> normalize(Set<String> profiles) {
        if (profiles == null || profiles.isEmpty()) {
            return Set.of();
        }
        return profiles.stream()
                .filter(Objects::nonNull)
                .map(OpenApiMockEnvironmentGuard::normalize)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    private static String normalize(String profile) {
        return Objects.requireNonNullElse(profile, "").trim().toLowerCase(Locale.ROOT);
    }
}
