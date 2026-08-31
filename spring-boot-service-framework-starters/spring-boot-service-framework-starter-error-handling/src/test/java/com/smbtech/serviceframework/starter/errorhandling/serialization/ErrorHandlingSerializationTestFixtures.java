package com.smbtech.serviceframework.starter.errorhandling.serialization;

import com.smbtech.serviceframework.commons.notification.Notification;
import com.smbtech.serviceframework.starter.errorhandling.api.NotificationResponseWriter;
import com.smbtech.serviceframework.starter.errorhandling.api.NotificationSerializer;
import java.io.IOException;
import java.io.OutputStream;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ValueSerializer;

/** Creates package-owned serialization defaults for tests outside this package. */
public final class ErrorHandlingSerializationTestFixtures {

    private ErrorHandlingSerializationTestFixtures() {}

    public static NotificationSerializer serializer() {
        return new NotificationJsonSerializer();
    }

    public static ValueSerializer<Notification> jacksonSerializer() {
        return new NotificationJsonSerializer();
    }

    public static NotificationResponseWriter responseWriter(ObjectMapper objectMapper) {
        return new NotificationJsonResponseWriter(objectMapper);
    }

    public static void write(
            Notification notification, OutputStream outputStream, ObjectMapper objectMapper)
            throws IOException {
        new NotificationJsonResponseWriter(objectMapper).write(notification, outputStream);
    }
}
