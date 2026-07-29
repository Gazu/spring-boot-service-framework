package com.smbtech.serviceframework.starter.mock.adapter.in.openapi;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(
        classes = OpenApiMockServerIntegrationTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "smbtech.mocks.openapi.enabled=true",
            "smbtech.mocks.openapi.status-override-enabled=true",
            "smbtech.mocks.openapi.contracts.pets.location=classpath:fixtures/pet-store-mock.yaml",
            "smbtech.mocks.openapi.contracts.pets.base-path=/mock"
        })
class OpenApiMockServerIntegrationTest {

    @LocalServerPort private int port;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void servesExplicitResponseExampleAndHeaders() throws Exception {
        HttpResponse<String> response = send("GET", "/mock/pets/pet-100", null);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Content-Type"))
                .hasValueSatisfying(value -> assertThat(value).startsWith("application/json"));
        assertThat(response.headers().firstValue("X-Result-Source")).contains("openapi");
        assertThat(response.headers().firstValue("X-Mock-Operation-Id")).contains("getPet");
        JsonNode body = objectMapper.readTree(response.body());
        assertThat(body.path("id").asString()).isEqualTo("pet-100");
        assertThat(body.path("name").asString()).isEqualTo("Luna");
    }

    @Test
    void selectsAnyDeclaredResponseStatusUsingConfiguredHeader() throws Exception {
        HttpResponse<String> response = send("GET", "/mock/pets/missing", "404");

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.headers().firstValue("Content-Type"))
                .hasValueSatisfying(
                        value -> assertThat(value).startsWith("application/problem+json"));
        assertThat(objectMapper.readTree(response.body()).path("code").asString())
                .isEqualTo("pet_not_found");
    }

    @Test
    void generatesDeterministicBodyFromResponseSchema() throws Exception {
        HttpResponse<String> response = send("POST", "/mock/pets", null);

        assertThat(response.statusCode()).isEqualTo(201);
        JsonNode body = objectMapper.readTree(response.body());
        assertThat(body.path("id").asString()).hasSize(8);
        assertThat(body.path("status").asString()).isEqualTo("available");
        assertThat(body.path("active").asBoolean()).isTrue();
        assertThat(body.path("createdAt").asString()).isEqualTo("2000-01-01T00:00:00Z");
        assertThat(body.path("tags")).hasSize(2);
    }

    @Test
    void rejectsUndeclaredStatusAndSuppressesHeadBody() throws Exception {
        HttpResponse<String> invalid = send("GET", "/mock/pets/pet-100", "418");
        HttpResponse<String> head = send("HEAD", "/mock/pets/pet-100", null);

        assertThat(invalid.statusCode()).isEqualTo(400);
        assertThat(invalid.body()).contains("undeclared_mock_status");
        assertThat(head.statusCode()).isEqualTo(204);
        assertThat(head.body()).isEmpty();
    }

    private HttpResponse<String> send(String method, String path, String mockStatus)
            throws Exception {
        HttpRequest.Builder request =
                HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:" + port + path))
                        .timeout(Duration.ofSeconds(5))
                        .method(method, HttpRequest.BodyPublishers.noBody());
        if (mockStatus != null) {
            request.header("X-Mock-Status", mockStatus);
        }
        try (HttpClient client = HttpClient.newHttpClient()) {
            return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApplication {}
}
