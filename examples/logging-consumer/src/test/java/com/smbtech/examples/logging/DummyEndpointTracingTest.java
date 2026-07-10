package com.smbtech.examples.logging;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.smbtech.examples.logging.application.ProjectApplicationService;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DummyEndpointTracingTest {
    private static final String TRANSACTION_ID = "tx-test-123";

    @LocalServerPort
    private int port;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void emitsTransactionTraceAndSpanIdsInMdc() throws Exception {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        ch.qos.logback.classic.Logger logger =
                context.getLogger(ProjectApplicationService.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try (HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/dummy"))
                    .header("X-Transaction-Id", TRANSACTION_ID)
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());

            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.headers().firstValue("X-Transaction-Id"))
                    .contains(TRANSACTION_ID);

            JsonNode body = objectMapper.readTree(response.body());
            assertThat(body.path("status").asText()).isEqualTo("ok");
            assertThat(body.path("transactionId").asText()).isEqualTo(TRANSACTION_ID);
            assertThat(body.path("traceId").asText()).hasSize(32);
            assertThat(body.path("spanId").asText()).hasSize(16);

            assertThat(appender.list)
                    .filteredOn(event -> event.getFormattedMessage()
                            .equals("Dummy endpoint invoked"))
                    .singleElement()
                    .satisfies(event -> {
                        Map<String, String> mdc = event.getMDCPropertyMap();
                        assertThat(mdc)
                                .containsEntry("transactionId", TRANSACTION_ID)
                                .containsEntry("traceId", body.path("traceId").asText())
                                .containsEntry("spanId", body.path("spanId").asText());
                    });
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }
}
