package com.smbtech.serviceframework.starter.errorhandling.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.smbtech.serviceframework.commons.notification.Notification;
import com.smbtech.serviceframework.starter.errorhandling.api.NotificationSerializer;
import java.io.IOException;
import java.util.Objects;

/**
 * Serializes {@link Notification} with a stable snake-case JSON contract. Registration is
 * intentionally local to the writer or mapper used for error responses.
 */
public final class NotificationJsonSerializer extends JsonSerializer<Notification>
        implements NotificationSerializer {

    private final NotificationMetadataKeyNormalizer metadataKeyNormalizer;

    /** Creates a serializer with the default metadata key normalizer. */
    public NotificationJsonSerializer() {
        this(new NotificationMetadataKeyNormalizer());
    }

    /**
     * Creates a serializer with a custom metadata key normalizer.
     *
     * @param metadataKeyNormalizer metadata key normalizer
     */
    public NotificationJsonSerializer(NotificationMetadataKeyNormalizer metadataKeyNormalizer) {
        this.metadataKeyNormalizer =
                Objects.requireNonNull(
                        metadataKeyNormalizer, "metadataKeyNormalizer must not be null");
    }

    @Override
    public void serialize(
            Notification notification, JsonGenerator generator, SerializerProvider serializers)
            throws IOException {
        Notification source = Objects.requireNonNull(notification, "notification must not be null");
        generator.writeStartObject(source);
        generator.writeStringField("code", source.code());
        generator.writeStringField("message", source.message());
        generator.writeStringField("severity", source.severity().name());
        generator.writeStringField("field_name", source.fieldName());
        generator.writeFieldName("metadata");
        serializers.defaultSerializeValue(
                metadataKeyNormalizer.normalize(source.metadata()), generator);
        generator.writeStringField("id", source.id().toString());
        generator.writeStringField("timestamp", source.timestamp().toString());
        generator.writeEndObject();
    }

    /**
     * Returns the configured metadata key normalizer.
     *
     * @return metadata key normalizer
     */
    public NotificationMetadataKeyNormalizer metadataKeyNormalizer() {
        return metadataKeyNormalizer;
    }
}
