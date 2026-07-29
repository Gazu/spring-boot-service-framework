package com.smbtech.serviceframework.starter.actuator.adapter.out.integration;

final class ModuleVersions {

    private static final String DEVELOPMENT_VERSION = "development";

    private ModuleVersions() {}

    static String resolve(Class<?> anchor) {
        Package modulePackage = anchor.getPackage();
        if (modulePackage == null) {
            return DEVELOPMENT_VERSION;
        }
        String version = modulePackage.getImplementationVersion();
        if (version == null || version.isBlank()) {
            version = modulePackage.getSpecificationVersion();
        }
        return version == null || version.isBlank() ? DEVELOPMENT_VERSION : version.trim();
    }
}
