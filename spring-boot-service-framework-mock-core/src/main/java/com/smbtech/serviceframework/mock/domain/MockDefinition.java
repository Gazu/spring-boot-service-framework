package com.smbtech.serviceframework.mock.domain;

import java.time.Duration;
import java.util.Objects;

/**
 * Carries immutable mock definition data.
 *
 * @param key key value
 * @param enabled enabled value
 * @param file file value
 * @param delay delay value
 */
public record MockDefinition(String key, boolean enabled, String file, Duration delay) {
    /** Creates and validates the record components. */
    public MockDefinition {
        key = Objects.requireNonNullElse(key, "").trim();
        file = Objects.requireNonNullElse(file, "").trim();
        delay = Objects.requireNonNullElse(delay, Duration.ZERO);
    }

    /**
     * Reports whether usable.
     *
     * @return is usable result
     */
    public boolean isUsable() {
        return enabled && !file.isBlank();
    }

    /**
     * Performs the disabled operation.
     *
     * @param key key value
     * @return disabled result
     */
    public static MockDefinition disabled(String key) {
        return new MockDefinition(key, false, "", Duration.ZERO);
    }
}
