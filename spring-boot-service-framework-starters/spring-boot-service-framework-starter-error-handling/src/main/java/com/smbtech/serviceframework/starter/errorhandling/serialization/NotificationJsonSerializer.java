package com.smbtech.serviceframework.starter.errorhandling.serialization;

import com.smbtech.serviceframework.commons.notification.Notification;
import com.smbtech.serviceframework.starter.errorhandling.api.NotificationSerializer;
import java.util.Objects;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

/**
 * Serializes {@link Notification} with a stable snake-case JSON contract. Registration is
 * intentionally local to the writer or mapper used for error responses.
 */
public final class NotificationJsonSerializer extends ValueSerializer<Notification>
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
            Notification notification, JsonGenerator generator, SerializationContext serializers) {
        Notification source = Objects.requireNonNull(notification, "notification must not be null");
        generator.writeStartObject(source);
        generator.writeStringProperty("code", source.code());
        generator.writeStringProperty("message", source.message());
        generator.writeStringProperty("severity", source.severity().name());
        generator.writeStringProperty("field_name", source.fieldName());
        generator.writeName("metadata");
        serializers.writeValue(generator, metadataKeyNormalizer.normalize(source.metadata()));
        generator.writeStringProperty("id", source.id().toString());
        generator.writeStringProperty("timestamp", source.timestamp().toString());
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
