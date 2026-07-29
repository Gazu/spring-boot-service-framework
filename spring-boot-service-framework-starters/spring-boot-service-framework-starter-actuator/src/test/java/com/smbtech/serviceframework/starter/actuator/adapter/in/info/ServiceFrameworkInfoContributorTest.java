package com.smbtech.serviceframework.starter.actuator.adapter.in.info;

import static org.assertj.core.api.Assertions.assertThat;

import com.smbtech.serviceframework.actuator.domain.FrameworkDiagnosticsSnapshot;
import com.smbtech.serviceframework.actuator.domain.FrameworkModuleInfo;
import com.smbtech.serviceframework.actuator.port.in.FrameworkDiagnostics;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.info.Info;

class ServiceFrameworkInfoContributorTest {

    @Test
    @SuppressWarnings("unchecked")
    void contributesBoundedModuleInformationUnderStableKey() {
        FrameworkDiagnostics diagnostics =
                diagnostics(
                        List.of(
                                new FrameworkModuleInfo(
                                        "rest-client",
                                        "0.4.0",
                                        Map.of(
                                                "configuredClients",
                                                2,
                                                "clientSecret",
                                                "must-not-leak"))));

        Info.Builder builder = new Info.Builder();
        new ServiceFrameworkInfoContributor(diagnostics).contribute(builder);
        Info info = builder.build();

        Map<String, Object> serviceFramework =
                (Map<String, Object>) info.get(ServiceFrameworkInfoContributor.INFO_KEY);
        List<Map<String, Object>> modules =
                (List<Map<String, Object>>) serviceFramework.get("modules");
        Map<String, Object> attributes = (Map<String, Object>) modules.getFirst().get("attributes");

        assertThat(serviceFramework)
                .containsEntry("available", true)
                .containsEntry("moduleCount", 1);
        assertThat(modules.getFirst())
                .containsEntry("name", "rest-client")
                .containsEntry("version", "0.4.0");
        assertThat(attributes)
                .containsEntry("configuredClients", 2)
                .containsEntry("clientSecret", "[REDACTED]");
        assertThat(info.toString()).doesNotContain("must-not-leak");
    }

    @Test
    @SuppressWarnings("unchecked")
    void convertsDiagnosticsFailureToSafeUnavailableInformation() {
        FrameworkDiagnostics diagnostics =
                new FrameworkDiagnostics() {
                    @Override
                    public FrameworkDiagnosticsSnapshot snapshot() {
                        return null;
                    }

                    @Override
                    public List<FrameworkModuleInfo> modules() {
                        throw new IllegalStateException("secret failure");
                    }
                };

        Info.Builder builder = new Info.Builder();
        new ServiceFrameworkInfoContributor(diagnostics).contribute(builder);
        Info info = builder.build();
        Map<String, Object> serviceFramework =
                (Map<String, Object>) info.get(ServiceFrameworkInfoContributor.INFO_KEY);

        assertThat(serviceFramework)
                .containsEntry("available", false)
                .containsEntry("moduleCount", 0)
                .containsEntry("reason", "diagnostics_failed");
        assertThat(serviceFramework.get("modules")).isEqualTo(List.of());
        assertThat(info.toString()).doesNotContain("secret failure");
    }

    @Test
    @SuppressWarnings("unchecked")
    void convertsMissingModuleResultToSafeUnavailableInformation() {
        Info.Builder builder = new Info.Builder();
        new ServiceFrameworkInfoContributor(diagnostics(null)).contribute(builder);

        Map<String, Object> serviceFramework =
                (Map<String, Object>) builder.build().get(ServiceFrameworkInfoContributor.INFO_KEY);

        assertThat(serviceFramework)
                .containsEntry("available", false)
                .containsEntry("reason", "no_result");
    }

    private static FrameworkDiagnostics diagnostics(List<FrameworkModuleInfo> modules) {
        return new FrameworkDiagnostics() {
            @Override
            public FrameworkDiagnosticsSnapshot snapshot() {
                return null;
            }

            @Override
            public List<FrameworkModuleInfo> modules() {
                return modules;
            }
        };
    }
}
