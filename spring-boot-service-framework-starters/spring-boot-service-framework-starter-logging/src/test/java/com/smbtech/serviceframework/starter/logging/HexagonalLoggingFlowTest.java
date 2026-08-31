package com.smbtech.serviceframework.starter.logging;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.smbtech.serviceframework.logging.domain.EventType;
import com.smbtech.serviceframework.logging.domain.StructuredEvent;
import com.smbtech.serviceframework.logging.port.in.StructuredLogger;
import com.smbtech.serviceframework.starter.logging.adapter.out.logback.ServiceFrameworkStructuredLogFormatter;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

class HexagonalLoggingFlowTest {

    @Test
    void sendsCoreEventThroughSlf4jAdapterAndJsonEncoder() throws Exception {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        ch.qos.logback.classic.Logger delegate = context.getLogger(HexagonalLoggingFlowTest.class);
        delegate.setLevel(Level.INFO);
        delegate.setAdditive(false);

        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        delegate.addAppender(appender);

        try {
            StructuredLogger logging = StructuredLoggers.get(HexagonalLoggingFlowTest.class);
            StructuredEvent event =
                    StructuredEvent.builder(EventType.AUDIT)
                            .message("Project {} updated", 42)
                            .with("projectId", 42)
                            .tag("PROJECT")
                            .sensitive()
                            .build();

            logging.info(event);

            assertThat(appender.list).hasSize(1);
            Map<String, Object> json =
                    new ObjectMapper()
                            .readValue(
                                    new ServiceFrameworkStructuredLogFormatter()
                                            .format(appender.list.getFirst()),
                                    new TypeReference<>() {});
            assertThat(json)
                    .containsEntry("type", "AUDIT")
                    .containsEntry("msg", "Project 42 updated")
                    .containsEntry("pii", true);
            assertThat(json.get("data")).isEqualTo(Map.of("projectId", 42));
            assertThat(json.get("tags")).isEqualTo(java.util.List.of("PROJECT"));
        } finally {
            delegate.detachAppender(appender);
            delegate.setAdditive(true);
        }
    }
}
