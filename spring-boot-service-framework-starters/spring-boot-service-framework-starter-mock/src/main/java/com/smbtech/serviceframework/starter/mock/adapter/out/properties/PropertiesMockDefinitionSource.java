package com.smbtech.serviceframework.starter.mock.adapter.out.properties;

import com.smbtech.serviceframework.mock.domain.MockDefinition;
import com.smbtech.serviceframework.mock.port.out.MockDefinitionSource;
import com.smbtech.serviceframework.starter.mock.autoconfigure.MockProperties;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Provides properties mock definition source behavior. */
final class PropertiesMockDefinitionSource implements MockDefinitionSource {

    private final MockProperties properties;

    /**
     * Creates a properties mock definition source instance.
     *
     * @param properties properties value
     */
    PropertiesMockDefinitionSource(MockProperties properties) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    @Override
    public Map<String, MockDefinition> loadDefinitions() {
        Map<String, MockDefinition> definitions = new LinkedHashMap<>();
        Objects.requireNonNullElse(
                        properties.getEndpoints(), Map.<String, MockProperties.Endpoint>of())
                .forEach(
                        (key, endpoint) -> {
                            if (endpoint == null) {
                                return;
                            }
                            definitions.put(
                                    key,
                                    new MockDefinition(
                                            key,
                                            endpoint.isEnabled(),
                                            endpoint.getFile(),
                                            endpoint.getDelay()));
                        });
        return definitions;
    }
}
