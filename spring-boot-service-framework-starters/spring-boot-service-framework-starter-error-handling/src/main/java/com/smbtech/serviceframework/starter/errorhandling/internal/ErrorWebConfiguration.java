package com.smbtech.serviceframework.starter.errorhandling.internal;

import com.smbtech.serviceframework.commons.notification.Notification;
import com.smbtech.serviceframework.error.ThrowableErrorResolver;
import com.smbtech.serviceframework.starter.errorhandling.api.NotificationSerializer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import tools.jackson.databind.ObjectMapper;

/** Registers package-local MVC adapters behind supported contracts. */
@Configuration(proxyBeanMethods = false)
class ErrorWebConfiguration {

    @Bean("validationExceptionResolver")
    @ConditionalOnMissingBean(name = "validationExceptionResolver")
    ThrowableErrorResolver validationExceptionResolver() {
        return new ValidationExceptionResolver();
    }

    @Bean("springMvcExceptionResolver")
    @ConditionalOnMissingBean(name = "springMvcExceptionResolver")
    ThrowableErrorResolver springMvcExceptionResolver() {
        return new SpringMvcExceptionResolver();
    }

    @Bean("notificationHttpMessageConverter")
    HttpMessageConverter<Notification> notificationHttpMessageConverter(
            ObjectProvider<ObjectMapper> objectMapper, NotificationSerializer serializer) {
        return new NotificationHttpMessageConverter(
                objectMapper.getIfAvailable(ObjectMapper::new), serializer);
    }

    @Bean
    WebMvcConfigurer notificationWebMvcConfigurer(
            @Qualifier("notificationHttpMessageConverter") HttpMessageConverter<Notification> notificationConverter) {
        return new NotificationWebMvcConfigurer(notificationConverter);
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(
            name = "com.smbtech.serviceframework.httpclient.exception.HttpClientResponseException")
    static class HttpClientConfiguration {

        @Bean("httpClientExceptionResolver")
        @ConditionalOnMissingBean(name = "httpClientExceptionResolver")
        ThrowableErrorResolver httpClientExceptionResolver() {
            return new HttpClientExceptionResolver();
        }
    }
}
