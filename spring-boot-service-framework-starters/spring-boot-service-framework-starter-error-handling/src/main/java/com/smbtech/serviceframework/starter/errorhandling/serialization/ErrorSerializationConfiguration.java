package com.smbtech.serviceframework.starter.errorhandling.serialization;

import com.smbtech.serviceframework.starter.errorhandling.api.NotificationResponseWriter;
import com.smbtech.serviceframework.starter.errorhandling.api.NotificationSerializer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

/** Registers isolated notification serialization and MVC integration. */
@Configuration(proxyBeanMethods = false)
class ErrorSerializationConfiguration {

    @Bean
    NotificationMetadataKeyNormalizer notificationMetadataKeyNormalizer() {
        return new NotificationMetadataKeyNormalizer();
    }

    @Bean
    @ConditionalOnMissingBean(NotificationSerializer.class)
    NotificationSerializer notificationSerializer(
            NotificationMetadataKeyNormalizer metadataKeyNormalizer) {
        return new NotificationJsonSerializer(metadataKeyNormalizer);
    }

    @Bean
    @ConditionalOnMissingBean(NotificationResponseWriter.class)
    NotificationResponseWriter notificationResponseWriter(
            ObjectProvider<ObjectMapper> objectMapper, NotificationSerializer serializer) {
        return new NotificationJsonResponseWriter(
                objectMapper.getIfAvailable(ObjectMapper::new), serializer);
    }
}
