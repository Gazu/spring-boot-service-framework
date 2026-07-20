package com.smbtech.serviceframework.httpclient.domain;

import java.util.Objects;

/**
 * Carries immutable key store definition data.
 *
 * @param id id value
 * @param location location value
 * @param base64 base64 value
 * @param type type value
 * @param password password value
 * @param keyAlias key alias value
 * @param keyPassword key password value
 */
public record KeyStoreDefinition(
        String id,
        String location,
        String base64,
        String type,
        String password,
        String keyAlias,
        String keyPassword) {
    /** Creates and validates the record components. */
    public KeyStoreDefinition {
        id = Objects.requireNonNullElse(id, "").trim();
        location = Objects.requireNonNullElse(location, "");
        base64 = Objects.requireNonNullElse(base64, "");
        type = Objects.requireNonNullElse(type, "PKCS12");
        password = Objects.requireNonNullElse(password, "");
        keyAlias = Objects.requireNonNullElse(keyAlias, "");
        keyPassword = keyPassword == null || keyPassword.isBlank() ? password : keyPassword;
    }

    /**
     * Reports whether inline content.
     *
     * @return has inline content result
     */
    public boolean hasInlineContent() {
        return !base64.isBlank();
    }
}
