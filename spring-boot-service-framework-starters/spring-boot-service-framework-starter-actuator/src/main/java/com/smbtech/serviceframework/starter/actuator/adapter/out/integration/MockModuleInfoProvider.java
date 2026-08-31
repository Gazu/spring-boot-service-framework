package com.smbtech.serviceframework.starter.actuator.adapter.out.integration;

import com.smbtech.serviceframework.actuator.domain.FrameworkModuleInfo;
import com.smbtech.serviceframework.actuator.port.out.FrameworkModuleInfoProvider;
import com.smbtech.serviceframework.starter.mock.autoconfigure.MockProperties;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Provides bounded application information for the mock starter. */
final class MockModuleInfoProvider implements FrameworkModuleInfoProvider {

    /** Stable framework module name. */
    static final String MODULE_NAME = "mock";

    private final MockProperties properties;

    /**
     * Creates a mock module information provider.
     *
     * @param properties mock configuration
     */
    MockModuleInfoProvider(MockProperties properties) {
        this.properties = Objects.requireNonNull(properties, "properties");
    }

    @Override
    public String moduleName() {
        return MODULE_NAME;
    }

    @Override
    public FrameworkModuleInfo provide() {
        Map<String, MockProperties.Endpoint> endpoints =
                Objects.requireNonNullElse(properties.getEndpoints(), Map.of());
        MockProperties.OpenApi openApi = properties.getOpenapi();
        Map<String, MockProperties.Contract> contracts =
                openApi == null
                        ? Map.of()
                        : Objects.requireNonNullElse(openApi.getContracts(), Map.of());

        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("configuredEndpointCount", endpoints.size());
        attributes.put(
                "enabledEndpointCount",
                endpoints.values().stream()
                        .filter(Objects::nonNull)
                        .filter(MockProperties.Endpoint::isEnabled)
                        .count());
        attributes.put("openApiEnabled", openApi != null && openApi.isEnabled());
        attributes.put("configuredOpenApiContractCount", contracts.size());
        attributes.put(
                "enabledOpenApiContractCount",
                contracts.values().stream()
                        .filter(Objects::nonNull)
                        .filter(MockProperties.Contract::isEnabled)
                        .count());

        return new FrameworkModuleInfo(
                MODULE_NAME, ModuleVersions.resolve(MockProperties.class), attributes);
    }
}
