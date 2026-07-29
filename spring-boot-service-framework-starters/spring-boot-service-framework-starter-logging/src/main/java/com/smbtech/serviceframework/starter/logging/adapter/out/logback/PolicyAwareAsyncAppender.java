package com.smbtech.serviceframework.starter.logging.adapter.out.logback;

import ch.qos.logback.classic.AsyncAppender;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import com.smbtech.serviceframework.logging.domain.EventType;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

/**
 * Logback async appender that maps the framework saturation policy to native queue settings.
 *
 * <p>This type is public only because Logback creates it reflectively from {@code
 * logback-spring.xml}. Applications should configure it through {@code smbtech.logging.async.*}.
 */
public final class PolicyAwareAsyncAppender extends AsyncAppender {
    private static final String APPENDER_NAME = "ASYNC";

    private SaturationPolicy saturationPolicy = SaturationPolicy.BLOCK;
    private final Lock dropPolicyLock = new ReentrantLock();
    private final ReentrantReadWriteLock lifecycleLock = new ReentrantReadWriteLock(true);
    private final Lock appendLock = lifecycleLock.readLock();
    private final Lock shutdownLock = lifecycleLock.writeLock();
    private final AtomicBoolean acceptingEvents = new AtomicBoolean();
    private final LongAdder discardedLowPriorityEvents = new LongAdder();
    private final LongAdder discardedFullQueueEvents = new LongAdder();
    private final LongAdder criticalFallbackEvents = new LongAdder();
    private final LongAdder blockedProducerEvents = new LongAdder();
    private final LongAdder blockedProducerDurationNanos = new LongAdder();
    private final LongAdder rejectedDuringShutdownEvents = new LongAdder();
    private final LongAdder shutdownCount = new LongAdder();
    private final LongAdder shutdownDurationNanos = new LongAdder();
    private final LongAdder shutdownTimeoutCount = new LongAdder();
    private int configuredDiscardingThreshold;
    private boolean legacyNeverBlock;
    private boolean criticalEventProtectionEnabled = true;
    private volatile SaturationPolicy effectiveSaturationPolicy = SaturationPolicy.BLOCK;
    private volatile Appender<ILoggingEvent> delegateAppender;
    private volatile int lastShutdownPendingEventCount;
    private volatile boolean lastShutdownTimedOut;

    /** Creates a policy-aware async appender. */
    public PolicyAwareAsyncAppender() {}

    /**
     * Sets the configured framework saturation policy.
     *
     * @param saturationPolicy configured policy name
     */
    public void setSaturationPolicy(String saturationPolicy) {
        this.saturationPolicy =
                SaturationPolicy.valueOf(saturationPolicy.strip().toUpperCase(Locale.ROOT));
    }

    /**
     * Sets the optional low-priority discarding threshold.
     *
     * @param configuredDiscardingThreshold configured remaining queue capacity
     */
    public void setConfiguredDiscardingThreshold(int configuredDiscardingThreshold) {
        this.configuredDiscardingThreshold = configuredDiscardingThreshold;
    }

    /**
     * Sets the compatibility override for the former never-block property.
     *
     * @param legacyNeverBlock whether the compatibility override is enabled
     */
    public void setLegacyNeverBlock(boolean legacyNeverBlock) {
        this.legacyNeverBlock = legacyNeverBlock;
    }

    /**
     * Sets whether critical events receive saturation protection.
     *
     * @param criticalEventProtectionEnabled whether critical event protection is enabled
     */
    public void setCriticalEventProtectionEnabled(boolean criticalEventProtectionEnabled) {
        this.criticalEventProtectionEnabled = criticalEventProtectionEnabled;
    }

    /**
     * Finds the active framework async appender attached to the root logger.
     *
     * @return the active appender, or empty when async routing is disabled or Logback is not active
     */
    public static Optional<PolicyAwareAsyncAppender> findActive() {
        ILoggerFactory loggerFactory = LoggerFactory.getILoggerFactory();
        if (!(loggerFactory instanceof ch.qos.logback.classic.LoggerContext loggerContext)) {
            return Optional.empty();
        }
        Appender<ILoggingEvent> appender =
                loggerContext
                        .getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)
                        .getAppender(APPENDER_NAME);
        return appender instanceof PolicyAwareAsyncAppender policyAware
                ? Optional.of(policyAware)
                : Optional.empty();
    }

    /**
     * Returns the number of low-priority events discarded near queue capacity.
     *
     * @return discarded low-priority event count
     */
    public long getDiscardedLowPriorityEventCount() {
        return discardedLowPriorityEvents.sum();
    }

    /**
     * Returns the number of events discarded because a non-blocking queue was full.
     *
     * @return full-queue discard count
     */
    public long getDiscardedFullQueueEventCount() {
        return discardedFullQueueEvents.sum();
    }

    /**
     * Returns the number of critical events delivered through synchronous fallback.
     *
     * @return critical synchronous fallback count
     */
    public long getCriticalFallbackEventCount() {
        return criticalFallbackEvents.sum();
    }

    /**
     * Returns the number of producer calls observed waiting for full-queue capacity.
     *
     * @return blocked producer count
     */
    public long getBlockedProducerEventCount() {
        return blockedProducerEvents.sum();
    }

    /**
     * Returns the cumulative observed producer wait time in nanoseconds.
     *
     * @return blocked producer duration in nanoseconds
     */
    public long getBlockedProducerDurationNanos() {
        return blockedProducerDurationNanos.sum();
    }

    /**
     * Reports whether the appender currently accepts events.
     *
     * @return whether event admission is open
     */
    public boolean isAcceptingEvents() {
        return acceptingEvents.get();
    }

    /**
     * Returns the number of events rejected after shutdown admission closed.
     *
     * @return shutdown rejection count
     */
    public long getRejectedDuringShutdownEventCount() {
        return rejectedDuringShutdownEvents.sum();
    }

    /**
     * Returns the number of completed shutdown attempts.
     *
     * @return shutdown count
     */
    public long getShutdownCount() {
        return shutdownCount.sum();
    }

    /**
     * Returns the cumulative shutdown duration in nanoseconds.
     *
     * @return shutdown duration in nanoseconds
     */
    public long getShutdownDurationNanos() {
        return shutdownDurationNanos.sum();
    }

    /**
     * Returns the number of shutdown attempts that exceeded the flush timeout.
     *
     * @return shutdown timeout count
     */
    public long getShutdownTimeoutCount() {
        return shutdownTimeoutCount.sum();
    }

    /**
     * Returns the number of queued events remaining when the last shutdown attempt returned.
     *
     * @return pending event count after the last shutdown attempt
     */
    public int getLastShutdownPendingEventCount() {
        return lastShutdownPendingEventCount;
    }

    /**
     * Reports whether the last shutdown attempt exceeded the configured flush timeout.
     *
     * @return whether the last shutdown timed out
     */
    public boolean isLastShutdownTimedOut() {
        return lastShutdownTimedOut;
    }

    @Override
    public void addAppender(Appender<ILoggingEvent> newAppender) {
        super.addAppender(newAppender);
        if (delegateAppender == null) {
            delegateAppender = newAppender;
        }
    }

    @Override
    public void start() {
        shutdownLock.lock();
        try {
            if (isStarted()) {
                return;
            }
            EffectiveSaturation effective =
                    resolve(
                            saturationPolicy,
                            getQueueSize(),
                            configuredDiscardingThreshold,
                            legacyNeverBlock);
            effectiveSaturationPolicy = effective.policy();
            setDiscardingThreshold(effective.discardingThreshold());
            setNeverBlock(effective.neverBlock());
            acceptingEvents.set(true);
            super.start();
            if (!isStarted()) {
                acceptingEvents.set(false);
            }
        } catch (RuntimeException | Error failure) {
            acceptingEvents.set(false);
            throw failure;
        } finally {
            shutdownLock.unlock();
        }
    }

    @Override
    public void stop() {
        if (!isStarted()) {
            return;
        }

        long startedAt = System.nanoTime();
        acceptingEvents.set(false);
        shutdownLock.lock();
        try {
            if (!isStarted()) {
                return;
            }
            try {
                super.stop();
            } finally {
                shutdownCount.increment();
                shutdownDurationNanos.add(System.nanoTime() - startedAt);
                lastShutdownPendingEventCount = getNumberOfElementsInQueue();
                lastShutdownTimedOut = delegateAppender != null && delegateAppender.isStarted();
                if (lastShutdownTimedOut) {
                    shutdownTimeoutCount.increment();
                }
            }
        } finally {
            shutdownLock.unlock();
        }
    }

    @Override
    protected boolean isDiscardable(ILoggingEvent event) {
        boolean discardable = !isCritical(event) && super.isDiscardable(event);
        if (discardable) {
            discardedLowPriorityEvents.increment();
        }
        return discardable;
    }

    @Override
    protected void append(ILoggingEvent event) {
        appendLock.lock();
        try {
            if (!acceptingEvents.get() || !isStarted()) {
                rejectedDuringShutdownEvents.increment();
                return;
            }
            appendWhileAccepting(event);
        } finally {
            appendLock.unlock();
        }
    }

    private void appendWhileAccepting(ILoggingEvent event) {
        if (effectiveSaturationPolicy != SaturationPolicy.DROP_WHEN_FULL) {
            appendWithBlockingObservation(event);
            return;
        }
        dropPolicyLock.lock();
        try {
            if (getRemainingCapacity() == 0) {
                if (isCritical(event) && delegateAppender != null) {
                    criticalFallbackEvents.increment();
                    preprocess(event);
                    delegateAppender.doAppend(event);
                } else {
                    discardedFullQueueEvents.increment();
                }
                return;
            }
            super.append(event);
        } finally {
            dropPolicyLock.unlock();
        }
    }

    private void appendWithBlockingObservation(ILoggingEvent event) {
        boolean observedFullQueue =
                getRemainingCapacity() == 0
                        && !(isQueueBelowDiscardingThreshold()
                                && !isCritical(event)
                                && super.isDiscardable(event));
        if (!observedFullQueue) {
            super.append(event);
            return;
        }

        long startedAt = System.nanoTime();
        super.append(event);
        blockedProducerEvents.increment();
        blockedProducerDurationNanos.add(System.nanoTime() - startedAt);
    }

    private boolean isCritical(ILoggingEvent event) {
        if (!criticalEventProtectionEnabled) {
            return false;
        }
        if (event.getLevel() != null && event.getLevel().isGreaterOrEqual(Level.WARN)) {
            return true;
        }
        if (StructuredEventExtractor.from(event.getArgumentArray())
                .map(structured -> isCritical(structured.type()))
                .orElse(false)) {
            return true;
        }
        return event.getMarkerList() != null
                && event.getMarkerList().stream().anyMatch(this::isCritical);
    }

    private boolean isCritical(Marker marker) {
        return marker.contains(EventType.AUDIT.value())
                || marker.contains(EventType.SECURITY.value());
    }

    private boolean isCritical(EventType eventType) {
        return EventType.AUDIT.equals(eventType) || EventType.SECURITY.equals(eventType);
    }

    private static EffectiveSaturation resolve(
            SaturationPolicy configuredPolicy,
            int queueSize,
            int configuredDiscardingThreshold,
            boolean legacyNeverBlock) {
        SaturationPolicy effectivePolicy = configuredPolicy;
        if (legacyNeverBlock) {
            effectivePolicy = SaturationPolicy.DROP_WHEN_FULL;
        } else if (configuredPolicy == SaturationPolicy.BLOCK
                && configuredDiscardingThreshold > 0) {
            effectivePolicy = SaturationPolicy.DISCARD_LOW_PRIORITY;
        }

        return switch (effectivePolicy) {
            case BLOCK -> new EffectiveSaturation(effectivePolicy, false, 0);
            case DISCARD_LOW_PRIORITY ->
                    new EffectiveSaturation(
                            effectivePolicy,
                            false,
                            configuredDiscardingThreshold > 0
                                    ? configuredDiscardingThreshold
                                    : Math.max(1, queueSize / 5));
            case DROP_WHEN_FULL -> new EffectiveSaturation(effectivePolicy, true, 0);
        };
    }

    private record EffectiveSaturation(
            SaturationPolicy policy, boolean neverBlock, int discardingThreshold) {}

    private enum SaturationPolicy {
        BLOCK,
        DISCARD_LOW_PRIORITY,
        DROP_WHEN_FULL
    }
}
