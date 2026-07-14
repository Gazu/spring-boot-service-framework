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
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.Signature;
import java.security.cert.Certificate;
import java.util.Base64;
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
    private static Path keyStore;
    private static String keyStoreBase64;
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
    static void startServers() throws Exception {
        keyStore = createKeyStore();
        keyStoreBase64 = Base64.getEncoder().encodeToString(Files.readAllBytes(keyStore));

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
        registry.add("spring.security.oauth2.client.provider.core-oauth-gateway.token-uri", () -> tokenUrl);
        registry.add(
                "spring.security.oauth2.client.registration.payments-client-credentials-token.client-id",
                () -> "smoke-client"
        );
        registry.add(
                "spring.security.oauth2.client.registration.payments-client-credentials-token.client-authentication-method",
                () -> "private_key_jwt"
        );
        registry.add(
                "spring.security.oauth2.client.registration.payments-jwt-bearer-token.client-id",
                () -> "smoke-jwt-bearer-client"
        );
        registry.add(
                "spring.security.oauth2.client.registration.payments-jwt-bearer-token.client-authentication-method",
                () -> "none"
        );
        registry.add("smbtech.rest-clients.clients.payments.base-url", () -> paymentsBaseUrl);
        registry.add("smbtech.rest-clients.clients.payments-jwt-bearer.base-url", () -> paymentsBaseUrl);
        registry.add("smbtech.rest-clients.clients.payments.resilience.enabled", () -> "false");
        registry.add(
                "smbtech.rest-clients.authentication.key-stores.payments-client-credentials-signing-key.base64",
                () -> keyStoreBase64
        );
        registry.add(
                "smbtech.rest-clients.authentication.key-stores.payments-client-credentials-signing-key.type",
                () -> "PKCS12"
        );
        registry.add(
                "smbtech.rest-clients.authentication.key-stores.payments-client-credentials-signing-key.key-alias",
                () -> "auth"
        );
        registry.add(
                "smbtech.rest-clients.authentication.key-stores.payments-jwt-bearer-signing-key.base64",
                () -> keyStoreBase64
        );
        registry.add(
                "smbtech.rest-clients.authentication.key-stores.payments-jwt-bearer-signing-key.type",
                () -> "PKCS12"
        );
        registry.add(
                "smbtech.rest-clients.authentication.key-stores.payments-jwt-bearer-signing-key.key-alias",
                () -> "auth"
        );
        registry.add(
                "smbtech.rest-clients.authentication.credentials.payments-client-credentials-keystore-password.base64",
                () -> encoded("changeit")
        );
        registry.add(
                "smbtech.rest-clients.authentication.credentials.payments-client-credentials-key-password.base64",
                () -> encoded("changeit")
        );
        registry.add(
                "smbtech.rest-clients.authentication.credentials.payments-jwt-bearer-keystore-password.base64",
                () -> encoded("changeit")
        );
        registry.add(
                "smbtech.rest-clients.authentication.credentials.payments-jwt-bearer-key-password.base64",
                () -> encoded("changeit")
        );
        registry.add("PAYMENTS_JWT_BEARER_ISSUER", () -> "smoke-jwt-bearer-client");
        registry.add("PAYMENTS_JWT_BEARER_SUBJECT", () -> "smoke-jwt-bearer-client");
        registry.add("PAYMENTS_JWT_BEARER_AUDIENCE", () -> tokenUrl);
        registry.add("PAYMENTS_JWT_BEARER_CUSTOMER_ID", () -> "17952397-3");
        registry.add("management.tracing.enabled", () -> "false");
    }

    @Test
    void exposesDummyEndpointWithoutLoginAndUsesSpringBootOAuth2ClientRegistration() throws Exception {
        assertThat(apiClientFactory).isNotNull();
        assertThat(paymentsRestClient).isNotNull();
        assertThat(clientRegistrationRepository.findByRegistrationId("payments-client-credentials-token")).isNotNull();
        assertThat(clientRegistrationRepository.findByRegistrationId("payments-jwt-bearer-token")).isNotNull();
        assertThat(restClientRegistry.names()).contains("payments", "payments-jwt-bearer");

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
                .containsEntry("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer");
        assertThat(tokenAuthorizationHeader.get()).isNull();
        assertThat(tokenRequestForm.get().get("assertion")).isNotBlank();
        assertThat(verify(tokenRequestForm.get().get("assertion"))).isTrue();
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

    private static Path createKeyStore() throws Exception {
        Path keyStorePath = Files.createTempDirectory("payments-jwt-bearer-signing-key").resolve("auth.p12");
        Path keytool = Path.of(System.getProperty("java.home"), "bin", executable("keytool"));
        Process process = new ProcessBuilder(
                keytool.toString(),
                "-genkeypair",
                "-alias", "auth",
                "-keyalg", "RSA",
                "-keysize", "2048",
                "-storetype", "PKCS12",
                "-keystore", keyStorePath.toString(),
                "-storepass", "changeit",
                "-keypass", "changeit",
                "-dname", "CN=Smoke Test",
                "-validity", "365",
                "-noprompt"
        ).redirectErrorStream(true).start();

        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IllegalStateException("keytool failed: " + output);
        }
        return keyStorePath;
    }

    private static boolean verify(String assertion) throws Exception {
        String[] parts = assertion.split("\\.");
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initVerify(certificate().getPublicKey());
        signature.update((parts[0] + "." + parts[1]).getBytes(StandardCharsets.UTF_8));
        return signature.verify(Base64.getUrlDecoder().decode(parts[2]));
    }

    private static Certificate certificate() throws Exception {
        KeyStore store = KeyStore.getInstance("PKCS12");
        try (var inputStream = Files.newInputStream(keyStore)) {
            store.load(inputStream, "changeit".toCharArray());
        }
        return store.getCertificate("auth");
    }

    private static String encoded(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String executable(String name) {
        return System.getProperty("os.name", "").toLowerCase().contains("win") ? name + ".exe" : name;
    }
}
