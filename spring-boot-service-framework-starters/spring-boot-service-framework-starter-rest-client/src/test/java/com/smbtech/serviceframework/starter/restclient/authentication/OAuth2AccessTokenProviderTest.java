package com.smbtech.serviceframework.starter.restclient.authentication;

import com.smbtech.serviceframework.httpclient.domain.AccessToken;
import com.smbtech.serviceframework.httpclient.exception.AuthenticationException;
import com.smbtech.serviceframework.httpclient.port.out.AccessTokenProvider;
import com.smbtech.serviceframework.starter.restclient.api.AccessTokenClient;
import com.smbtech.serviceframework.starter.restclient.autoconfigure.RestClientAutoConfiguration;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.Signature;
import java.security.cert.Certificate;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OAuth2AccessTokenProviderTest {

    @TempDir
    Path tempDir;

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void obtainsClientCredentialsTokenAndCachesIt() throws Exception {
        TokenEndpoint endpoint = startTokenEndpoint("""
                {"access_token":"token-123","token_type":"Bearer","expires_in":3600,"scope":"customer.read customer.write"}
                """);

        springClientCredentialsContextRunner(endpoint.url())
                .run(context -> {
                    AccessTokenProvider provider = context.getBean(AccessTokenProvider.class);

                    String first = provider.getAccessToken("customer-api", "customer.read");
                    String second = provider.getAccessToken("customer-api", "customer.read");

                    assertThat(first).isEqualTo("token-123");
                    assertThat(second).isEqualTo("token-123");
                    assertThat(endpoint.requestCount()).isEqualTo(1);
                    assertThat(endpoint.authorizationHeader()).isEqualTo("Basic ZGVtby1jbGllbnQ6ZGVtby1zZWNyZXQ=");
                    assertThat(endpoint.formValues()).containsEntry("grant_type", "client_credentials");
                    assertThat(endpoint.formValues()).containsEntry("scope", "customer.read customer.write");
                });
    }

    @Test
    void obtainsClientCredentialsAccessTokenThroughPublicTokenClient() throws Exception {
        TokenEndpoint endpoint = startTokenEndpoint("""
                {"access_token":"token-client-123","token_type":"Bearer","expires_in":3600,"scope":"customer.read customer.write"}
                """);

        springClientCredentialsContextRunner(endpoint.url())
                .run(context -> {
                    AccessTokenClient client = context.getBean(AccessTokenClient.class);

                    AccessToken first = client.clientCredentials("customer-api", "customer.read");
                    AccessToken second = client.clientCredentials("customer-api", "customer.read");

                    assertThat(first.value()).isEqualTo("token-client-123");
                    assertThat(first.tokenType()).isEqualTo("Bearer");
                    assertThat(first.scopes()).containsExactlyInAnyOrder("customer.read", "customer.write");
                    assertThat(second).isSameAs(first);
                    assertThat(endpoint.requestCount()).isEqualTo(1);
                    assertThat(endpoint.authorizationHeader()).isEqualTo("Basic ZGVtby1jbGllbnQ6ZGVtby1zZWNyZXQ=");
                    assertThat(endpoint.formValues()).containsEntry("grant_type", "client_credentials");
                });
    }

    @Test
    void obtainsClientCredentialsTokenWithSpringSecurityRegistrationAndCachesIt() throws Exception {
        TokenEndpoint endpoint = startTokenEndpoint("""
                {"access_token":"spring-token-123","token_type":"Bearer","expires_in":3600,"scope":"customer.read customer.write"}
                """);

        springClientCredentialsContextRunner(endpoint.url())
                .run(context -> {
                    AccessTokenProvider provider = context.getBean(AccessTokenProvider.class);

                    String first = provider.getAccessToken("customer-api", "customer.read");
                    String second = provider.getAccessToken("customer-api", "customer.read");

                    assertThat(first).isEqualTo("spring-token-123");
                    assertThat(second).isEqualTo("spring-token-123");
                    assertThat(endpoint.requestCount()).isEqualTo(1);
                    assertThat(endpoint.authorizationHeader()).isEqualTo("Basic ZGVtby1jbGllbnQ6ZGVtby1zZWNyZXQ=");
                    assertThat(endpoint.formValues()).containsEntry("grant_type", "client_credentials");
                    assertThat(endpoint.formValues()).containsEntry("scope", "customer.read customer.write");
                });
    }

    @Test
    void obtainsClientCredentialsAccessTokenThroughPublicTokenClientWithSpringSecurityRegistration() throws Exception {
        TokenEndpoint endpoint = startTokenEndpoint("""
                {"access_token":"spring-token-client-123","token_type":"Bearer","expires_in":3600,"scope":"customer.read customer.write"}
                """);

        springClientCredentialsContextRunner(endpoint.url())
                .run(context -> {
                    AccessTokenClient client = context.getBean(AccessTokenClient.class);

                    AccessToken first = client.clientCredentials("customer-api", "customer.read");
                    AccessToken second = client.clientCredentials("customer-api", "customer.read");

                    assertThat(first.value()).isEqualTo("spring-token-client-123");
                    assertThat(first.tokenType()).isEqualTo("Bearer");
                    assertThat(first.scopes()).containsExactlyInAnyOrder("customer.read", "customer.write");
                    assertThat(second).isSameAs(first);
                    assertThat(endpoint.requestCount()).isEqualTo(1);
                    assertThat(endpoint.authorizationHeader()).isEqualTo("Basic ZGVtby1jbGllbnQ6ZGVtby1zZWNyZXQ=");
                    assertThat(endpoint.formValues()).containsEntry("grant_type", "client_credentials");
                    assertThat(endpoint.formValues()).containsEntry("scope", "customer.read customer.write");
                });
    }

    @Test
    void validatesSpringClientCredentialsTokenWithRegistrationScopesWhenResponseOmitsScope() throws Exception {
        TokenEndpoint endpoint = startTokenEndpoint("""
                {"access_token":"spring-token-without-scope-123","token_type":"Bearer","expires_in":3600}
                """);

        springClientCredentialsContextRunner(endpoint.url())
                .run(context -> {
                    AccessTokenProvider provider = context.getBean(AccessTokenProvider.class);

                    String first = provider.getAccessToken("customer-api", "customer.write");
                    String second = provider.getAccessToken("customer-api", "customer.read");

                    assertThat(first).isEqualTo("spring-token-without-scope-123");
                    assertThat(second).isEqualTo("spring-token-without-scope-123");
                    assertThat(endpoint.requestCount()).isEqualTo(1);
                    assertThat(endpoint.formValues()).containsEntry("scope", "customer.read customer.write");
                });
    }

    @Test
    void obtainsPrivateKeyJwtClientCredentialsTokenWithSpringSecurityRegistration() throws Exception {
        Path keyStore = createKeyStore();
        TokenEndpoint endpoint = startTokenEndpoint("""
                {"access_token":"private-key-jwt-token-123","token_type":"Bearer","expires_in":3600,"scope":"customer.read customer.write"}
                """);

        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(RestClientAutoConfiguration.class))
                .withBean(ClientRegistrationRepository.class, () -> privateKeyJwtClientRegistrationRepository(endpoint.url()))
                .withPropertyValues(
                        "smbtech.rest-clients.clients.customer.base-url=https://customer.example",
                        "smbtech.rest-clients.clients.customer.authentication-type=CLIENT_CREDENTIALS",
                        "smbtech.rest-clients.clients.customer.credential-token-requestor-id=customer-api",
                        "smbtech.rest-clients.clients.customer.scopes=customer.read",
                        "smbtech.rest-clients.authentication.client-assertions.customer-api.key-store-id=auth-key",
                        "smbtech.rest-clients.authentication.client-assertions.customer-api.token-lifetime=75s",
                        "smbtech.rest-clients.authentication.client-assertions.customer-api.custom-claims.acgp=acgp.ct",
                        "smbtech.rest-clients.authentication.client-assertions.customer-api.custom-claims.channel=backend",
                        "smbtech.rest-clients.authentication.key-stores.auth-key.location=file:" + keyStore,
                        "smbtech.rest-clients.authentication.key-stores.auth-key.type=PKCS12",
                        "smbtech.rest-clients.authentication.key-stores.auth-key.password-ref=keystore-password",
                        "smbtech.rest-clients.authentication.key-stores.auth-key.key-alias=auth",
                        "smbtech.rest-clients.authentication.key-stores.auth-key.key-password-ref=key-password",
                        "smbtech.rest-clients.authentication.credentials.keystore-password.base64=" + encoded("changeit"),
                        "smbtech.rest-clients.authentication.credentials.key-password.base64=" + encoded("changeit")
                )
                .run(context -> {
                    AccessTokenProvider provider = context.getBean(AccessTokenProvider.class);

                    String token = provider.getAccessToken("customer-api", "customer.read");

                    assertThat(token).isEqualTo("private-key-jwt-token-123");
                    assertThat(endpoint.authorizationHeader()).isNull();
                    assertThat(endpoint.formValues()).containsEntry("grant_type", "client_credentials");
                    assertThat(endpoint.formValues()).containsEntry("scope", "customer.read customer.write");
                    assertThat(endpoint.formValues().get("client_assertion_type")).contains("jwt-bearer");

                    String assertion = endpoint.formValues().get("client_assertion");
                    assertThat(assertion).isNotBlank();
                    assertThat(verify(assertion, keyStore)).isTrue();
                    assertThat(decodePayload(assertion))
                            .contains("\"iss\":\"demo-client\"")
                            .contains("\"sub\":\"demo-client\"")
                            .contains("\"aud\":\"" + endpoint.url() + "\"")
                            .contains("\"acgp\":\"acgp.ct\"")
                            .contains("\"channel\":\"backend\"");
                });
    }

    @Test
    void obtainsJwtBearerGrantTokenWithSpringSecurityRegistration() throws Exception {
        Path keyStore = createKeyStore();
        TokenEndpoint endpoint = startTokenEndpoint("""
                {"access_token":"spring-jwt-bearer-token-123","token_type":"Bearer","expires_in":3600,"scope":"payments.write payments.read"}
                """);

        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(RestClientAutoConfiguration.class))
                .withBean(ClientRegistrationRepository.class, () -> jwtBearerClientRegistrationRepository(endpoint.url()))
                .withPropertyValues(
                        "smbtech.rest-clients.clients.payments.base-url=https://payments.example",
                        "smbtech.rest-clients.clients.payments.authentication-type=CLIENT_CREDENTIALS",
                        "smbtech.rest-clients.clients.payments.credential-token-requestor-id=payments-api",
                        "smbtech.rest-clients.clients.payments.scopes=payments.write",
                        "smbtech.rest-clients.authentication.jwt-bearer.payments-api.key-store-id=auth-key",
                        "smbtech.rest-clients.authentication.jwt-bearer.payments-api.issuer=payments-issuer",
                        "smbtech.rest-clients.authentication.jwt-bearer.payments-api.subject=payments-subject",
                        "smbtech.rest-clients.authentication.jwt-bearer.payments-api.audience=https://auth.example/token",
                        "smbtech.rest-clients.authentication.jwt-bearer.payments-api.token-lifetime=2m",
                        "smbtech.rest-clients.authentication.jwt-bearer.payments-api.custom-claims.tenant=payments",
                        "smbtech.rest-clients.authentication.jwt-bearer.payments-api.custom-claims.channel=backend",
                        "smbtech.rest-clients.authentication.key-stores.auth-key.location=file:" + keyStore,
                        "smbtech.rest-clients.authentication.key-stores.auth-key.type=PKCS12",
                        "smbtech.rest-clients.authentication.key-stores.auth-key.password-ref=keystore-password",
                        "smbtech.rest-clients.authentication.key-stores.auth-key.key-alias=auth",
                        "smbtech.rest-clients.authentication.key-stores.auth-key.key-password-ref=key-password",
                        "smbtech.rest-clients.authentication.credentials.keystore-password.base64=" + encoded("changeit"),
                        "smbtech.rest-clients.authentication.credentials.key-password.base64=" + encoded("changeit")
                )
                .run(context -> {
                    AccessTokenProvider provider = context.getBean(AccessTokenProvider.class);
                    AccessTokenClient client = context.getBean(AccessTokenClient.class);

                    String first = provider.getAccessToken("payments-api", "payments.write");
                    AccessToken second = client.jwtBearer("payments-api", "payments.write");

                    assertThat(first).isEqualTo("spring-jwt-bearer-token-123");
                    assertThat(second.value()).isEqualTo("spring-jwt-bearer-token-123");
                    assertThat(endpoint.requestCount()).isEqualTo(1);
                    assertThat(endpoint.authorizationHeader()).isNull();
                    assertThat(endpoint.formValues()).containsEntry(
                            "grant_type",
                            "urn:ietf:params:oauth:grant-type:jwt-bearer"
                    );
                    assertThat(endpoint.formValues()).containsEntry("client_id", "payments-client");
                    assertThat(endpoint.formValues()).containsEntry("scope", "payments.write payments.read");

                    String assertion = endpoint.formValues().get("assertion");
                    assertThat(assertion).isNotBlank();
                    assertThat(verify(assertion, keyStore)).isTrue();
                    assertThat(decodePayload(assertion))
                            .contains("\"iss\":\"payments-issuer\"")
                            .contains("\"sub\":\"payments-subject\"")
                            .contains("\"aud\":\"https://auth.example/token\"")
                            .contains("\"tenant\":\"payments\"")
                            .contains("\"channel\":\"backend\"");
                });
    }

    @Test
    void rejectsTokenWhenExpectedScopeIsMissing() throws Exception {
        TokenEndpoint endpoint = startTokenEndpoint("""
                {"access_token":"token-456","token_type":"Bearer","expires_in":3600,"scope":"customer.read"}
                """);

        springClientCredentialsContextRunner(endpoint.url())
                .run(context -> {
                    AccessTokenProvider provider = context.getBean(AccessTokenProvider.class);

                    assertThatThrownBy(() -> provider.getAccessToken("customer-api", "customer.write"))
                            .isInstanceOf(AuthenticationException.class)
                            .hasMessageContaining("expected=[customer.write]")
                            .hasMessageContaining("actual=[customer.read]");
                });
    }

    @Test
    void failsFastWhenSpringRegistrationRepositoryIsMissing() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(RestClientAutoConfiguration.class))
                .withPropertyValues(
                        "smbtech.rest-clients.clients.customer.base-url=https://customer.example",
                        "smbtech.rest-clients.clients.customer.authentication-type=CLIENT_CREDENTIALS",
                        "smbtech.rest-clients.clients.customer.credential-token-requestor-id=customer-api",
                        "smbtech.rest-clients.clients.customer.scopes=customer.read"
                )
                .run(context -> {
                    AccessTokenProvider provider = context.getBean(AccessTokenProvider.class);
                    AccessTokenClient client = context.getBean(AccessTokenClient.class);

                    assertThatThrownBy(() -> provider.getAccessToken("customer-api", "customer.read"))
                            .isInstanceOf(AuthenticationException.class)
                            .hasMessageContaining("OAuth2 client registration not configured for token request: customer-api");
                    assertThatThrownBy(() -> client.clientCredentials("customer-api", "customer.read"))
                            .isInstanceOf(AuthenticationException.class)
                            .hasMessageContaining("OAuth2 client registration not configured for client_credentials: customer-api");
                });
    }

    @Test
    void publicTokenClientRejectsGrantMismatch() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(RestClientAutoConfiguration.class))
                .withBean(
                        ClientRegistrationRepository.class,
                        () -> jwtBearerClientRegistrationRepository("https://auth.example/token")
                )
                .run(context -> {
                    AccessTokenClient client = context.getBean(AccessTokenClient.class);

                    assertThatThrownBy(() -> client.clientCredentials("payments-api", "payments.write"))
                            .isInstanceOf(AuthenticationException.class)
                            .hasMessageContaining("OAuth2 client registration not configured for client_credentials: payments-api");
                });
    }

    @Test
    void jwtBearerGrantRequiresSmbTechJwtBearerExtensionConfiguration() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(RestClientAutoConfiguration.class))
                .withBean(
                        ClientRegistrationRepository.class,
                        () -> jwtBearerClientRegistrationRepository("https://auth.example/token")
                )
                .run(context -> {
                    AccessTokenProvider provider = context.getBean(AccessTokenProvider.class);
                    AccessTokenClient client = context.getBean(AccessTokenClient.class);

                    assertThatThrownBy(() -> provider.getAccessToken("payments-api", "payments.write"))
                            .isInstanceOf(AuthenticationException.class)
                            .hasMessageContaining("jwt-bearer configuration not found for OAuth2 registration: payments-api");
                    assertThatThrownBy(() -> client.jwtBearer("payments-api", "payments.write"))
                            .isInstanceOf(AuthenticationException.class)
                            .hasMessageContaining("jwt-bearer configuration not found for OAuth2 registration: payments-api");
                });
    }

    private ApplicationContextRunner springClientCredentialsContextRunner(String tokenUri) {
        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(RestClientAutoConfiguration.class))
                .withBean(ClientRegistrationRepository.class, () -> clientRegistrationRepository(tokenUri))
                .withPropertyValues(
                        "smbtech.rest-clients.clients.customer.base-url=https://customer.example",
                        "smbtech.rest-clients.clients.customer.authentication-type=CLIENT_CREDENTIALS",
                        "smbtech.rest-clients.clients.customer.credential-token-requestor-id=customer-api",
                        "smbtech.rest-clients.clients.customer.scopes=customer.read"
                );
    }

    private ClientRegistrationRepository clientRegistrationRepository(String tokenUri) {
        ClientRegistration customerApi = ClientRegistration
                .withRegistrationId("customer-api")
                .tokenUri(tokenUri)
                .clientId("demo-client")
                .clientSecret("demo-secret")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .scope("customer.read", "customer.write")
                .build();
        return new InMemoryClientRegistrationRepository(customerApi);
    }

    private ClientRegistrationRepository privateKeyJwtClientRegistrationRepository(String tokenUri) {
        ClientRegistration customerApi = ClientRegistration
                .withRegistrationId("customer-api")
                .tokenUri(tokenUri)
                .clientId("demo-client")
                .clientAuthenticationMethod(ClientAuthenticationMethod.PRIVATE_KEY_JWT)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .scope("customer.read", "customer.write")
                .build();
        return new InMemoryClientRegistrationRepository(customerApi);
    }

    private ClientRegistrationRepository jwtBearerClientRegistrationRepository(String tokenUri) {
        ClientRegistration paymentsApi = ClientRegistration
                .withRegistrationId("payments-api")
                .tokenUri(tokenUri)
                .clientId("payments-client")
                .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                .authorizationGrantType(new AuthorizationGrantType("urn:ietf:params:oauth:grant-type:jwt-bearer"))
                .scope("payments.write", "payments.read")
                .build();
        return new InMemoryClientRegistrationRepository(paymentsApi);
    }

    private TokenEndpoint startTokenEndpoint(String responseBody) throws IOException {
        AtomicInteger requests = new AtomicInteger();
        AtomicReference<String> authorization = new AtomicReference<>();
        AtomicReference<Map<String, String>> form = new AtomicReference<>(Map.of());

        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/token", exchange -> {
            requests.incrementAndGet();
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            form.set(readForm(exchange));
            byte[] response = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.setExecutor(Executors.newSingleThreadExecutor());
        server.start();

        return new TokenEndpoint(
                "http://localhost:" + server.getAddress().getPort() + "/token",
                requests,
                authorization,
                form
        );
    }

    private Map<String, String> readForm(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> values = new LinkedHashMap<>();
        for (String pair : body.split("&")) {
            String[] parts = pair.split("=", 2);
            if (parts.length == 2) {
                values.put(decode(parts[0]), decode(parts[1]));
            }
        }
        return values;
    }

    private String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private Path createKeyStore() throws Exception {
        Path keyStore = tempDir.resolve("auth.p12");
        Path keytool = Path.of(System.getProperty("java.home"), "bin", executable("keytool"));
        Process process = new ProcessBuilder(
                keytool.toString(),
                "-genkeypair",
                "-alias", "auth",
                "-keyalg", "RSA",
                "-keysize", "2048",
                "-storetype", "PKCS12",
                "-keystore", keyStore.toString(),
                "-storepass", "changeit",
                "-keypass", "changeit",
                "-dname", "CN=Private Key JWT Test",
                "-validity", "365",
                "-noprompt"
        ).redirectErrorStream(true).start();

        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IllegalStateException("keytool failed: " + output);
        }
        return keyStore;
    }

    private String executable(String name) {
        return System.getProperty("os.name", "").toLowerCase().contains("win") ? name + ".exe" : name;
    }

    private boolean verify(String assertion, Path keyStorePath) throws Exception {
        String[] parts = assertion.split("\\.");
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initVerify(certificate(keyStorePath).getPublicKey());
        signature.update((parts[0] + "." + parts[1]).getBytes(StandardCharsets.UTF_8));
        return signature.verify(Base64.getUrlDecoder().decode(parts[2]));
    }

    private Certificate certificate(Path keyStorePath) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (var inputStream = java.nio.file.Files.newInputStream(keyStorePath)) {
            keyStore.load(inputStream, "changeit".toCharArray());
        }
        return keyStore.getCertificate("auth");
    }

    private String decodePayload(String assertion) {
        String[] parts = assertion.split("\\.");
        return new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
    }

    private String encoded(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private record TokenEndpoint(
            String url,
            AtomicInteger requests,
            AtomicReference<String> authorization,
            AtomicReference<Map<String, String>> form
    ) {
        public int requestCount() {
            return requests.get();
        }

        public String authorizationHeader() {
            return authorization.get();
        }

        public Map<String, String> formValues() {
            return form.get();
        }
    }
}
