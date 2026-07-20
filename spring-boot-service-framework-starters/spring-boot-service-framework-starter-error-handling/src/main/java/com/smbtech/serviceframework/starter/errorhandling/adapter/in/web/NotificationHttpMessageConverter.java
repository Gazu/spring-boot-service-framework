package com.smbtech.serviceframework.starter.errorhandling.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smbtech.serviceframework.commons.notification.Notification;
import com.smbtech.serviceframework.starter.errorhandling.api.NotificationSerializer;
import com.smbtech.serviceframework.starter.errorhandling.serialization.NotificationJsonResponseWriter;
import com.smbtech.serviceframework.starter.errorhandling.serialization.NotificationJsonSerializer;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;

/** Writes notification responses with an isolated Jackson serializer. */
public final class NotificationHttpMessageConverter
        extends AbstractHttpMessageConverter<Notification> {

    private final NotificationJsonResponseWriter notificationWriter;

    /**
     * Creates a converter from a copy of the application mapper.
     *
     * @param objectMapper application object mapper
     */
    public NotificationHttpMessageConverter(ObjectMapper objectMapper) {
        this(new NotificationJsonResponseWriter(objectMapper));
    }

    /**
     * Creates a converter from an isolated mapper copy and custom serializer.
     *
     * @param objectMapper application object mapper
     * @param serializer notification serializer
     */
    public NotificationHttpMessageConverter(
            ObjectMapper objectMapper, NotificationJsonSerializer serializer) {
        this(new NotificationJsonResponseWriter(objectMapper, serializer));
    }

    /**
     * Creates a converter from an isolated mapper copy and custom serializer.
     *
     * @param objectMapper application object mapper
     * @param serializer notification serializer
     */
    public NotificationHttpMessageConverter(
            ObjectMapper objectMapper, NotificationSerializer serializer) {
        this(new NotificationJsonResponseWriter(objectMapper, serializer));
    }

    /**
     * Creates a converter with a dedicated notification writer.
     *
     * @param notificationWriter notification JSON writer
     */
    public NotificationHttpMessageConverter(NotificationJsonResponseWriter notificationWriter) {
        super(StandardCharsets.UTF_8, MediaType.APPLICATION_JSON);
        this.notificationWriter =
                Objects.requireNonNull(notificationWriter, "notificationWriter must not be null");
    }

    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        return false;
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return Notification.class.isAssignableFrom(clazz);
    }

    @Override
    protected Notification readInternal(
            Class<? extends Notification> clazz, HttpInputMessage inputMessage) {
        throw new HttpMessageNotReadableException(
                "Notification request deserialization is not supported", inputMessage);
    }

    @Override
    protected void writeInternal(Notification notification, HttpOutputMessage outputMessage)
            throws IOException {
        notificationWriter.write(notification, outputMessage.getBody());
    }
}
