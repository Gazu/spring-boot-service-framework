package com.smbtech.serviceframework.starter.actuator.adapter.out.integration;

import com.smbtech.serviceframework.starter.restclient.api.RestClientRegistry;
import com.smbtech.serviceframework.starter.restclient.autoconfigure.RestClientProperties;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

record RestClientIntegrationSnapshot(
        int configuredClientCount,
        int enabledClientCount,
        int registeredClientCount,
        int resilienceEnabledClientCount,
        int circuitBreakerEnabledClientCount) {

    static RestClientIntegrationSnapshot capture(
            RestClientProperties properties, RestClientRegistry registry) {
        Map<String, RestClientProperties.Client> clients =
                Objects.requireNonNullElse(properties.getClients(), Map.of());
        int enabledClients = 0;
        int resilienceEnabledClients = 0;
        int circuitBreakerEnabledClients = 0;
        for (RestClientProperties.Client client : clients.values()) {
            if (client == null || !client.isEnabled()) {
                continue;
            }
            enabledClients++;
            RestClientProperties.Resilience resilience = client.getResilience();
            if (resilience == null) {
                continue;
            }
            RestClientProperties.Retry retry = resilience.getRetry();
            RestClientProperties.CircuitBreaker circuitBreaker = resilience.getCircuitBreaker();
            boolean circuitBreakerEnabled = circuitBreaker != null && circuitBreaker.isEnabled();
            if (resilience.isEnabled()
                    || (retry != null && retry.isEnabled())
                    || circuitBreakerEnabled) {
                resilienceEnabledClients++;
            }
            if (circuitBreakerEnabled) {
                circuitBreakerEnabledClients++;
            }
        }
        Set<String> registeredNames = registry.names();
        return new RestClientIntegrationSnapshot(
                clients.size(),
                enabledClients,
                registeredNames == null ? 0 : registeredNames.size(),
                resilienceEnabledClients,
                circuitBreakerEnabledClients);
    }

    Map<String, Object> details() {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("configuredClientCount", configuredClientCount);
        details.put("enabledClientCount", enabledClientCount);
        details.put("registeredClientCount", registeredClientCount);
        details.put("resilienceEnabledClientCount", resilienceEnabledClientCount);
        details.put("circuitBreakerEnabledClientCount", circuitBreakerEnabledClientCount);
        return Map.copyOf(details);
    }
}
