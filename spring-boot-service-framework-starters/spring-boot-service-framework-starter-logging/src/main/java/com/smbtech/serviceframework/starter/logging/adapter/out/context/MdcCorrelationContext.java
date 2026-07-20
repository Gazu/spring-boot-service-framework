package com.smbtech.serviceframework.starter.logging.adapter.out.context;

import com.smbtech.serviceframework.logging.port.out.CorrelationContext;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.MDC;

/** SLF4J MDC adapter that restores the previous thread context after each scope. */
public final class MdcCorrelationContext implements CorrelationContext {
    /** Creates a mdc correlation context instance. */
    public MdcCorrelationContext() {}

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
