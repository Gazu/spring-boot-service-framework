package com.smbtech.serviceframework.starter.logging.autoconfigure;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class AsyncLoggingPropertiesValidatorTest {

    @Test
    void acceptsBoundaryValues() {
        LoggingProperties minimums = properties(256, 0, 100);
        LoggingProperties maximums = properties(65_536, 65_535, 30_000);

        assertThatCode(() -> AsyncLoggingPropertiesValidator.validate(minimums))
                .doesNotThrowAnyException();
        assertThatCode(() -> AsyncLoggingPropertiesValidator.validate(maximums))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsQueueSizeOutsideSupportedRange() {
        assertInvalid(
                properties(255, 0, 100),
                "smbtech.logging.async.queue-size must be between 256 and 65536 "
                        + "(inclusive) (was 255)");
        assertInvalid(
                properties(65_537, 0, 100),
                "smbtech.logging.async.queue-size must be between 256 and 65536 "
                        + "(inclusive) (was 65537)");
    }

    @Test
    void rejectsDiscardingThresholdOutsideQueueCapacity() {
        assertInvalid(
                properties(256, -1, 100),
                "smbtech.logging.async.discarding-threshold must be between 0 (inclusive) "
                        + "and smbtech.logging.async.queue-size (exclusive) (was -1)");
        assertInvalid(
                properties(256, 256, 100),
                "smbtech.logging.async.discarding-threshold must be between 0 (inclusive) "
                        + "and smbtech.logging.async.queue-size (exclusive) (was 256)");
    }

    @Test
    void rejectsFlushTimeOutsideSupportedRange() {
        assertInvalid(
                properties(256, 0, 99),
                "smbtech.logging.async.max-flush-time-ms must be between 100 and 30000 "
                        + "(inclusive) (was 99)");
        assertInvalid(
                properties(256, 0, 30_001),
                "smbtech.logging.async.max-flush-time-ms must be between 100 and 30000 "
                        + "(inclusive) (was 30001)");
    }

    @Test
    void rejectsNullSaturationPolicy() {
        LoggingProperties properties = properties(256, 0, 100);
        properties.getAsync().setSaturationPolicy(null);

        assertThatThrownBy(() -> AsyncLoggingPropertiesValidator.validate(properties))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(
                        "Invalid async logging configuration: "
                                + "smbtech.logging.async.saturation-policy must not be null");
    }

    private static LoggingProperties properties(
            int queueSize, int discardingThreshold, int maxFlushTimeMs) {
        LoggingProperties properties = new LoggingProperties();
        properties.getAsync().setQueueSize(queueSize);
        properties.getAsync().setDiscardingThreshold(discardingThreshold);
        properties.getAsync().setMaxFlushTimeMs(maxFlushTimeMs);
        return properties;
    }

    private static void assertInvalid(LoggingProperties properties, String message) {
        assertThatThrownBy(() -> AsyncLoggingPropertiesValidator.validate(properties))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Invalid async logging configuration: " + message);
    }
}
