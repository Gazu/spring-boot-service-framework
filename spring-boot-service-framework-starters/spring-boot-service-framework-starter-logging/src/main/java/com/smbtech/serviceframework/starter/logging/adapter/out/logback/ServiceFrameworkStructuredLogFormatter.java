package com.smbtech.serviceframework.starter.logging.adapter.out.logback;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import com.smbtech.serviceframework.logging.domain.EventType;
import com.smbtech.serviceframework.logging.domain.StructuredEvent;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Marker;
import org.springframework.boot.logging.structured.StructuredLogFormatter;
import tools.jackson.core.StreamWriteFeature;
import tools.jackson.core.json.JsonWriteFeature;
import tools.jackson.databind.ObjectWriter;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/** Spring Boot structured logging formatter that preserves the service framework JSON contract. */
public final class ServiceFrameworkStructuredLogFormatter
        implements StructuredLogFormatter<ILoggingEvent> {
    private static final String TRANSACTION_ID = "transactionId";
    private static final String SENSITIVE_MARKER = "SENSITIVE";

    private final ObjectWriter writer;

    /** Creates a service framework structured log formatter instance. */
    public ServiceFrameworkStructuredLogFormatter() {
        this.writer =
                JsonMapper.builder()
                        .enable(JsonWriteFeature.ESCAPE_NON_ASCII)
                        .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                        .disable(StreamWriteFeature.FLUSH_PASSED_TO_STREAM)
                        .build()
                        .writer();
    }

    @Override
    public String format(ILoggingEvent event) {
        try {
            return writer.writeValueAsString(payload(event)) + System.lineSeparator();
        } catch (Exception exception) {
            return fallback(exception);
        }
    }

    private Map<String, Object> payload(ILoggingEvent event) {
        Optional<StructuredEvent> structured =
                StructuredEventExtractor.from(event.getArgumentArray());
        Map<String, String> mdc = Optional.ofNullable(event.getMDCPropertyMap()).orElse(Map.of());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("ts", event.getInstant());
        payload.put("uuid", mdc.getOrDefault(TRANSACTION_ID, UUID.randomUUID().toString()));
        payload.put("type", eventType(event, structured));
        payload.put("msg", event.getFormattedMessage());
        payload.put("class", event.getLoggerName());
        payload.put("pii", isSensitive(event, structured));
        payload.put("thread", event.getThreadName());
        payload.put("mdc", mdc);
        payload.put("data", structured.map(StructuredEvent::data).orElse(Map.of()));
        payload.put("tags", tags(event, structured));
        payload.put("exception", ExceptionDetails.from(throwable(event, structured)));
        return payload;
    }

    private String eventType(ILoggingEvent event, Optional<StructuredEvent> structured) {
        return structured
                .filter(value -> !EventType.APPLICATION.equals(value.type()))
                .map(value -> value.type().value())
                .orElse(event.getLevel().levelStr);
    }

    private boolean isSensitive(ILoggingEvent event, Optional<StructuredEvent> structured) {
        return structured
                .map(StructuredEvent::isSensitive)
                .orElseGet(
                        () ->
                                firstMarker(event)
                                        .map(marker -> marker.contains(SENSITIVE_MARKER))
                                        .orElse(false));
    }

    private List<String> tags(ILoggingEvent event, Optional<StructuredEvent> structured) {
        if (structured.isPresent()) {
            return List.copyOf(structured.orElseThrow().tags());
        }
        List<String> tags = new ArrayList<>();
        firstMarker(event)
                .ifPresent(
                        marker ->
                                marker.iterator()
                                        .forEachRemaining(
                                                child -> {
                                                    if (!SENSITIVE_MARKER.equals(child.getName())) {
                                                        tags.add(child.getName());
                                                    }
                                                }));
        return tags;
    }

    private Throwable throwable(ILoggingEvent event, Optional<StructuredEvent> structured) {
        if (event.getThrowableProxy() instanceof ThrowableProxy proxy) {
            return proxy.getThrowable();
        }
        return structured.flatMap(StructuredEvent::failure).orElse(null);
    }

    private Optional<Marker> firstMarker(ILoggingEvent event) {
        List<Marker> markers = event.getMarkerList();
        return markers == null || markers.isEmpty()
                ? Optional.empty()
                : Optional.of(markers.getFirst());
    }

    private String fallback(Exception exception) {
        try {
            Map<String, Object> fallback = new LinkedHashMap<>();
            fallback.put("ts", Instant.now());
            fallback.put("uuid", UUID.randomUUID().toString());
            fallback.put("type", "ERROR");
            fallback.put("msg", "Failed to format structured log");
            fallback.put("class", getClass().getName());
            fallback.put("pii", false);
            fallback.put("thread", Thread.currentThread().getName());
            fallback.put("mdc", Map.of());
            fallback.put("data", Map.of());
            fallback.put("tags", List.of());
            fallback.put("exception", ExceptionDetails.from(exception));
            return writer.writeValueAsString(fallback) + System.lineSeparator();
        } catch (Exception ignored) {
            return "{\"type\":\"ERROR\",\"msg\":\"Failed to format structured log\"}\n";
        }
    }
}
