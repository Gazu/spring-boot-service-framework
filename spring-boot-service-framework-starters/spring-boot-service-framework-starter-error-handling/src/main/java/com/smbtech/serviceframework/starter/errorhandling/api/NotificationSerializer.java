package com.smbtech.serviceframework.starter.errorhandling.api;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.smbtech.serviceframework.commons.notification.Notification;
import java.io.IOException;

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
            Notification notification, JsonGenerator generator, SerializerProvider serializers)
            throws IOException;
}
