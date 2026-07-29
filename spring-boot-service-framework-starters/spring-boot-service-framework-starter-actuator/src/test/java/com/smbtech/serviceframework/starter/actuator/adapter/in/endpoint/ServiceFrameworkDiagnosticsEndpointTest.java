package com.smbtech.serviceframework.starter.actuator.adapter.in.endpoint;

import static org.assertj.core.api.Assertions.assertThat;

import com.smbtech.serviceframework.actuator.domain.ComponentHealth;
import com.smbtech.serviceframework.actuator.domain.FrameworkDiagnosticsSnapshot;
import com.smbtech.serviceframework.actuator.domain.FrameworkModuleInfo;
import com.smbtech.serviceframework.actuator.port.in.FrameworkDiagnostics;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.endpoint.Access;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;

class ServiceFrameworkDiagnosticsEndpointTest {

    @Test
    @SuppressWarnings("unchecked")
    void returnsBoundedHealthAndModuleDiagnostics() {
        FrameworkDiagnostics diagnostics =
                new FrameworkDiagnostics() {
                    @Override
                    public FrameworkDiagnosticsSnapshot snapshot() {
                        return new FrameworkDiagnosticsSnapshot(
                                Instant.parse("2026-07-27T12:00:00Z"),
                                Map.of(
                                        "rest-client",
                                        ComponentHealth.up(
                                                "rest-client",
                                                Map.of(
                                                        "configuredClients",
                                                        2,
                                                        "authorization",
                                                        "must-not-leak"))));
                    }

                    @Override
                    public List<FrameworkModuleInfo> modules() {
                        return List.of(FrameworkModuleInfo.of("rest-client", "0.4.0"));
                    }
                };

        Map<String, Object> response =
                new ServiceFrameworkDiagnosticsEndpoint(diagnostics).diagnostics();
        Map<String, Object> components = (Map<String, Object>) response.get("components");
        Map<String, Object> restClient = (Map<String, Object>) components.get("rest-client");
        Map<String, Object> details = (Map<String, Object>) restClient.get("details");
        List<Map<String, Object>> modules = (List<Map<String, Object>>) response.get("modules");

        assertThat(response)
                .containsEntry("capturedAt", "2026-07-27T12:00:00Z")
                .containsEntry("status", "UP")
                .containsEntry("componentCount", 1)
                .containsEntry("moduleCount", 1);
        assertThat(details)
                .containsEntry("configuredClients", 2)
                .containsEntry("authorization", "[REDACTED]");
        assertThat(modules.getFirst())
                .containsEntry("name", "rest-client")
                .containsEntry("version", "0.4.0");
        assertThat(response.toString()).doesNotContain("must-not-leak");
    }

    @Test
    void endpointIsReadOnlyAndDisabledByDefault() {
        Endpoint endpoint = ServiceFrameworkDiagnosticsEndpoint.class.getAnnotation(Endpoint.class);
        List<Method> readOperations =
                Arrays.stream(ServiceFrameworkDiagnosticsEndpoint.class.getDeclaredMethods())
                        .filter(method -> method.isAnnotationPresent(ReadOperation.class))
                        .toList();

        assertThat(endpoint.id()).isEqualTo("serviceframework");
        assertThat(endpoint.defaultAccess()).isEqualTo(Access.NONE);
        assertThat(readOperations).extracting(Method::getName).containsExactly("diagnostics");
    }

    @Test
    void convertsDiagnosticsFailureToSafeUnknownResponse() {
        FrameworkDiagnostics diagnostics =
                new FrameworkDiagnostics() {
                    @Override
                    public FrameworkDiagnosticsSnapshot snapshot() {
                        throw new IllegalStateException("secret failure");
                    }

                    @Override
                    public List<FrameworkModuleInfo> modules() {
                        return List.of();
                    }
                };

        Map<String, Object> response =
                new ServiceFrameworkDiagnosticsEndpoint(diagnostics).diagnostics();

        assertThat(response)
                .containsEntry("status", "UNKNOWN")
                .containsEntry("componentCount", 0)
                .containsEntry("moduleCount", 0)
                .containsEntry("reason", "diagnostics_failed");
        assertThat(response.toString()).doesNotContain("secret failure");
    }

    @Test
    void convertsMissingResultToSafeUnknownResponse() {
        FrameworkDiagnostics diagnostics =
                new FrameworkDiagnostics() {
                    @Override
                    public FrameworkDiagnosticsSnapshot snapshot() {
                        return null;
                    }

                    @Override
                    public List<FrameworkModuleInfo> modules() {
                        return List.of();
                    }
                };

        assertThat(new ServiceFrameworkDiagnosticsEndpoint(diagnostics).diagnostics())
                .containsEntry("status", "UNKNOWN")
                .containsEntry("reason", "no_result");
    }
}
