package com.smbtech.serviceframework.starter.actuator.adapter.out.integration;

import com.smbtech.serviceframework.actuator.domain.FrameworkModuleInfo;
import com.smbtech.serviceframework.actuator.port.out.FrameworkModuleInfoProvider;
import com.smbtech.serviceframework.starter.errorhandling.autoconfigure.ErrorHandlingProperties;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Provides bounded application information for the error handling starter. */
public final class ErrorHandlingModuleInfoProvider implements FrameworkModuleInfoProvider {

    /** Stable framework module name. */
    public static final String MODULE_NAME = "error-handling";

    private final ErrorHandlingProperties properties;

    /**
     * Creates an error handling module information provider.
     *
     * @param properties error handling configuration
     */
    public ErrorHandlingModuleInfoProvider(ErrorHandlingProperties properties) {
        this.properties = Objects.requireNonNull(properties, "properties");
    }

    @Override
    public String moduleName() {
        return MODULE_NAME;
    }

    @Override
    public FrameworkModuleInfo provide() {
        ErrorHandlingProperties.Response response = properties.getResponse();
        ErrorHandlingProperties.Logging logging = properties.getLogging();
        ErrorHandlingProperties.Metrics metrics = properties.getMetrics();
        ErrorHandlingProperties.Security security = properties.getSecurity();

        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("enabled", properties.isEnabled());
        attributes.put(
                "responseExposure",
                response == null || response.getExposure() == null
                        ? "UNKNOWN"
                        : response.getExposure().name());
        attributes.put("structuredLoggingEnabled", logging != null && logging.isEnabled());
        attributes.put("metricsEnabled", metrics != null && metrics.isEnabled());
        attributes.put("securityAdaptersEnabled", security != null && security.isEnabled());
        return new FrameworkModuleInfo(
                MODULE_NAME, ModuleVersions.resolve(ErrorHandlingProperties.class), attributes);
    }
}
