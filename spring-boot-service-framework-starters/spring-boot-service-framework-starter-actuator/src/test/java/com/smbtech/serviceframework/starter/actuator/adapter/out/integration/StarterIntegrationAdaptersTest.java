package com.smbtech.serviceframework.starter.actuator.adapter.out.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.smbtech.serviceframework.actuator.domain.ComponentHealth;
import com.smbtech.serviceframework.actuator.domain.FrameworkModuleInfo;
import com.smbtech.serviceframework.error.ErrorExposure;
import com.smbtech.serviceframework.starter.errorhandling.autoconfigure.ErrorHandlingProperties;
import com.smbtech.serviceframework.starter.logging.autoconfigure.LoggingProperties;
import com.smbtech.serviceframework.starter.mock.autoconfigure.MockProperties;
import com.smbtech.serviceframework.starter.restclient.api.RestClientRegistry;
import com.smbtech.serviceframework.starter.restclient.autoconfigure.RestClientProperties;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class StarterIntegrationAdaptersTest {

    @Test
    void restClientIntegrationIsPassiveAndDoesNotExposeClientConfiguration() {
        RestClientProperties properties = new RestClientProperties();
        RestClientProperties.Client enabled = new RestClientProperties.Client();
        enabled.setBaseUrl("https://secret.internal");
        enabled.getDefaultHeaders().put("Authorization", "secret");
        enabled.getResilience().setEnabled(true);
        enabled.getResilience().getCircuitBreaker().setEnabled(true);
        RestClientProperties.Client disabled = new RestClientProperties.Client();
        disabled.setEnabled(false);
        properties.getClients().put("payments", enabled);
        properties.getClients().put("disabled-client", disabled);
        RestClientRegistry registry = passiveRegistry(Set.of("payments"));

        ComponentHealth health = new RestClientDiagnosticProbe(properties, registry).check();
        FrameworkModuleInfo module =
                new RestClientModuleInfoProvider(properties, registry).provide();

        assertThat(health.name()).isEqualTo("rest-client");
        assertThat(health.details())
                .containsEntry("configuredClientCount", 2)
                .containsEntry("enabledClientCount", 1)
                .containsEntry("registeredClientCount", 1)
                .containsEntry("resilienceEnabledClientCount", 1)
                .containsEntry("circuitBreakerEnabledClientCount", 1);
        assertThat(module.name()).isEqualTo("rest-client");
        assertThat(module.version()).isNotBlank();
        assertThat(health.toString())
                .doesNotContain("payments")
                .doesNotContain("secret.internal")
                .doesNotContain("Authorization")
                .doesNotContain("secret");
    }

    @Test
    void mockIntegrationExposesOnlyBoundedCounts() {
        MockProperties properties = new MockProperties();
        MockProperties.Endpoint endpoint = new MockProperties.Endpoint();
        endpoint.setEnabled(true);
        endpoint.setFile("classpath:private-response.json");
        properties.getEndpoints().put("private-endpoint", endpoint);
        properties.getOpenapi().setEnabled(true);
        MockProperties.Contract contract = new MockProperties.Contract();
        contract.setLocation("classpath:private-contract.yaml");
        properties.getOpenapi().getContracts().put("private-contract", contract);

        FrameworkModuleInfo module = new MockModuleInfoProvider(properties).provide();

        assertThat(module.attributes())
                .containsEntry("configuredEndpointCount", 1)
                .containsEntry("enabledEndpointCount", 1L)
                .containsEntry("openApiEnabled", true)
                .containsEntry("configuredOpenApiContractCount", 1)
                .containsEntry("enabledOpenApiContractCount", 1L);
        assertThat(module.toString())
                .doesNotContain("private-endpoint")
                .doesNotContain("private-response")
                .doesNotContain("private-contract");
    }

    @Test
    void loggingIntegrationDoesNotExposeCorrelationHeaderConfiguration() {
        LoggingProperties properties = new LoggingProperties();
        properties.setProduction(true);
        properties.setLevel("WARN");
        properties.getTransaction().setHeaderName("X-Private-Correlation");

        FrameworkModuleInfo module = new LoggingModuleInfoProvider(properties).provide();

        assertThat(module.attributes())
                .containsEntry("productionMode", true)
                .containsEntry("level", "WARN")
                .containsEntry("asyncEnabled", true)
                .containsEntry("asyncSaturationPolicy", "BLOCK")
                .containsEntry("asyncCriticalEventProtectionEnabled", true)
                .containsEntry("asyncObservabilityEnabled", true)
                .containsEntry("transactionCorrelationEnabled", true);
        assertThat(module.toString()).doesNotContain("X-Private-Correlation");
    }

    @Test
    void errorHandlingIntegrationExposesOnlyFeatureState() {
        ErrorHandlingProperties properties = new ErrorHandlingProperties();
        properties.getResponse().setExposure(ErrorExposure.INTERNAL);
        properties.getMetrics().setMetricName("private.metric.name");
        properties.getResponse().setMetadataAllowlist(Set.of("privateMetadata"));

        FrameworkModuleInfo module = new ErrorHandlingModuleInfoProvider(properties).provide();

        assertThat(module.attributes())
                .containsEntry("enabled", true)
                .containsEntry("responseExposure", "INTERNAL")
                .containsEntry("structuredLoggingEnabled", true)
                .containsEntry("metricsEnabled", true)
                .containsEntry("securityAdaptersEnabled", true);
        assertThat(module.toString())
                .doesNotContain("private.metric.name")
                .doesNotContain("privateMetadata");
    }

    private static RestClientRegistry passiveRegistry(Set<String> names) {
        return new RestClientRegistry() {
            @Override
            public RestClient get(String name) {
                throw new AssertionError("The passive integration must not create REST clients");
            }

            @Override
            public Set<String> names() {
                return names;
            }

            @Override
            public Map<String, RestClient> all() {
                throw new AssertionError("The passive integration must not create REST clients");
            }
        };
    }
}
