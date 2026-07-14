package com.smbtech.serviceframework.starter.mock.adapter.out.properties;

import com.smbtech.serviceframework.starter.mock.autoconfigure.MockProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PropertiesMockDefinitionSourceTest {

    @Test
    void returnsEmptyDefinitionsWhenEndpointMapIsNull() {
        MockProperties properties = new MockProperties();
        properties.setEndpoints(null);

        var definitions = new PropertiesMockDefinitionSource(properties).loadDefinitions();

        assertThat(definitions).isEmpty();
    }

    @Test
    void ignoresNullEndpointEntriesAndMapsConfiguredEndpoints() {
        MockProperties properties = new MockProperties();
        MockProperties.Endpoint endpoint = new MockProperties.Endpoint();
        endpoint.setEnabled(true);
        endpoint.setFile("classpath:mocks/payments-success.json");
        endpoint.setDelay(Duration.ofMillis(25));

        Map<String, MockProperties.Endpoint> endpoints = new LinkedHashMap<>();
        endpoints.put("payments-success", endpoint);
        endpoints.put("ignored", null);
        properties.setEndpoints(endpoints);

        var definitions = new PropertiesMockDefinitionSource(properties).loadDefinitions();

        assertThat(definitions).containsOnlyKeys("payments-success");
        assertThat(definitions.get("payments-success").key()).isEqualTo("payments-success");
        assertThat(definitions.get("payments-success").enabled()).isTrue();
        assertThat(definitions.get("payments-success").file()).isEqualTo("classpath:mocks/payments-success.json");
        assertThat(definitions.get("payments-success").delay()).isEqualTo(Duration.ofMillis(25));
    }
}
