package com.smbtech.serviceframework.logging.application;

import com.smbtech.serviceframework.logging.domain.EventType;
import com.smbtech.serviceframework.logging.domain.LogLevel;
import com.smbtech.serviceframework.logging.domain.StructuredEvent;
import com.smbtech.serviceframework.logging.port.out.LogEventSink;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StructuredLoggingServiceTest {

    @Test
    void delegatesEnabledEventToOutputPort() {
        RecordingSink sink = new RecordingSink();
        StructuredLoggingService service = new StructuredLoggingService(sink, false);
        StructuredEvent event = StructuredEvent.builder(EventType.AUDIT)
                .message("Project updated")
                .build();

        service.info(event);

        assertEquals(List.of(new RecordedEvent(LogLevel.INFO, event)), sink.events);
    }

    @Test
    void suppressesMetricsInProduction() {
        RecordingSink sink = new RecordingSink();
        StructuredLoggingService service = new StructuredLoggingService(sink, true);
        StructuredEvent metric = StructuredEvent.builder(EventType.METRIC)
                .message("Projects counted")
                .build();

        service.info(metric);

        assertFalse(service.isEnabled(LogLevel.INFO, EventType.METRIC));
        assertTrue(sink.events.isEmpty());
    }

    private static final class RecordingSink implements LogEventSink {
        private final List<RecordedEvent> events = new ArrayList<>();

        @Override
        public boolean isEnabled(LogLevel level, EventType eventType) {
            return true;
        }

        @Override
        public void write(LogLevel level, StructuredEvent event) {
            events.add(new RecordedEvent(level, event));
        }
    }

    private record RecordedEvent(LogLevel level, StructuredEvent event) {
    }
}
