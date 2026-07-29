package com.smbtech.serviceframework.starter.mock.adapter.in.openapi;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.smbtech.serviceframework.starter.mock.autoconfigure.MockProperties;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class OpenApiMockEnvironmentGuardTest {

    @Test
    void blocksDefaultProductionProfiles() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");

        assertThatThrownBy(
                        () ->
                                new OpenApiMockEnvironmentGuard(environment)
                                        .validate(new MockProperties.OpenApi()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("smbtech.mocks.openapi.allow-in-production=true");
    }

    @Test
    void allowsProductionOnlyWhenExplicitlyConfigured() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("production");
        MockProperties.OpenApi properties = new MockProperties.OpenApi();
        properties.setAllowInProduction(true);

        assertThatCode(() -> new OpenApiMockEnvironmentGuard(environment).validate(properties))
                .doesNotThrowAnyException();
    }

    @Test
    void supportsApplicationSpecificProductionProfiles() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("LIVE");
        MockProperties.OpenApi properties = new MockProperties.OpenApi();
        properties.setProductionProfiles(Set.of("live"));

        assertThatThrownBy(() -> new OpenApiMockEnvironmentGuard(environment).validate(properties))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("production profile 'live'");
    }
}
