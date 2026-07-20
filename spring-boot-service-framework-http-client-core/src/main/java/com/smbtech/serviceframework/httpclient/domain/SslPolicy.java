package com.smbtech.serviceframework.httpclient.domain;

import java.util.Objects;

/**
 * Carries immutable ssl policy data.
 *
 * @param enabled enabled value
 * @param trustStoreId trust store id value
 * @param keyStoreId key store id value
 */
public record SslPolicy(boolean enabled, String trustStoreId, String keyStoreId) {
    /**
     * Performs the disabled operation.
     *
     * @return disabled result
     */
    public static SslPolicy disabled() {
        return new SslPolicy(false, "", "");
    }

    /** Creates and validates the record components. */
    public SslPolicy {
        trustStoreId = Objects.requireNonNullElse(trustStoreId, "").trim();
        keyStoreId = Objects.requireNonNullElse(keyStoreId, "").trim();
    }

    /**
     * Performs the uses configured stores operation.
     *
     * @return uses configured stores result
     */
    public boolean usesConfiguredStores() {
        return !trustStoreId.isBlank() || !keyStoreId.isBlank();
    }
}
