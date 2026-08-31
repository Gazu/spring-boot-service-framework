package com.smbtech.serviceframework.starter.errorhandling.internal;

import com.smbtech.serviceframework.commons.notification.Notification;
import com.smbtech.serviceframework.error.ResolvedError;
import com.smbtech.serviceframework.error.metadata.StandardErrorMetadata;
import com.smbtech.serviceframework.starter.errorhandling.api.security.OAuth2SecurityMetadataFactory;
import com.smbtech.serviceframework.starter.errorhandling.api.security.SecurityFailureContext;
import com.smbtech.serviceframework.starter.errorhandling.api.security.SecurityFailureResolution;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

final class SecurityFailureMetadataEnricher {

    private final OAuth2SecurityMetadataFactory metadataFactory;

    SecurityFailureMetadataEnricher(OAuth2SecurityMetadataFactory metadataFactory) {
        this.metadataFactory =
                Objects.requireNonNull(metadataFactory, "metadataFactory must not be null");
    }

    SecurityFailureResolution enrich(
            SecurityFailureContext context, SecurityFailureResolution resolution) {
        SecurityFailureContext safeContext =
                Objects.requireNonNull(context, "context must not be null");
        SecurityFailureResolution safeResolution =
                Objects.requireNonNull(resolution, "resolution must not be null");
        StandardErrorMetadata standardMetadata =
                Objects.requireNonNull(
                        metadataFactory.create(safeContext, safeResolution),
                        "OAuth2SecurityMetadataFactory must not return null");
        ResolvedError resolvedError = safeResolution.resolvedError();
        Notification source = resolvedError.notification();
        Map<String, Object> metadata = new LinkedHashMap<>(source.metadata());
        metadata.putAll(standardMetadata.toMap());
        return safeResolution.withResolvedError(
                resolvedError.withNotification(source.withMetadata(metadata)));
    }
}
