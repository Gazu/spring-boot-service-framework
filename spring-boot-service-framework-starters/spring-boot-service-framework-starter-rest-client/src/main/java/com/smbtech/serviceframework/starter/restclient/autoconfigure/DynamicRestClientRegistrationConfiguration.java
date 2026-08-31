package com.smbtech.serviceframework.starter.restclient.autoconfigure;

import static org.springframework.beans.factory.config.BeanDefinition.ROLE_INFRASTRUCTURE;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;

@Configuration(proxyBeanMethods = false)
class DynamicRestClientRegistrationConfiguration {

    @Bean
    @Role(ROLE_INFRASTRUCTURE)
    static RestClientBeanRegistrar restClientBeanRegistrar() {
        return new RestClientBeanRegistrar();
    }
}
