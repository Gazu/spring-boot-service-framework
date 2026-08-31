package com.smbtech.serviceframework.starter.mock.adapter.in.openapi;

import com.smbtech.serviceframework.starter.mock.autoconfigure.MockProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import tools.jackson.databind.ObjectMapper;

@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "smbtech.mocks.openapi", name = "enabled", havingValue = "true")
class OpenApiMockConfiguration {

    @Bean(name = "openApiMockServerRegistrar")
    @ConditionalOnMissingBean(name = "openApiMockServerRegistrar")
    SmartInitializingSingleton openApiMockServerRegistrar(
            MockProperties properties,
            ResourceLoader resourceLoader,
            ObjectProvider<ObjectMapper> objectMapper,
            RequestMappingHandlerMapping handlerMapping,
            Environment environment) {
        OpenApiMockContractLoader contractLoader =
                new OpenApiMockContractLoader(
                        resourceLoader, objectMapper.getIfAvailable(ObjectMapper::new));
        return new OpenApiMockServerRegistrar(
                properties.getOpenapi(), contractLoader, handlerMapping, environment);
    }
}
