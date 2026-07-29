package com.smbtech.examples.logging;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.smbtech.examples.logging.application.AsyncLoggingScenarioService;
import com.smbtech.serviceframework.logging.domain.EventType;
import com.smbtech.serviceframework.logging.domain.StructuredEvent;
import com.smbtech.serviceframework.starter.logging.adapter.out.logback.PolicyAwareAsyncAppender;
import io.micrometer.core.instrument.MeterRegistry;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AsyncLoggingExampleTest {
    private static final String TRANSACTION_ID = "tx-async-example";

    @LocalServerPort private int port;

    @Autowired private MeterRegistry meterRegistry;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @Timeout(15)
    void emitsMixedEventsThroughTheRealAsyncAppenderAndExposesMetrics() throws Exception {
        PolicyAwareAsyncAppender asyncAppender =
                PolicyAwareAsyncAppender.findActive().orElseThrow();
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        ch.qos.logback.classic.Logger scenarioLogger =
                loggerContext.getLogger(AsyncLoggingScenarioService.class);
        RecordingAppender recordingAppender = new RecordingAppender();
        recordingAppender.setContext(loggerContext);
        recordingAppender.start();
        PolicyAwareAsyncAppender verificationAppender = new PolicyAwareAsyncAppender();
        verificationAppender.setName("ASYNC_EXAMPLE_TEST");
        verificationAppender.setContext(loggerContext);
        verificationAppender.setQueueSize(256);
        verificationAppender.setSaturationPolicy("BLOCK");
        verificationAppender.setMaxFlushTime(1000);
        verificationAppender.addAppender(recordingAppender);
        verificationAppender.start();
        scenarioLogger.addAppender(verificationAppender);

        try (HttpClient client =
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()) {
            HttpResponse<String> emission =
                    client.send(
                            HttpRequest.newBuilder()
                                    .uri(
                                            URI.create(
                                                    "http://localhost:"
                                                            + port
                                                            + "/api/logging/async"
                                                            + "?events=12&critical-every=4"))
                                    .header("X-Transaction-Id", TRANSACTION_ID)
                                    .timeout(Duration.ofSeconds(5))
                                    .POST(HttpRequest.BodyPublishers.noBody())
                                    .build(),
                            HttpResponse.BodyHandlers.ofString());

            assertThat(emission.statusCode()).isEqualTo(200);
            JsonNode body = objectMapper.readTree(emission.body());
            assertThat(body.path("attemptedEvents").asInt()).isEqualTo(12);
            assertThat(body.path("applicationEvents").asInt()).isEqualTo(9);
            assertThat(body.path("criticalEvents").asInt()).isEqualTo(3);

            String batchId = body.path("batchId").asText();
            List<ILoggingEvent> batchEvents = awaitBatchEvents(recordingAppender, batchId, 12);
            assertThat(batchEvents).hasSize(12);
            assertThat(batchEvents)
                    .allSatisfy(
                            event ->
                                    assertThat(event.getMDCPropertyMap())
                                            .containsEntry("transactionId", TRANSACTION_ID)
                                            .containsKeys("traceId", "spanId"));
            assertThat(batchEvents.stream().map(AsyncLoggingExampleTest::structuredEvent))
                    .filteredOn(event -> event.type() == EventType.AUDIT)
                    .hasSize(3);

            assertThat(meterRegistry.find("smbtech.logging.async.queue.capacity").gauge())
                    .isNotNull();
            assertThat(asyncAppender.getQueueSize()).isEqualTo(2048);

            HttpResponse<String> metrics =
                    client.send(
                            HttpRequest.newBuilder()
                                    .uri(
                                            URI.create(
                                                    "http://localhost:"
                                                            + port
                                                            + "/actuator/metrics/"
                                                            + "smbtech.logging.async.queue.capacity"))
                                    .timeout(Duration.ofSeconds(5))
                                    .GET()
                                    .build(),
                            HttpResponse.BodyHandlers.ofString());
            assertThat(metrics.statusCode()).isEqualTo(200);
            assertThat(metrics.body()).contains("smbtech.logging.async.queue.capacity");
        } finally {
            scenarioLogger.detachAppender(verificationAppender);
            verificationAppender.stop();
            recordingAppender.stop();
        }
    }

    private static List<ILoggingEvent> awaitBatchEvents(
            RecordingAppender appender, String batchId, int expectedCount)
            throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        List<ILoggingEvent> events;
        do {
            events = appender.eventsFor(batchId);
            if (events.size() == expectedCount) {
                return events;
            }
            Thread.sleep(10);
        } while (System.nanoTime() < deadline);
        return events;
    }

    private static StructuredEvent structuredEvent(ILoggingEvent event) {
        return Arrays.stream(event.getArgumentArray())
                .filter(StructuredEvent.class::isInstance)
                .map(StructuredEvent.class::cast)
                .findFirst()
                .orElseThrow();
    }

    private static boolean containsStructuredEvent(ILoggingEvent event) {
        return event.getArgumentArray() != null
                && Arrays.stream(event.getArgumentArray())
                        .anyMatch(StructuredEvent.class::isInstance);
    }

    private static final class RecordingAppender extends AppenderBase<ILoggingEvent> {
        private final ConcurrentLinkedQueue<ILoggingEvent> events = new ConcurrentLinkedQueue<>();

        @Override
        protected void append(ILoggingEvent event) {
            events.add(event);
        }

        private List<ILoggingEvent> eventsFor(String batchId) {
            return events.stream()
                    .filter(AsyncLoggingExampleTest::containsStructuredEvent)
                    .filter(event -> batchId.equals(structuredEvent(event).data().get("batchId")))
                    .toList();
        }
    }
}
