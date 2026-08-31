package com.smbtech.serviceframework.starter.mock.autoconfigure;

import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;

/** Selects package-local mock configurations without exposing their adapters. */
final class MockConfigurationImportSelector implements ImportSelector {

    private static final String[] CONFIGURATIONS = {
        "com.smbtech.serviceframework.starter.mock.adapter.out.properties.PropertiesMockConfiguration",
        "com.smbtech.serviceframework.starter.mock.adapter.out.resource.ResourceMockConfiguration",
        "com.smbtech.serviceframework.starter.mock.adapter.in.spring.SpringMockConfiguration",
        "com.smbtech.serviceframework.starter.mock.adapter.out.restclient.RestClientMockConfiguration",
        "com.smbtech.serviceframework.starter.mock.adapter.in.openapi.OpenApiMockConfiguration"
    };

    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
        return CONFIGURATIONS.clone();
    }
}
