package com.smbtech.examples.restclient;

import com.smbtech.examples.restclient.application.PaymentsService;
import com.smbtech.serviceframework.starter.restclient.api.ApiClientFactory;
import com.smbtech.serviceframework.starter.restclient.api.RestClientRegistry;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PublishedRestClientStarterSmokeTest {

    private static HttpServer tokenServer;
    private static HttpServer paymentsServer;
    private static String tokenUrl;
    private static String paymentsBaseUrl;
    private static final AtomicReference<Map<String, String>> tokenRequestForm = new AtomicReference<>(Map.of());
    private static final AtomicReference<String> tokenAuthorizationHeader = new AtomicReference<>();
    private static final AtomicReference<String> applicationNameHeader = new AtomicReference<>();
    private static final AtomicReference<String> authorizationHeader = new AtomicReference<>();

    @LocalServerPort
    private int port;

    @Autowired
    private PaymentsService paymentsService;

    @Autowired
    private ApiClientFactory apiClientFactory;

    @Autowired
    private RestClientRegistry restClientRegistry;

    @Autowired
    private RestClient paymentsRestClient;

    @Autowired
    private ClientRegistrationRepository clientRegistrationRepository;

    @BeforeAll
    static void startServers() throws IOException {
        tokenServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        tokenServer.createContext("/oauth2/token", PublishedRestClientStarterSmokeTest::token);
        tokenServer.setExecutor(Executors.newSingleThreadExecutor());
        tokenServer.start();
        tokenUrl = "http://localhost:" + tokenServer.getAddress().getPort() + "/oauth2/token";

        paymentsServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        paymentsServer.createContext("/dummy", PublishedRestClientStarterSmokeTest::payments);
        paymentsServer.setExecutor(Executors.newSingleThreadExecutor());
        paymentsServer.start();
        paymentsBaseUrl = "http://localhost:" + paymentsServer.getAddress().getPort();
    }

    @AfterAll
    static void stopServers() {
        if (tokenServer != null) {
            tokenServer.stop(0);
        }
        if (paymentsServer != null) {
            paymentsServer.stop(0);
        }
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.security.oauth2.client.provider.my-provider.token-uri", () -> tokenUrl);
        registry.add("spring.security.oauth2.client.registration.payments-token.client-id", () -> "smoke-client");
        registry.add("spring.security.oauth2.client.registration.payments-token.client-secret", () -> "smoke-secret");
        registry.add("smbtech.rest-clients.clients.payments.base-url", () -> paymentsBaseUrl);
        registry.add("smbtech.rest-clients.clients.payments.resilience.enabled", () -> "false");
        registry.add("management.tracing.enabled", () -> "false");
    }

    @Test
    void exposesDummyEndpointWithoutLoginAndUsesSpringBootOAuth2ClientRegistration() throws Exception {
        assertThat(apiClientFactory).isNotNull();
        assertThat(paymentsRestClient).isNotNull();
        assertThat(clientRegistrationRepository.findByRegistrationId("payments-token")).isNotNull();
        assertThat(restClientRegistry.names()).contains("payments");

        HttpResponse<String> response = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .build()
                .send(
                        HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/dummy")).GET().build(),
                        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
                );

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("ok-from-payments");
        assertThat(response.headers().firstValue("Location")).isEmpty();
        assertThat(applicationNameHeader).hasValue("rest-client-consumer-example");
        assertThat(authorizationHeader).hasValue("Bearer smoke-token");
        assertThat(tokenRequestForm.get())
                .containsEntry("grant_type", "client_credentials")
                .containsEntry("scope", "payment.read");
        assertThat(tokenAuthorizationHeader.get()).startsWith("Basic ");
    }

    @Test
    void serviceCanUseDeclarativeApiClientDirectly() {
        assertThat(paymentsService.dummy()).isEqualTo("ok-from-payments");
    }

    private static void token(HttpExchange exchange) throws IOException {
        tokenAuthorizationHeader.set(exchange.getRequestHeaders().getFirst("Authorization"));
        tokenRequestForm.set(readForm(exchange));
        byte[] body = """
                {"access_token":"smoke-token","token_type":"Bearer","expires_in":3600,"scope":"payment.read"}
                """.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, body.length);
        try (OutputStream response = exchange.getResponseBody()) {
            response.write(body);
        }
    }

    private static void payments(HttpExchange exchange) throws IOException {
        applicationNameHeader.set(exchange.getRequestHeaders().getFirst("X-Application-Name"));
        authorizationHeader.set(exchange.getRequestHeaders().getFirst("Authorization"));
        byte[] body = "ok-from-payments".getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, body.length);
        try (OutputStream response = exchange.getResponseBody()) {
            response.write(body);
        }
    }

    private static Map<String, String> readForm(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> values = new LinkedHashMap<>();
        if (body.isBlank()) {
            return values;
        }
        for (String pair : body.split("&")) {
            String[] parts = pair.split("=", 2);
            String key = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
            String value = parts.length > 1 ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8) : "";
            values.put(key, value);
        }
        return values;
    }
}
