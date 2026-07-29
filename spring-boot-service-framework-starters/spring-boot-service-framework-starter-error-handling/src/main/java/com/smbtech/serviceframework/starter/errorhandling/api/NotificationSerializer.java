package com.smbtech.serviceframework.starter.errorhandling.api;

import com.smbtech.serviceframework.commons.notification.Notification;
import java.io.IOException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;

/** Serializes the public notification response contract. */
@FunctionalInterface
public interface NotificationSerializer {

    /**
     * Writes a notification to the current JSON generator.
     *
     * @param notification notification to serialize
     * @param generator JSON generator
     * @param serializers Jackson serializer provider
     * @throws IOException when serialization fails
     */
    void serialize(
            Notification notification, JsonGenerator generator, SerializationContext serializers)
            throws IOException;
}
