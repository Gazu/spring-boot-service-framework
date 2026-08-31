package com.smbtech.serviceframework.starter.actuator.adapter.out.integration;

import com.smbtech.serviceframework.actuator.port.out.DiagnosticProbe;
import com.smbtech.serviceframework.actuator.port.out.FrameworkModuleInfoProvider;
import com.smbtech.serviceframework.starter.errorhandling.autoconfigure.ErrorHandlingProperties;
import com.smbtech.serviceframework.starter.logging.autoconfigure.LoggingProperties;
import com.smbtech.serviceframework.starter.mock.autoconfigure.MockProperties;
import com.smbtech.serviceframework.starter.restclient.api.RestClientRegistry;
import com.smbtech.serviceframework.starter.restclient.autoconfigure.RestClientProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class ActuatorIntegrationConfiguration {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass({RestClientProperties.class, RestClientRegistry.class})
    @ConditionalOnBean({RestClientProperties.class, RestClientRegistry.class})
    static class RestClientIntegrationConfiguration {

        @Bean(name = "serviceFrameworkRestClientDiagnosticProbe")
        @ConditionalOnMissingBean(name = "serviceFrameworkRestClientDiagnosticProbe")
        DiagnosticProbe serviceFrameworkRestClientDiagnosticProbe(
                RestClientProperties properties, RestClientRegistry registry) {
            return new RestClientDiagnosticProbe(properties, registry);
        }

        @Bean(name = "serviceFrameworkRestClientModuleInfoProvider")
        @ConditionalOnMissingBean(name = "serviceFrameworkRestClientModuleInfoProvider")
        FrameworkModuleInfoProvider serviceFrameworkRestClientModuleInfoProvider(
                RestClientProperties properties, RestClientRegistry registry) {
            return new RestClientModuleInfoProvider(properties, registry);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(MockProperties.class)
    @ConditionalOnBean(MockProperties.class)
    static class MockIntegrationConfiguration {

        @Bean(name = "serviceFrameworkMockModuleInfoProvider")
        @ConditionalOnMissingBean(name = "serviceFrameworkMockModuleInfoProvider")
        FrameworkModuleInfoProvider serviceFrameworkMockModuleInfoProvider(
                MockProperties properties) {
            return new MockModuleInfoProvider(properties);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(LoggingProperties.class)
    @ConditionalOnBean(LoggingProperties.class)
    static class LoggingIntegrationConfiguration {

        @Bean(name = "serviceFrameworkLoggingModuleInfoProvider")
        @ConditionalOnMissingBean(name = "serviceFrameworkLoggingModuleInfoProvider")
        FrameworkModuleInfoProvider serviceFrameworkLoggingModuleInfoProvider(
                LoggingProperties properties) {
            return new LoggingModuleInfoProvider(properties);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(ErrorHandlingProperties.class)
    @ConditionalOnBean(ErrorHandlingProperties.class)
    static class ErrorHandlingIntegrationConfiguration {

        @Bean(name = "serviceFrameworkErrorHandlingModuleInfoProvider")
        @ConditionalOnMissingBean(name = "serviceFrameworkErrorHandlingModuleInfoProvider")
        FrameworkModuleInfoProvider serviceFrameworkErrorHandlingModuleInfoProvider(
                ErrorHandlingProperties properties) {
            return new ErrorHandlingModuleInfoProvider(properties);
        }
    }
}
