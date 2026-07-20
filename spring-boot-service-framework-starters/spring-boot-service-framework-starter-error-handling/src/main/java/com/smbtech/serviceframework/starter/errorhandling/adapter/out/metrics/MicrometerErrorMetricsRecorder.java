package com.smbtech.serviceframework.starter.errorhandling.adapter.out.metrics;

import com.smbtech.serviceframework.error.ResolvedError;
import com.smbtech.serviceframework.error.metadata.StandardErrorMetadataKeys;
import com.smbtech.serviceframework.starter.errorhandling.api.ErrorMetricsRecorder;
import com.smbtech.serviceframework.starter.errorhandling.api.security.SecurityFailureReason;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** Records resolved error counters with stable, bounded tag dimensions. */
public final class MicrometerErrorMetricsRecorder implements ErrorMetricsRecorder {

    /** Counter used when no application-specific metric name is configured. */
    public static final String DEFAULT_METRIC_NAME = "smbtech.error.handling.errors";

    /** Tag value used for errors outside Spring Security. */
    public static final String NO_SECURITY_REASON = "none";

    /** Bounded fallback for customized unsupported security reasons. */
    public static final String OTHER_SECURITY_REASON = "other";

    private static final Set<String> SECURITY_REASONS =
            Arrays.stream(SecurityFailureReason.values())
                    .map(SecurityFailureReason::metadataValue)
                    .collect(Collectors.toUnmodifiableSet());

    private final MeterRegistry meterRegistry;
    private final String metricName;

    /**
     * Creates a recorder using the default metric name.
     *
     * @param meterRegistry Micrometer registry
     */
    public MicrometerErrorMetricsRecorder(MeterRegistry meterRegistry) {
        this(meterRegistry, DEFAULT_METRIC_NAME);
    }

    /**
     * Creates a recorder using a custom metric name.
     *
     * @param meterRegistry Micrometer registry
     * @param metricName error counter name
     */
    public MicrometerErrorMetricsRecorder(MeterRegistry meterRegistry, String metricName) {
        this.meterRegistry =
                Objects.requireNonNull(meterRegistry, "meterRegistry must not be null");
        if (metricName == null || metricName.isBlank()) {
            throw new IllegalArgumentException("metricName must not be blank");
        }
        this.metricName = metricName.trim();
    }

    @Override
    public void record(ResolvedError resolvedError, int statusCode) {
        ResolvedError error =
                Objects.requireNonNull(resolvedError, "resolvedError must not be null");
        if (statusCode < 100 || statusCode > 599) {
            throw new IllegalArgumentException("statusCode must be between 100 and 599");
        }

        Counter.builder(metricName)
                .description("Resolved HTTP errors")
                .tag("code", error.notification().code())
                .tag("category", error.category().name())
                .tag("status", Integer.toString(statusCode))
                .tag("security_reason", securityReason(error))
                .register(meterRegistry)
                .increment();
    }

    private static String securityReason(ResolvedError error) {
        Object security = error.notification().metadata().get(StandardErrorMetadataKeys.SECURITY);
        if (!(security instanceof Map<?, ?> values)) {
            return NO_SECURITY_REASON;
        }
        Object reason = values.get(StandardErrorMetadataKeys.Security.REASON);
        if (!(reason instanceof String value)) {
            return OTHER_SECURITY_REASON;
        }
        return SECURITY_REASONS.contains(value) ? value : OTHER_SECURITY_REASON;
    }

    /**
     * Returns the configured metric name.
     *
     * @return metric name
     */
    public String metricName() {
        return metricName;
    }
}
