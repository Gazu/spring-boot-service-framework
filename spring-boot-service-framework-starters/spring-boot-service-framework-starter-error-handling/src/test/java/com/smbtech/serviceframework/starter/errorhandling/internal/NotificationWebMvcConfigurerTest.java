package com.smbtech.serviceframework.starter.errorhandling.internal;

import static com.smbtech.serviceframework.starter.errorhandling.serialization.ErrorHandlingSerializationTestFixtures.serializer;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverters;
import tools.jackson.databind.ObjectMapper;

class NotificationWebMvcConfigurerTest {

    @Test
    void registersNotificationConverterWithoutChangingApplicationMapper() throws Exception {
        ObjectMapper applicationMapper = new ObjectMapper();
        var originalModules = applicationMapper.registeredModules();
        NotificationHttpMessageConverter converter =
                new NotificationHttpMessageConverter(applicationMapper, serializer());
        NotificationWebMvcConfigurer configurer = new NotificationWebMvcConfigurer(converter);
        HttpMessageConverters.ServerBuilder builder =
                HttpMessageConverters.forServer().registerDefaults();

        configurer.configureMessageConverters(builder);
        List<HttpMessageConverter<?>> converters = toList(builder.build());

        assertTrue(
                converters.stream().anyMatch(NotificationHttpMessageConverter.class::isInstance));
        assertSame(
                configurer.notificationConverter(),
                converters.stream()
                        .filter(NotificationHttpMessageConverter.class::isInstance)
                        .findFirst()
                        .orElseThrow());
        assertEquals(originalModules, applicationMapper.registeredModules());
    }

    @Test
    void rejectsMissingConverterAndMapper() {
        assertThrows(
                NullPointerException.class,
                () -> new NotificationHttpMessageConverter(null, serializer()));
        assertThrows(
                NullPointerException.class,
                () -> new NotificationWebMvcConfigurer((NotificationHttpMessageConverter) null));
    }

    private static List<HttpMessageConverter<?>> toList(HttpMessageConverters converters) {
        java.util.ArrayList<HttpMessageConverter<?>> result = new java.util.ArrayList<>();
        converters.forEach(result::add);
        return List.copyOf(result);
    }
}
