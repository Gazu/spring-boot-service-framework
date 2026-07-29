package com.smbtech.examples.actuator;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PublishedActuatorStarterSmokeTest {

    private static final String USERNAME = "actuator-user";
    private static final String PASSWORD = "change-me";

    @LocalServerPort private int port;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void exposesTheApplicationEndpointAndPublicActuatorEndpoints() throws Exception {
        try (HttpClient client =
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()) {
            HttpResponse<String> dummy = get(client, "/api/dummy", false);
            HttpResponse<String> health = get(client, "/actuator/health", false);
            HttpResponse<String> info = get(client, "/actuator/info", false);

            assertThat(dummy.statusCode()).isEqualTo(200);
            assertThat(objectMapper.readTree(dummy.body()).path("status").asString())
                    .isEqualTo("ok");
            assertThat(health.statusCode()).isEqualTo(200);
            assertThat(objectMapper.readTree(health.body()).path("status").asString())
                    .isEqualTo("UP");
            assertThat(info.statusCode()).isEqualTo(200);
            assertThat(info.body()).contains("actuator-consumer");
        }
    }

    @Test
    void protectsTheDiagnosticAndMetricsEndpoints() throws Exception {
        try (HttpClient client =
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()) {
            assertThat(get(client, "/actuator/serviceframework", false).statusCode())
                    .isEqualTo(401);
            assertThat(
                            get(client, "/actuator/metrics/smbtech.service.framework.status", false)
                                    .statusCode())
                    .isEqualTo(401);

            HttpResponse<String> diagnostics = get(client, "/actuator/serviceframework", true);
            HttpResponse<String> metrics =
                    get(client, "/actuator/metrics/smbtech.service.framework.status", true);

            assertThat(diagnostics.statusCode()).isEqualTo(200);
            JsonNode body = objectMapper.readTree(diagnostics.body());
            assertThat(body.path("status").asString()).isEqualTo("UP");
            assertThat(
                            body.path("components")
                                    .path("example-application")
                                    .path("status")
                                    .asString())
                    .isEqualTo("UP");
            assertThat(body.path("modules").get(0).path("name").asString())
                    .isEqualTo("actuator-consumer");
            assertThat(diagnostics.body()).contains("[REDACTED]").doesNotContain("must-not-leak");

            assertThat(metrics.statusCode()).isEqualTo(200);
            assertThat(metrics.body())
                    .contains("smbtech.service.framework.status")
                    .contains("status");
        }
    }

    @Test
    void showsFrameworkHealthDetailsOnlyToTheActuatorRole() throws Exception {
        try (HttpClient client =
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()) {
            JsonNode publicHealth =
                    objectMapper.readTree(get(client, "/actuator/health", false).body());
            JsonNode authorizedHealth =
                    objectMapper.readTree(get(client, "/actuator/health", true).body());

            assertThat(publicHealth.has("components")).isFalse();
            assertThat(
                            authorizedHealth
                                    .path("components")
                                    .path("serviceFramework")
                                    .path("details")
                                    .path("components")
                                    .path("example-application")
                                    .path("status")
                                    .asString())
                    .isEqualTo("UP");
        }
    }

    private HttpResponse<String> get(HttpClient client, String path, boolean authenticated)
            throws Exception {
        HttpRequest.Builder request =
                HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:" + port + path))
                        .timeout(Duration.ofSeconds(5))
                        .GET();
        if (authenticated) {
            String credentials =
                    Base64.getEncoder()
                            .encodeToString(
                                    (USERNAME + ":" + PASSWORD).getBytes(StandardCharsets.UTF_8));
            request.header("Authorization", "Basic " + credentials);
        }
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
}
