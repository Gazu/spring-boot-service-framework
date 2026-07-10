package com.smbtech.serviceframework.mock.domain;

import java.time.Duration;
import java.util.Objects;

public record MockDefinition(
        String key,
        boolean enabled,
        String file,
        Duration delay
) {
    public MockDefinition {
        key = Objects.requireNonNullElse(key, "").trim();
        file = Objects.requireNonNullElse(file, "").trim();
        delay = Objects.requireNonNullElse(delay, Duration.ZERO);
    }

    public boolean isUsable() {
        return enabled && !file.isBlank();
    }

    public static MockDefinition disabled(String key) {
        return new MockDefinition(key, false, "", Duration.ZERO);
    }
}
