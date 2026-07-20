package com.smbtech.serviceframework.starter.errorhandling.adapter.in.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smbtech.serviceframework.commons.notification.Notification;
import com.smbtech.serviceframework.starter.errorhandling.serialization.NotificationJsonSerializer;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverters;

class NotificationWebMvcConfigurerTest {

    @Test
    void registersNotificationConverterWithoutChangingApplicationMapper() throws Exception {
        ObjectMapper applicationMapper = new ObjectMapper();
        JsonSerializer<?> originalSerializer =
                applicationMapper
                        .getSerializerProviderInstance()
                        .findValueSerializer(Notification.class);
        NotificationWebMvcConfigurer configurer =
                new NotificationWebMvcConfigurer(applicationMapper);
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
        JsonSerializer<?> unchangedSerializer =
                applicationMapper
                        .getSerializerProviderInstance()
                        .findValueSerializer(Notification.class);
        assertSame(originalSerializer, unchangedSerializer);
        assertFalse(unchangedSerializer instanceof NotificationJsonSerializer);
    }

    @Test
    void rejectsMissingConverterAndMapper() {
        assertThrows(
                NullPointerException.class,
                () -> new NotificationWebMvcConfigurer((ObjectMapper) null));
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
