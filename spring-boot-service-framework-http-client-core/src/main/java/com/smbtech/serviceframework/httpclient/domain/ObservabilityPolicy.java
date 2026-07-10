package com.smbtech.serviceframework.httpclient.domain;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record ObservabilityPolicy(
        boolean enabled,
        String metricName,
        boolean includeUri,
        boolean includeStatus,
        boolean includeException,
        Map<String, String> tags
) {
    private static final String DEFAULT_METRIC_NAME = "smbtech.http.client.requests";

    public static ObservabilityPolicy defaults() {
        return new ObservabilityPolicy(true, DEFAULT_METRIC_NAME, false, true, true, Map.of());
    }

    public ObservabilityPolicy {
        metricName = metricName == null || metricName.isBlank() ? DEFAULT_METRIC_NAME : metricName.trim();
        tags = Map.copyOf(new LinkedHashMap<>(Objects.requireNonNullElse(tags, Map.of())));
    }
}
