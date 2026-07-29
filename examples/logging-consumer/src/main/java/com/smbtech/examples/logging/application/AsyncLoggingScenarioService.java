package com.smbtech.examples.logging.application;

import static java.lang.invoke.MethodHandles.lookup;

import com.smbtech.serviceframework.logging.domain.EventType;
import com.smbtech.serviceframework.logging.domain.StructuredEvent;
import com.smbtech.serviceframework.logging.port.in.StructuredLogger;
import com.smbtech.serviceframework.starter.logging.StructuredLoggers;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AsyncLoggingScenarioService {
    private static final StructuredLogger LOG = StructuredLoggers.get(lookup());

    public EmissionResult emit(int eventCount, int criticalEvery) {
        String batchId = UUID.randomUUID().toString();
        int criticalEvents = 0;

        for (int sequence = 1; sequence <= eventCount; sequence++) {
            boolean critical = criticalEvery > 0 && sequence % criticalEvery == 0;
            EventType eventType = critical ? EventType.AUDIT : EventType.APPLICATION;
            LOG.info(
                    StructuredEvent.builder(eventType)
                            .message("Async logging example event {}/{}", sequence, eventCount)
                            .with("batchId", batchId)
                            .with("sequence", sequence)
                            .with("critical", critical)
                            .tag("ASYNC_EXAMPLE")
                            .build());
            if (critical) {
                criticalEvents++;
            }
        }

        return new EmissionResult(batchId, eventCount, eventCount - criticalEvents, criticalEvents);
    }

    public record EmissionResult(
            String batchId, int attemptedEvents, int applicationEvents, int criticalEvents) {}
}
