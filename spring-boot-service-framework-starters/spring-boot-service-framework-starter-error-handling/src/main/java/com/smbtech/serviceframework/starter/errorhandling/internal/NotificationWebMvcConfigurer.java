package com.smbtech.serviceframework.starter.errorhandling.internal;

import java.util.Objects;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Adds the notification-only converter without customizing the global mapper. */
final class NotificationWebMvcConfigurer implements WebMvcConfigurer {

    private final HttpMessageConverter<?> notificationConverter;

    /**
     * Creates a configurer with a custom notification converter.
     *
     * @param notificationConverter notification converter
     */
    public NotificationWebMvcConfigurer(HttpMessageConverter<?> notificationConverter) {
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
    public HttpMessageConverter<?> notificationConverter() {
        return notificationConverter;
    }
}
