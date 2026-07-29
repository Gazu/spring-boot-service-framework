package com.smbtech.serviceframework.starter.errorhandling.adapter.in.web;

import java.util.Objects;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import tools.jackson.databind.ObjectMapper;

/** Adds the notification-only converter without customizing the global mapper. */
public final class NotificationWebMvcConfigurer implements WebMvcConfigurer {

    private final NotificationHttpMessageConverter notificationConverter;

    /**
     * Creates a configurer using an isolated copy of the application mapper.
     *
     * @param objectMapper application object mapper
     */
    public NotificationWebMvcConfigurer(ObjectMapper objectMapper) {
        this(new NotificationHttpMessageConverter(objectMapper));
    }

    /**
     * Creates a configurer with a custom notification converter.
     *
     * @param notificationConverter notification converter
     */
    public NotificationWebMvcConfigurer(NotificationHttpMessageConverter notificationConverter) {
        this.notificationConverter =
                Objects.requireNonNull(
                        notificationConverter, "notificationConverter must not be null");
    }

    @Override
    public void configureMessageConverters(HttpMessageConverters.ServerBuilder builder) {
        builder.addCustomConverter(notificationConverter);
    }

    /**
     * Returns the configured notification converter.
     *
     * @return notification converter
     */
    public NotificationHttpMessageConverter notificationConverter() {
        return notificationConverter;
    }
}
