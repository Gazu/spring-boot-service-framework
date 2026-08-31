package com.smbtech.serviceframework.starter.restclient.autoconfigure;

import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;

final class OAuth2RestClientAuthenticationConfigurationImportSelector implements ImportSelector {

    private static final String[] CONFIGURATIONS = {
        "com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring.OAuth2AuthenticationConfiguration"
    };

    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
        return CONFIGURATIONS.clone();
    }
}
