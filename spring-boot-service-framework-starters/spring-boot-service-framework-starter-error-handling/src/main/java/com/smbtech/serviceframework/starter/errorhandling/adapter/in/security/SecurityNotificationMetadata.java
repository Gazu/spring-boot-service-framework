package com.smbtech.serviceframework.starter.errorhandling.adapter.in.security;

import com.smbtech.serviceframework.commons.notification.Notification;
import com.smbtech.serviceframework.error.ResolvedError;
import com.smbtech.serviceframework.error.metadata.StandardErrorMetadata;
import com.smbtech.serviceframework.starter.errorhandling.api.security.OAuth2SecurityMetadataFactory;
import com.smbtech.serviceframework.starter.errorhandling.api.security.SecurityFailureContext;
import com.smbtech.serviceframework.starter.errorhandling.api.security.SecurityFailureResolution;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

final class SecurityNotificationMetadata {

    private SecurityNotificationMetadata() {}

    static SecurityFailureResolution apply(
            SecurityFailureContext context,
            SecurityFailureResolution resolution,
            OAuth2SecurityMetadataFactory metadataFactory) {
        StandardErrorMetadata standardMetadata =
                Objects.requireNonNull(metadataFactory, "metadataFactory must not be null")
                        .create(context, resolution);
        ResolvedError resolvedError = resolution.resolvedError();
        Notification source = resolvedError.notification();
        Map<String, Object> metadata = new LinkedHashMap<>(source.metadata());
        metadata.putAll(standardMetadata.toMap());
        Notification notification =
                new Notification(
                        source.code(),
                        source.message(),
                        source.severity(),
                        source.fieldName(),
                        metadata,
                        source.id(),
                        source.timestamp());
        return new SecurityFailureResolution(
                new ResolvedError(
                        notification,
                        resolvedError.category(),
                        resolvedError.exposure(),
                        resolvedError.diagnosticMessage(),
                        resolvedError.fieldViolations()),
                resolution.reason(),
                resolution.oauth2Error(),
                resolution.bearerChallenge());
    }
}
