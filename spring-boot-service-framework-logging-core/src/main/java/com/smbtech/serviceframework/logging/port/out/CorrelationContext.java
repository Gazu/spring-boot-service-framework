package com.smbtech.serviceframework.logging.port.out;

import java.util.Map;
import java.util.Optional;

/** Outbound port for transport-independent correlation values. */
public interface CorrelationContext {

    /**
     * Captures the current correlation values.
     *
     * @return immutable or isolated correlation snapshot
     */
    Map<String, String> snapshot();

    /**
     * Replaces or overlays correlation values for a bounded scope.
     *
     * @param values correlation values to install
     * @return scope that restores the previous values when closed
     */
    Scope open(Map<String, String> values);

    /**
     * Finds a correlation value in the current snapshot.
     *
     * @param key correlation key
     * @return matching value when present
     */
    default Optional<String> find(String key) {
        return Optional.ofNullable(snapshot().get(key));
    }

    /** Restores the previous correlation context when closed. */
    @FunctionalInterface
    interface Scope extends AutoCloseable {
        @Override
        void close();
    }
}
