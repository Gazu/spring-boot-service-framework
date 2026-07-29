package com.smbtech.serviceframework.starter.actuator.adapter.out.integration;

import com.smbtech.serviceframework.actuator.domain.FrameworkModuleInfo;
import com.smbtech.serviceframework.actuator.port.out.FrameworkModuleInfoProvider;
import com.smbtech.serviceframework.starter.logging.adapter.out.logback.PolicyAwareAsyncAppender;
import com.smbtech.serviceframework.starter.logging.autoconfigure.LoggingProperties;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Provides bounded application information for the logging starter. */
public final class LoggingModuleInfoProvider implements FrameworkModuleInfoProvider {

    /** Stable framework module name. */
    public static final String MODULE_NAME = "logging";

    private final LoggingProperties properties;

    /**
     * Creates a logging module information provider.
     *
     * @param properties logging configuration
     */
    public LoggingModuleInfoProvider(LoggingProperties properties) {
        this.properties = Objects.requireNonNull(properties, "properties");
    }

    @Override
    public String moduleName() {
        return MODULE_NAME;
    }

    @Override
    public FrameworkModuleInfo provide() {
        LoggingProperties.Async async = properties.getAsync();
        LoggingProperties.Transaction transaction = properties.getTransaction();
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("productionMode", properties.isProduction());
        attributes.put("level", Objects.requireNonNullElse(properties.getLevel(), "UNKNOWN"));
        attributes.put("asyncEnabled", async != null && async.isEnabled());
        attributes.put(
                "asyncSaturationPolicy",
                async == null || async.getSaturationPolicy() == null
                        ? "UNKNOWN"
                        : async.getSaturationPolicy().name());
        attributes.put(
                "asyncCriticalEventProtectionEnabled",
                async != null && async.isCriticalEventProtectionEnabled());
        attributes.put(
                "asyncObservabilityEnabled",
                async != null
                        && async.getObservability() != null
                        && async.getObservability().isEnabled());
        attributes.put(
                "transactionCorrelationEnabled", transaction != null && transaction.isEnabled());
        PolicyAwareAsyncAppender.findActive()
                .ifPresent(appender -> addRuntimeAttributes(attributes, appender));
        return new FrameworkModuleInfo(
                MODULE_NAME, ModuleVersions.resolve(LoggingProperties.class), attributes);
    }

    private static void addRuntimeAttributes(
            Map<String, Object> attributes, PolicyAwareAsyncAppender appender) {
        attributes.put("asyncQueueCapacity", appender.getQueueSize());
        attributes.put("asyncQueueDepth", appender.getNumberOfElementsInQueue());
        attributes.put("asyncQueueRemainingCapacity", appender.getRemainingCapacity());
        attributes.put(
                "asyncDiscardedLowPriorityEvents", appender.getDiscardedLowPriorityEventCount());
        attributes.put("asyncDiscardedFullQueueEvents", appender.getDiscardedFullQueueEventCount());
        attributes.put("asyncCriticalFallbackEvents", appender.getCriticalFallbackEventCount());
        attributes.put("asyncBlockedProducerEvents", appender.getBlockedProducerEventCount());
        attributes.put(
                "asyncBlockedProducerDurationNanos", appender.getBlockedProducerDurationNanos());
        attributes.put("asyncAcceptingEvents", appender.isAcceptingEvents());
        attributes.put(
                "asyncRejectedDuringShutdownEvents",
                appender.getRejectedDuringShutdownEventCount());
        attributes.put("asyncShutdownCount", appender.getShutdownCount());
        attributes.put("asyncShutdownTimeoutCount", appender.getShutdownTimeoutCount());
        attributes.put(
                "asyncLastShutdownPendingEvents", appender.getLastShutdownPendingEventCount());
        attributes.put("asyncLastShutdownTimedOut", appender.isLastShutdownTimedOut());
    }
}
