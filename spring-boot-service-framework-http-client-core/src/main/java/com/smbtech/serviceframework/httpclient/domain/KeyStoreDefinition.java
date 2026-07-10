package com.smbtech.serviceframework.httpclient.domain;

import java.util.Objects;

public record KeyStoreDefinition(
        String id,
        String location,
        String base64,
        String type,
        String password,
        String keyAlias,
        String keyPassword
) {
    public KeyStoreDefinition {
        id = Objects.requireNonNullElse(id, "").trim();
        location = Objects.requireNonNullElse(location, "");
        base64 = Objects.requireNonNullElse(base64, "");
        type = Objects.requireNonNullElse(type, "PKCS12");
        password = Objects.requireNonNullElse(password, "");
        keyAlias = Objects.requireNonNullElse(keyAlias, "");
        keyPassword = keyPassword == null || keyPassword.isBlank() ? password : keyPassword;
    }

    public boolean hasInlineContent() {
        return !base64.isBlank();
    }
}
