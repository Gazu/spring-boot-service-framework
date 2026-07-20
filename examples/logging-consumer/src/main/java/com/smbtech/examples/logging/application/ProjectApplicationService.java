package com.smbtech.examples.logging.application;

import static java.lang.invoke.MethodHandles.lookup;

import com.smbtech.serviceframework.logging.domain.EventType;
import com.smbtech.serviceframework.logging.domain.StructuredEvent;
import com.smbtech.serviceframework.logging.port.in.StructuredLogger;
import com.smbtech.serviceframework.starter.logging.StructuredLoggers;
import org.springframework.stereotype.Service;

@Service
public class ProjectApplicationService {
    private static final StructuredLogger LOG = StructuredLoggers.get(lookup());

    public void update(long projectId) {
        LOG.info(
                StructuredEvent.builder(EventType.AUDIT)
                        .message("Project {} updated", projectId)
                        .with("projectId", projectId)
                        .tag("PROJECT")
                        .build());

        LOG.info(
                e -> {
                    e.message("Project {} updated. In Consumer", projectId);
                    e.with("projectId", projectId);
                    e.tag("PROJECT");
                    e.tag("CONSUMER");
                });
    }

    public void logDummyRequest(String transactionId, String traceId, String spanId) {
        LOG.info(
                StructuredEvent.builder(EventType.APPLICATION)
                        .message("Dummy endpoint invoked")
                        .with("transactionId", transactionId)
                        .with("traceId", traceId)
                        .with("spanId", spanId)
                        .tag("DUMMY")
                        .build());

        LOG.info(
                e -> {
                    e.message("Dummy endpoint invoked. {}", "In Consumer");
                    e.tag("CONSUMER");
                });
    }
}
