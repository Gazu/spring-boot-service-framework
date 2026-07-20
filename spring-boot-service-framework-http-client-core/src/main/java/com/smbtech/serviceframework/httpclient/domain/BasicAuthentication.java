package com.smbtech.serviceframework.httpclient.domain;

import java.util.Objects;

/**
 * Carries immutable basic authentication data.
 *
 * @param username username value
 * @param password password value
 */
public record BasicAuthentication(String username, String password) {
    /** Creates and validates the record components. */
    public BasicAuthentication {
        username = Objects.requireNonNullElse(username, "");
        password = Objects.requireNonNullElse(password, "");
    }

    /**
     * Reports whether configured.
     *
     * @return is configured result
     */
    public boolean isConfigured() {
        return !username.isBlank() && !password.isBlank();
    }
}
