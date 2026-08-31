package com.smbtech.serviceframework.starter.actuator.autoconfigure;

import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;

/** Selects package-local Actuator configurations without exposing their adapters. */
final class ActuatorConfigurationImportSelector implements ImportSelector {

    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
        return switch (importingClassMetadata.getClassName()) {
            case "com.smbtech.serviceframework.starter.actuator.autoconfigure.ActuatorAutoConfiguration" ->
                    configuration(
                            "com.smbtech.serviceframework.starter.actuator.adapter.out.diagnostics.FrameworkDiagnosticsConfiguration");
            case "com.smbtech.serviceframework.starter.actuator.autoconfigure.ActuatorHealthAutoConfiguration" ->
                    configuration(
                            "com.smbtech.serviceframework.starter.actuator.adapter.in.health.ServiceFrameworkHealthConfiguration");
            case "com.smbtech.serviceframework.starter.actuator.autoconfigure.ActuatorInfoAutoConfiguration" ->
                    configuration(
                            "com.smbtech.serviceframework.starter.actuator.adapter.in.info.ServiceFrameworkInfoConfiguration");
            case "com.smbtech.serviceframework.starter.actuator.autoconfigure.ActuatorEndpointAutoConfiguration" ->
                    configuration(
                            "com.smbtech.serviceframework.starter.actuator.adapter.in.endpoint.ServiceFrameworkEndpointConfiguration");
            case "com.smbtech.serviceframework.starter.actuator.autoconfigure.ActuatorIntegrationsAutoConfiguration" ->
                    configuration(
                            "com.smbtech.serviceframework.starter.actuator.adapter.out.integration.ActuatorIntegrationConfiguration");
            case "com.smbtech.serviceframework.starter.actuator.autoconfigure.ActuatorMetricsAutoConfiguration" ->
                    configuration(
                            "com.smbtech.serviceframework.starter.actuator.adapter.in.metrics.ServiceFrameworkMetricsConfiguration");
            default -> new String[0];
        };
    }

    private static String[] configuration(String className) {
        return new String[] {className};
    }
}
