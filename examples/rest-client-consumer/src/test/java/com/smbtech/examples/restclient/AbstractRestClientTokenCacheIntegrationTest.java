package com.smbtech.examples.restclient;

import com.smbtech.examples.restclient.application.PaymentsService;
import com.smbtech.serviceframework.starter.restclient.api.RestClientRegistry;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

abstract class AbstractRestClientTokenCacheIntegrationTest {

    private static HttpServer tokenServer;
    private static HttpServer paymentsServer;
    private static String tokenUrl;
    private static String paymentsBaseUrl;
    private static String keyStoreBase64;

    private static final AtomicInteger tokenRequests = new AtomicInteger();
    private static final AtomicInteger clientCredentialsTokenRequests = new AtomicInteger();
    private static final AtomicInteger jwtBearerTokenRequests = new AtomicInteger();
    private static final AtomicInteger paymentsRequests = new AtomicInteger();
    private static final AtomicReference<List<String>> paymentsAuthorizationHeaders =
            new AtomicReference<>(List.of());

    @Autowired protected RestClientRegistry restClientRegistry;

    @Autowired protected PaymentsService paymentsService;

    @BeforeAll
    static void startServers() throws Exception {
        Path keyStore = createKeyStore();
        keyStoreBase64 = Base64.getEncoder().encodeToString(Files.readAllBytes(keyStore));

        tokenServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        tokenServer.createContext(
                "/oauth2/token", AbstractRestClientTokenCacheIntegrationTest::token);
        tokenServer.setExecutor(Executors.newSingleThreadExecutor());
        tokenServer.start();
        tokenUrl = "http://localhost:" + tokenServer.getAddress().getPort() + "/oauth2/token";

        paymentsServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        paymentsServer.createContext(
                "/dummy", AbstractRestClientTokenCacheIntegrationTest::payments);
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

    @BeforeEach
    void resetObservations() {
        tokenRequests.set(0);
        clientCredentialsTokenRequests.set(0);
        jwtBearerTokenRequests.set(0);
        paymentsRequests.set(0);
        paymentsAuthorizationHeaders.set(List.of());
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add(
                "spring.security.oauth2.client.provider.core-oauth-gateway.token-uri",
                () -> tokenUrl);
        registry.add(
                "spring.security.oauth2.client.registration.payments-client-credentials-token.client-id",
                () -> "integration-client-credentials-client");
        registry.add(
                "spring.security.oauth2.client.registration.payments-client-credentials-token.client-secret",
                () -> "integration-client-credentials-secret");
        registry.add(
                "spring.security.oauth2.client.registration.payments-client-credentials-token.client-authentication-method",
                () -> "client_secret_basic");
        registry.add(
                "spring.security.oauth2.client.registration.payments-jwt-bearer-token.client-id",
                () -> "integration-jwt-bearer-client");
        registry.add(
                "spring.security.oauth2.client.registration.payments-jwt-bearer-token.client-authentication-method",
                () -> "none");
        registry.add("smbtech.rest-clients.clients.payments.base-url", () -> paymentsBaseUrl);
        registry.add(
                "smbtech.rest-clients.clients.payments-jwt-bearer.base-url", () -> paymentsBaseUrl);
        registry.add("smbtech.rest-clients.clients.payments.resilience.enabled", () -> "false");
        registry.add(
                "smbtech.rest-clients.authentication.jwt-bearer.payments-jwt-bearer-token.issuer",
                () -> "integration-jwt-bearer-client");
        registry.add(
                "smbtech.rest-clients.authentication.jwt-bearer.payments-jwt-bearer-token.subject",
                () -> "integration-jwt-bearer-client");
        registry.add(
                "smbtech.rest-clients.authentication.jwt-bearer.payments-jwt-bearer-token.audience",
                () -> tokenUrl);
        registry.add(
                "smbtech.rest-clients.authentication.key-stores.payments-jwt-bearer-signing-key.base64",
                () -> keyStoreBase64);
        registry.add(
                "smbtech.rest-clients.authentication.key-stores.payments-jwt-bearer-signing-key.type",
                () -> "PKCS12");
        registry.add(
                "smbtech.rest-clients.authentication.key-stores.payments-jwt-bearer-signing-key.key-alias",
                () -> "auth");
        registry.add(
                "smbtech.rest-clients.authentication.credentials.payments-jwt-bearer-keystore-password.base64",
                () -> encoded("changeit"));
        registry.add(
                "smbtech.rest-clients.authentication.credentials.payments-jwt-bearer-key-password.base64",
                () -> encoded("changeit"));
        registry.add("PAYMENTS_JWT_BEARER_CUSTOMER_ID", () -> "17952397-3");
        registry.add("management.tracing.enabled", () -> "false");
    }

    protected String getWithClientCredentialsRestClient() {
        return restClientRegistry.get("payments").get().uri("/dummy").retrieve().body(String.class);
    }

    protected int tokenRequests() {
        return tokenRequests.get();
    }

    protected int clientCredentialsTokenRequests() {
        return clientCredentialsTokenRequests.get();
    }

    protected int jwtBearerTokenRequests() {
        return jwtBearerTokenRequests.get();
    }

    protected int paymentsRequests() {
        return paymentsRequests.get();
    }

    protected List<String> paymentsAuthorizationHeaders() {
        return paymentsAuthorizationHeaders.get();
    }

    private static void token(HttpExchange exchange) throws IOException {
        Map<String, String> form = readForm(exchange);
        String grantType = form.get("grant_type");
        tokenRequests.incrementAndGet();

        String accessToken;
        if ("client_credentials".equals(grantType)) {
            accessToken =
                    "client-credentials-token-" + clientCredentialsTokenRequests.incrementAndGet();
        } else if ("urn:ietf:params:oauth:grant-type:jwt-bearer".equals(grantType)) {
            accessToken = "jwt-bearer-token-" + jwtBearerTokenRequests.incrementAndGet();
        } else {
            accessToken = "unexpected-token";
        }

        byte[] body =
                """
                {"access_token":"%s","token_type":"Bearer","expires_in":3600,"scope":"payment.read"}
                """
                        .formatted(accessToken)
                        .getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, body.length);
        try (OutputStream response = exchange.getResponseBody()) {
            response.write(body);
        }
    }

    private static void payments(HttpExchange exchange) throws IOException {
        paymentsRequests.incrementAndGet();
        List<String> headers = new ArrayList<>(paymentsAuthorizationHeaders.get());
        headers.add(exchange.getRequestHeaders().getFirst("Authorization"));
        paymentsAuthorizationHeaders.set(List.copyOf(headers));

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
            String value =
                    parts.length > 1 ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8) : "";
            values.put(key, value);
        }
        return values;
    }

    private static Path createKeyStore() throws Exception {
        Path keyStorePath =
                Files.createTempDirectory("payments-jwt-bearer-signing-key").resolve("auth.p12");
        Path keytool = Path.of(System.getProperty("java.home"), "bin", executable("keytool"));
        Process process =
                new ProcessBuilder(
                                keytool.toString(),
                                "-genkeypair",
                                "-alias",
                                "auth",
                                "-keyalg",
                                "RSA",
                                "-keysize",
                                "2048",
                                "-storetype",
                                "PKCS12",
                                "-keystore",
                                keyStorePath.toString(),
                                "-storepass",
                                "changeit",
                                "-keypass",
                                "changeit",
                                "-dname",
                                "CN=Token Cache Integration Test",
                                "-validity",
                                "365",
                                "-noprompt")
                        .redirectErrorStream(true)
                        .start();

        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IllegalStateException("keytool failed: " + output);
        }
        return keyStorePath;
    }

    private static String encoded(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String executable(String name) {
        return System.getProperty("os.name", "").toLowerCase().contains("win")
                ? name + ".exe"
                : name;
    }
}
