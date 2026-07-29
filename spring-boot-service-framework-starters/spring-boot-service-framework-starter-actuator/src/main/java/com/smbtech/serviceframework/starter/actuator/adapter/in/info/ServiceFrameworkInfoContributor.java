package com.smbtech.serviceframework.starter.actuator.adapter.in.info;

import com.smbtech.serviceframework.actuator.domain.FrameworkModuleInfo;
import com.smbtech.serviceframework.actuator.port.in.FrameworkDiagnostics;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;

/** Adds bounded Service Framework module information to Spring Boot Actuator info. */
public final class ServiceFrameworkInfoContributor implements InfoContributor {

    /** Stable key used in the Actuator info response. */
    public static final String INFO_KEY = "serviceFramework";

    private final FrameworkDiagnostics diagnostics;

    /**
     * Creates the Service Framework info contributor.
     *
     * @param diagnostics framework diagnostics use case
     */
    public ServiceFrameworkInfoContributor(FrameworkDiagnostics diagnostics) {
        this.diagnostics = Objects.requireNonNull(diagnostics, "diagnostics");
    }

    @Override
    public void contribute(Info.Builder builder) {
        Objects.requireNonNull(builder, "builder");
        try {
            List<FrameworkModuleInfo> modules = diagnostics.modules();
            if (modules == null) {
                builder.withDetail(INFO_KEY, unavailable("no_result"));
                return;
            }
            builder.withDetail(INFO_KEY, moduleInformation(modules));
        } catch (RuntimeException ignored) {
            builder.withDetail(INFO_KEY, unavailable("diagnostics_failed"));
        }
    }

    private static Map<String, Object> moduleInformation(List<FrameworkModuleInfo> modules) {
        List<Map<String, Object>> details = new ArrayList<>(modules.size());
        for (FrameworkModuleInfo module : modules) {
            FrameworkModuleInfo required = Objects.requireNonNull(module, "module");
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", required.name());
            item.put("version", required.version());
            if (!required.attributes().isEmpty()) {
                item.put("attributes", required.attributes());
            }
            details.add(Collections.unmodifiableMap(item));
        }

        Map<String, Object> information = new LinkedHashMap<>();
        information.put("available", true);
        information.put("moduleCount", details.size());
        information.put("modules", Collections.unmodifiableList(details));
        return Collections.unmodifiableMap(information);
    }

    private static Map<String, Object> unavailable(String reason) {
        Map<String, Object> information = new LinkedHashMap<>();
        information.put("available", false);
        information.put("moduleCount", 0);
        information.put("modules", List.of());
        information.put("reason", reason);
        return Collections.unmodifiableMap(information);
    }
}
