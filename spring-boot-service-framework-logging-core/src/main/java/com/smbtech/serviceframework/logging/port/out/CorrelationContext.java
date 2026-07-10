package com.smbtech.serviceframework.logging.port.out;

import java.util.Map;
import java.util.Optional;

/**
 * Outbound port for transport-independent correlation values.
 */
public interface CorrelationContext {

    Map<String, String> snapshot();

    Scope open(Map<String, String> values);

    default Optional<String> find(String key) {
        return Optional.ofNullable(snapshot().get(key));
    }

    /**
     * Restores the previous correlation context when closed.
     */
    @FunctionalInterface
    interface Scope extends AutoCloseable {
        @Override
        void close();
    }
}
