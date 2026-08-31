package com.smbtech.serviceframework.starter.errorhandling.serialization;

import com.smbtech.serviceframework.commons.notification.Notification;
import com.smbtech.serviceframework.starter.errorhandling.api.NotificationResponseWriter;
import com.smbtech.serviceframework.starter.errorhandling.api.NotificationSerializer;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectWriter;
import tools.jackson.databind.module.SimpleModule;

/** Writes notification JSON with a dedicated mapper copy and snake-case serializer. */
final class NotificationJsonResponseWriter implements NotificationResponseWriter {

    private final ObjectWriter objectWriter;

    /**
     * Creates a writer from an isolated copy of the application mapper.
     *
     * @param objectMapper application object mapper
     */
    NotificationJsonResponseWriter(ObjectMapper objectMapper) {
        this(objectMapper, new NotificationJsonSerializer());
    }

    /**
     * Creates a writer from an isolated mapper copy and a custom serializer.
     *
     * @param objectMapper application object mapper
     * @param serializer notification serializer
     */
    NotificationJsonResponseWriter(
            ObjectMapper objectMapper, NotificationJsonSerializer serializer) {
        this(objectMapper, (NotificationSerializer) serializer);
    }

    /**
     * Creates a writer from an isolated mapper copy and a custom serializer.
     *
     * @param objectMapper application object mapper
     * @param serializer notification serializer
     */
    NotificationJsonResponseWriter(ObjectMapper objectMapper, NotificationSerializer serializer) {
        SimpleModule module = new SimpleModule("service-framework-notification-json");
        NotificationSerializer safeSerializer =
                Objects.requireNonNull(serializer, "serializer must not be null");
        module.addSerializer(Notification.class, new JsonSerializerAdapter(safeSerializer));
        ObjectMapper isolatedMapper =
                Objects.requireNonNull(objectMapper, "objectMapper must not be null")
                        .rebuild()
                        .addModule(module)
                        .build();
        this.objectWriter = isolatedMapper.writerFor(Notification.class);
    }

    @Override
    public void write(
            ResponseEntity<Notification> responseEntity, HttpServletResponse servletResponse)
            throws IOException {
        ResponseEntity<Notification> source =
                Objects.requireNonNull(responseEntity, "responseEntity must not be null");
        HttpServletResponse response =
                Objects.requireNonNull(servletResponse, "servletResponse must not be null");
        Notification body =
                Objects.requireNonNull(source.getBody(), "responseEntity body must not be null");

        response.setStatus(source.getStatusCode().value());
        copyHeaders(source.getHeaders(), response);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        if (response.getContentType() == null) {
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        }
        write(body, response.getOutputStream());
    }

    /**
     * Writes only the notification body to an output stream.
     *
     * @param notification notification body
     * @param outputStream destination stream
     * @throws IOException when JSON cannot be written
     */
    void write(Notification notification, OutputStream outputStream) throws IOException {
        objectWriter.writeValue(
                Objects.requireNonNull(outputStream, "outputStream must not be null"),
                Objects.requireNonNull(notification, "notification must not be null"));
    }

    private static void copyHeaders(HttpHeaders headers, HttpServletResponse response) {
        headers.forEach((name, values) -> setHeader(response, name, values));
    }

    private static void setHeader(HttpServletResponse response, String name, List<String> values) {
        if (values.isEmpty()) {
            return;
        }
        response.setHeader(name, values.getFirst());
        for (int index = 1; index < values.size(); index++) {
            response.addHeader(name, values.get(index));
        }
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
