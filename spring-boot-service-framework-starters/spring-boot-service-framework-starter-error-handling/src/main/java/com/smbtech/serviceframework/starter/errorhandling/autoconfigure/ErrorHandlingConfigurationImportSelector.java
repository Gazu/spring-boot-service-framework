package com.smbtech.serviceframework.starter.errorhandling.autoconfigure;

import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;

/** Selects package-local error handling configurations without exposing them as public API. */
final class ErrorHandlingConfigurationImportSelector implements ImportSelector {

    private static final String[] CONFIGURATIONS = {
        "com.smbtech.serviceframework.starter.errorhandling.internal.ErrorResponseConfiguration",
        "com.smbtech.serviceframework.starter.errorhandling.customizer.ErrorCustomizationConfiguration",
        "com.smbtech.serviceframework.starter.errorhandling.serialization.ErrorSerializationConfiguration",
        "com.smbtech.serviceframework.starter.errorhandling.internal.ErrorWebConfiguration",
        "com.smbtech.serviceframework.starter.errorhandling.adapter.out.logging.ErrorLoggingConfiguration",
        "com.smbtech.serviceframework.starter.errorhandling.adapter.out.metrics.ErrorMetricsConfiguration",
        "com.smbtech.serviceframework.starter.errorhandling.internal.ErrorSecurityConfiguration",
        "com.smbtech.serviceframework.starter.errorhandling.internal.ErrorSecurityAdapterConfiguration"
    };

    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
        return CONFIGURATIONS.clone();
    }
}
