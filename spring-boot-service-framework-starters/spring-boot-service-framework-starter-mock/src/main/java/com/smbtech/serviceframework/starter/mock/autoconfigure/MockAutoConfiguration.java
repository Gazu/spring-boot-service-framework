package com.smbtech.serviceframework.starter.mock.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smbtech.serviceframework.mock.port.in.MockCatalog;
import com.smbtech.serviceframework.mock.port.in.MockResponder;
import com.smbtech.serviceframework.mock.port.out.MockDefinitionSource;
import com.smbtech.serviceframework.mock.port.out.MockResponseSource;
import com.smbtech.serviceframework.mock.service.DefaultMockCatalog;
import com.smbtech.serviceframework.mock.service.DefaultMockResponder;
import com.smbtech.serviceframework.starter.mock.adapter.in.spring.MockResponseEntityMapper;
import com.smbtech.serviceframework.starter.mock.adapter.in.spring.SpringMockService;
import com.smbtech.serviceframework.starter.mock.adapter.out.properties.PropertiesMockDefinitionSource;
import com.smbtech.serviceframework.starter.mock.adapter.out.resource.ResourceMockResponseSource;
import com.smbtech.serviceframework.starter.mock.api.mock.MockService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ResourceLoader;

@AutoConfiguration
@ConditionalOnClass(ObjectMapper.class)
@EnableConfigurationProperties(MockProperties.class)
public class MockAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    MockDefinitionSource mockDefinitionSource(MockProperties properties) {
        return new PropertiesMockDefinitionSource(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    MockCatalog mockCatalog(MockDefinitionSource source) {
        return new DefaultMockCatalog(source);
    }

    @Bean
    @ConditionalOnMissingBean
    MockResponseSource mockResponseSource(
            ResourceLoader resourceLoader,
            ObjectProvider<ObjectMapper> objectMapper
    ) {
        return new ResourceMockResponseSource(
                resourceLoader,
                objectMapper.getIfAvailable(this::fallbackObjectMapper)
        );
    }

    @Bean
    @ConditionalOnMissingBean
    MockResponder mockResponder(
            MockCatalog catalog,
            MockResponseSource responseSource
    ) {
        return new DefaultMockResponder(catalog, responseSource);
    }

    @Bean
    @ConditionalOnMissingBean
    MockResponseEntityMapper mockResponseEntityMapper(
            ObjectProvider<ObjectMapper> objectMapper
    ) {
        return new MockResponseEntityMapper(
                objectMapper.getIfAvailable(this::fallbackObjectMapper)
        );
    }

    @Bean
    @ConditionalOnMissingBean
    MockService mockService(
            MockResponder mockResponder,
            MockResponseEntityMapper responseEntityMapper
    ) {
        return new SpringMockService(mockResponder, responseEntityMapper);
    }

    private ObjectMapper fallbackObjectMapper() {
        return new ObjectMapper().findAndRegisterModules();
    }
}
