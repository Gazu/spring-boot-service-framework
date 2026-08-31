package com.smbtech.serviceframework.starter.errorhandling.internal;

import com.smbtech.serviceframework.commons.notification.Notification;
import com.smbtech.serviceframework.starter.errorhandling.api.NotificationSerializer;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectWriter;
import tools.jackson.databind.module.SimpleModule;

/** Writes notification responses with an isolated Jackson serializer. */
final class NotificationHttpMessageConverter extends AbstractHttpMessageConverter<Notification> {

    private final ObjectWriter objectWriter;

    NotificationHttpMessageConverter(
            ObjectMapper objectMapper, NotificationSerializer notificationSerializer) {
        super(StandardCharsets.UTF_8, MediaType.APPLICATION_JSON);
        NotificationSerializer serializer =
                Objects.requireNonNull(
                        notificationSerializer, "notificationSerializer must not be null");
        SimpleModule module = new SimpleModule("service-framework-notification-http-json");
        module.addSerializer(Notification.class, new JsonSerializerAdapter(serializer));
        ObjectMapper isolatedMapper =
                Objects.requireNonNull(objectMapper, "objectMapper must not be null")
                        .rebuild()
                        .addModule(module)
                        .build();
        this.objectWriter = isolatedMapper.writerFor(Notification.class);
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
        objectWriter.writeValue(outputMessage.getBody(), notification);
    }

    private static final class JsonSerializerAdapter
            extends tools.jackson.databind.ValueSerializer<Notification> {
        private final NotificationSerializer serializer;

        private JsonSerializerAdapter(NotificationSerializer serializer) {
            this.serializer = serializer;
        }

        @Override
        public void serialize(
                Notification notification,
                tools.jackson.core.JsonGenerator generator,
                tools.jackson.databind.SerializationContext serializers) {
            try {
                serializer.serialize(notification, generator, serializers);
            } catch (IOException exception) {
                throw tools.jackson.core.JacksonException.wrapWithPath(
                        exception, notification, "notification");
            }
        }
    }
}
