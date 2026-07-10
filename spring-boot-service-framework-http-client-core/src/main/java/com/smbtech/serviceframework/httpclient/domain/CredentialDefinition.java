package com.smbtech.serviceframework.httpclient.domain;

import java.util.Objects;

public record CredentialDefinition(
        String id,
        String value
) {
    public CredentialDefinition {
        id = Objects.requireNonNullElse(id, "").trim();
        value = Objects.requireNonNullElse(value, "");
    }
}
