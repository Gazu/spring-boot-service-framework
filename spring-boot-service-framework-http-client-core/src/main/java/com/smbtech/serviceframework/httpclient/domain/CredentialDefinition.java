package com.smbtech.serviceframework.httpclient.domain;

import java.util.Objects;

/**
 * Carries immutable credential definition data.
 *
 * @param id id value
 * @param value plain-text credential value
 */
public record CredentialDefinition(String id, String value) {
    /** Creates and validates the record components. */
    public CredentialDefinition {
        id = Objects.requireNonNullElse(id, "").trim();
        value = Objects.requireNonNullElse(value, "");
    }
}
