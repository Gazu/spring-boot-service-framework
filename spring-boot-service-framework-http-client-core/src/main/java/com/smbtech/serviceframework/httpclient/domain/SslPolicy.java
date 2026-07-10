package com.smbtech.serviceframework.httpclient.domain;

import java.util.Objects;

public record SslPolicy(
        boolean enabled,
        String trustStoreId,
        String keyStoreId
) {
    public static SslPolicy disabled() {
        return new SslPolicy(false, "", "");
    }

    public SslPolicy {
        trustStoreId = Objects.requireNonNullElse(trustStoreId, "").trim();
        keyStoreId = Objects.requireNonNullElse(keyStoreId, "").trim();
    }

    public boolean usesConfiguredStores() {
        return !trustStoreId.isBlank() || !keyStoreId.isBlank();
    }
}
