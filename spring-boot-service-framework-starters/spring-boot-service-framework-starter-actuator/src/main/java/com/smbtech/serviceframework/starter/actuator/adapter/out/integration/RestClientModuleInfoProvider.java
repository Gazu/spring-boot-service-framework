package com.smbtech.serviceframework.starter.actuator.adapter.out.integration;

import com.smbtech.serviceframework.actuator.domain.FrameworkModuleInfo;
import com.smbtech.serviceframework.actuator.port.out.FrameworkModuleInfoProvider;
import com.smbtech.serviceframework.starter.restclient.api.RestClientRegistry;
import com.smbtech.serviceframework.starter.restclient.autoconfigure.RestClientProperties;
import java.util.Objects;

/** Provides bounded application information for the REST client starter. */
public final class RestClientModuleInfoProvider implements FrameworkModuleInfoProvider {

    /** Stable framework module name. */
    public static final String MODULE_NAME = "rest-client";

    private final RestClientProperties properties;
    private final RestClientRegistry registry;

    /**
     * Creates a REST client module information provider.
     *
     * @param properties REST client configuration
     * @param registry REST client registry
     */
    public RestClientModuleInfoProvider(
            RestClientProperties properties, RestClientRegistry registry) {
        this.properties = Objects.requireNonNull(properties, "properties");
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    @Override
    public String moduleName() {
        return MODULE_NAME;
    }

    @Override
    public FrameworkModuleInfo provide() {
        return new FrameworkModuleInfo(
                MODULE_NAME,
                ModuleVersions.resolve(RestClientProperties.class),
                RestClientIntegrationSnapshot.capture(properties, registry).details());
    }
}
