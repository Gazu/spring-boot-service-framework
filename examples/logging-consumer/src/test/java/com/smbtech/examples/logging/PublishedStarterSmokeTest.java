package com.smbtech.examples.logging;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.smbtech.examples.logging.application.ProjectApplicationService;
import com.smbtech.serviceframework.logging.domain.EventType;
import com.smbtech.serviceframework.logging.domain.StructuredEvent;
import com.smbtech.serviceframework.logging.port.in.StructuredLoggerFactory;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class PublishedStarterSmokeTest {

    @Autowired private ProjectApplicationService service;

    @Autowired private StructuredLoggerFactory loggerFactory;

    @Test
    void loadsStarterTransitivelyAndEmitsCoreEvent() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        ch.qos.logback.classic.Logger logger = context.getLogger(ProjectApplicationService.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            service.update(42L);

            assertThat(loggerFactory).isNotNull();
            ILoggingEvent event =
                    appender.list.stream()
                            .filter(
                                    current ->
                                            current.getFormattedMessage()
                                                    .equals("Project 42 updated"))
                            .findFirst()
                            .orElseThrow();
            assertThat(event.getFormattedMessage()).isEqualTo("Project 42 updated");
            assertThat(event.getArgumentArray())
                    .anySatisfy(
                            argument -> {
                                assertThat(argument).isInstanceOf(StructuredEvent.class);
                                StructuredEvent structured = (StructuredEvent) argument;
                                assertThat(structured.type()).isEqualTo(EventType.AUDIT);
                                assertThat(structured.data()).containsEntry("projectId", 42L);
                            });
        } finally {
            logger.detachAppender(appender);
        }
    }
}
