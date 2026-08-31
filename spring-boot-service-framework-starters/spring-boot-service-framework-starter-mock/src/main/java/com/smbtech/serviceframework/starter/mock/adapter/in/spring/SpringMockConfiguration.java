package com.smbtech.serviceframework.starter.mock.adapter.in.spring;

import com.smbtech.serviceframework.mock.port.in.MockResponder;
import com.smbtech.serviceframework.starter.mock.api.MockService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

@Configuration(proxyBeanMethods = false)
class SpringMockConfiguration {

    @Bean
    @ConditionalOnMissingBean
    MockService mockService(
            MockResponder mockResponder, ObjectProvider<ObjectMapper> objectMapper) {
        return new SpringMockService(
                mockResponder,
                new MockResponseEntityMapper(objectMapper.getIfAvailable(ObjectMapper::new)));
    }
}
