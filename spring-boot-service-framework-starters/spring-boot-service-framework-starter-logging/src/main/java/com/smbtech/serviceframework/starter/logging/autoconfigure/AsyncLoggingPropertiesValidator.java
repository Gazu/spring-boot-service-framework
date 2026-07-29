package com.smbtech.serviceframework.starter.logging.autoconfigure;

final class AsyncLoggingPropertiesValidator {
    static final int MIN_QUEUE_SIZE = 256;
    static final int MAX_QUEUE_SIZE = 65536;
    static final int MIN_MAX_FLUSH_TIME_MS = 100;
    static final int MAX_MAX_FLUSH_TIME_MS = 30000;

    private AsyncLoggingPropertiesValidator() {}

    static void validate(LoggingProperties properties) {
        LoggingProperties.Async async = properties.getAsync();
        if (async.getSaturationPolicy() == null) {
            throw new IllegalStateException(
                    "Invalid async logging configuration: "
                            + "smbtech.logging.async.saturation-policy must not be null");
        }
        requireRange(
                "smbtech.logging.async.queue-size",
                async.getQueueSize(),
                MIN_QUEUE_SIZE,
                MAX_QUEUE_SIZE);
        requireRange(
                "smbtech.logging.async.max-flush-time-ms",
                async.getMaxFlushTimeMs(),
                MIN_MAX_FLUSH_TIME_MS,
                MAX_MAX_FLUSH_TIME_MS);

        int discardingThreshold = async.getDiscardingThreshold();
        if (discardingThreshold < 0 || discardingThreshold >= async.getQueueSize()) {
            throw invalid(
                    "smbtech.logging.async.discarding-threshold",
                    "must be between 0 (inclusive) and smbtech.logging.async.queue-size "
                            + "(exclusive)",
                    discardingThreshold);
        }
    }

    private static void requireRange(String property, int value, int minimum, int maximum) {
        if (value < minimum || value > maximum) {
            throw invalid(
                    property,
                    "must be between " + minimum + " and " + maximum + " (inclusive)",
                    value);
        }
    }

    private static IllegalStateException invalid(String property, String requirement, int value) {
        return new IllegalStateException(
                "Invalid async logging configuration: "
                        + property
                        + " "
                        + requirement
                        + " (was "
                        + value
                        + ")");
    }
}
