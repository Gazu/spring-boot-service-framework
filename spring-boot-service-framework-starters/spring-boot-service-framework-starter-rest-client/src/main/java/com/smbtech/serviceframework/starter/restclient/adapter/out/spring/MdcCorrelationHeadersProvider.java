package com.smbtech.serviceframework.starter.restclient.adapter.out.spring;

import com.smbtech.serviceframework.httpclient.port.out.CorrelationHeadersProvider;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.MDC;

/** Provides mdc correlation headers provider behavior. */
public final class MdcCorrelationHeadersProvider implements CorrelationHeadersProvider {
    /** Creates a mdc correlation headers provider instance. */
    public MdcCorrelationHeadersProvider() {}

    private static final String TRACE_ID = "traceId";
    private static final String SPAN_ID = "spanId";
    private static final String TRANSACTION_ID = "transactionId";

    @Override
    public Map<String, String> currentHeaders() {
        Map<String, String> headers = new LinkedHashMap<>();
        putIfPresent(headers, "X-B3-TraceId", MDC.get(TRACE_ID));
        putIfPresent(headers, "X-B3-SpanId", MDC.get(SPAN_ID));
        putIfPresent(headers, "X-B3-Sampled", "1");
        putIfPresent(headers, "X-Transaction-Id", MDC.get(TRANSACTION_ID));
        return headers;
    }

    private void putIfPresent(Map<String, String> headers, String name, String value) {
        if (value != null && !value.isBlank()) {
            headers.put(name, value);
        }
    }
}
