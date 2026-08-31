package com.smbtech.serviceframework.starter.mock.adapter.out.resource;

import com.smbtech.serviceframework.mock.port.out.MockResponseSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import tools.jackson.databind.ObjectMapper;

@Configuration(proxyBeanMethods = false)
class ResourceMockConfiguration {

    @Bean
    @ConditionalOnMissingBean
    MockResponseSource mockResponseSource(
            ResourceLoader resourceLoader, ObjectProvider<ObjectMapper> objectMapper) {
        return new ResourceMockResponseSource(
                resourceLoader, objectMapper.getIfAvailable(ObjectMapper::new));
    }
}
