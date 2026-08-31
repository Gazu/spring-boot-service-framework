package com.smbtech.serviceframework.starter.mock.adapter.out.properties;

import com.smbtech.serviceframework.mock.port.out.MockDefinitionSource;
import com.smbtech.serviceframework.starter.mock.autoconfigure.MockProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class PropertiesMockConfiguration {

    @Bean
    @ConditionalOnMissingBean
    MockDefinitionSource mockDefinitionSource(MockProperties properties) {
        return new PropertiesMockDefinitionSource(properties);
    }
}
