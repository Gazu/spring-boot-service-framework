package com.smbtech.serviceframework.starter.actuator.adapter.out.integration;

import com.smbtech.serviceframework.actuator.domain.ComponentHealth;
import com.smbtech.serviceframework.actuator.port.out.DiagnosticProbe;
import com.smbtech.serviceframework.starter.restclient.api.RestClientRegistry;
import com.smbtech.serviceframework.starter.restclient.autoconfigure.RestClientProperties;
import java.util.Objects;

/** Provides passive, bounded diagnostics for the REST client starter. */
final class RestClientDiagnosticProbe implements DiagnosticProbe {

    /** Stable diagnostics component name. */
    static final String COMPONENT_NAME = "rest-client";

    private final RestClientProperties properties;
    private final RestClientRegistry registry;

    /**
     * Creates a REST client diagnostic probe.
     *
     * @param properties REST client configuration
     * @param registry REST client registry
     */
    RestClientDiagnosticProbe(RestClientProperties properties, RestClientRegistry registry) {
        this.properties = Objects.requireNonNull(properties, "properties");
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    @Override
    public String componentName() {
        return COMPONENT_NAME;
    }

    @Override
    public ComponentHealth check() {
        return ComponentHealth.up(
                COMPONENT_NAME,
                RestClientIntegrationSnapshot.capture(properties, registry).details());
    }
}
