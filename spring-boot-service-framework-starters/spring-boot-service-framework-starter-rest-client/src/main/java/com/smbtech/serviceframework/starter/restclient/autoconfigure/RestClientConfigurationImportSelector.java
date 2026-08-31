package com.smbtech.serviceframework.starter.restclient.autoconfigure;

import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;

final class RestClientConfigurationImportSelector implements ImportSelector {

    private static final String[] CONFIGURATIONS = {
        "com.smbtech.serviceframework.starter.restclient.autoconfigure.DynamicRestClientRegistrationConfiguration",
        "com.smbtech.serviceframework.starter.restclient.adapter.out.apache.ApacheHttpClientConfiguration",
        "com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.keystore.KeyStoreRuntimeConfiguration",
        "com.smbtech.serviceframework.starter.restclient.adapter.out.spring.RestClientRuntimeConfiguration"
    };

    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
        return CONFIGURATIONS.clone();
    }
}
