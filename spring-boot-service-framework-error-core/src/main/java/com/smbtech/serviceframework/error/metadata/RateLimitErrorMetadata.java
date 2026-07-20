package com.smbtech.serviceframework.error.metadata;

import java.util.Map;

/**
 * Public rate-limit retry metadata.
 *
 * @param retryAfterSeconds non-negative delay before retrying
 */
public record RateLimitErrorMetadata(long retryAfterSeconds) {

    /** Creates and validates the record components. */
    public RateLimitErrorMetadata {
        if (retryAfterSeconds < 0) {
            throw new IllegalArgumentException("retryAfterSeconds must not be negative");
        }
    }

    /**
     * Returns this namespace as immutable notification metadata.
     *
     * @return result
     */
    public Map<String, Object> toMap() {
        return Map.of(StandardErrorMetadataKeys.RateLimit.RETRY_AFTER_SECONDS, retryAfterSeconds);
    }
}
