package com.smbtech.serviceframework.starter.logging.adapter.out.context;

import com.smbtech.serviceframework.logging.port.out.CorrelationContext;
import org.slf4j.MDC;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * SLF4J MDC adapter that restores the previous thread context after each scope.
 */
public final class MdcCorrelationContext implements CorrelationContext {

    @Override
    public Map<String, String> snapshot() {
        Map<String, String> values = MDC.getCopyOfContextMap();
        return values == null ? Map.of() : Map.copyOf(values);
    }

    @Override
    public Scope open(Map<String, String> values) {
        Map<String, String> previous = MDC.getCopyOfContextMap();
        Map<String, String> merged = new LinkedHashMap<>();
        if (previous != null) {
            merged.putAll(previous);
        }
        if (values != null) {
            merged.putAll(values);
        }
        MDC.setContextMap(merged);

        return () -> {
            if (previous == null || previous.isEmpty()) {
                MDC.clear();
            } else {
                MDC.setContextMap(previous);
            }
        };
    }
}
