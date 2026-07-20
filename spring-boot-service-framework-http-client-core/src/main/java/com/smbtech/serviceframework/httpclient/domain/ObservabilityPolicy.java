package com.smbtech.serviceframework.httpclient.domain;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Carries immutable observability policy data.
 *
 * @param enabled enabled value
 * @param metricName metric name value
 * @param includeUri include uri value
 * @param includeStatus include status value
 * @param includeException include exception value
 * @param tags tags value
 */
public record ObservabilityPolicy(
        boolean enabled,
        String metricName,
        boolean includeUri,
        boolean includeStatus,
        boolean includeException,
        Map<String, String> tags) {
    private static final String DEFAULT_METRIC_NAME = "smbtech.http.client.requests";

    /**
     * Performs the defaults operation.
     *
     * @return defaults result
     */
    public static ObservabilityPolicy defaults() {
        return new ObservabilityPolicy(true, DEFAULT_METRIC_NAME, false, true, true, Map.of());
    }

    /** Creates and validates the record components. */
    public ObservabilityPolicy {
        metricName =
                metricName == null || metricName.isBlank()
                        ? DEFAULT_METRIC_NAME
                        : metricName.trim();
        tags = Map.copyOf(new LinkedHashMap<>(Objects.requireNonNullElse(tags, Map.of())));
    }
}
