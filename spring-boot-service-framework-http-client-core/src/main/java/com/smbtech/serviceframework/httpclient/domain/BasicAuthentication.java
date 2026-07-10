package com.smbtech.serviceframework.httpclient.domain;

import java.util.Objects;

public record BasicAuthentication(
        String username,
        String password
) {
    public BasicAuthentication {
        username = Objects.requireNonNullElse(username, "");
        password = Objects.requireNonNullElse(password, "");
    }

    public boolean isConfigured() {
        return !username.isBlank() && !password.isBlank();
    }
}
