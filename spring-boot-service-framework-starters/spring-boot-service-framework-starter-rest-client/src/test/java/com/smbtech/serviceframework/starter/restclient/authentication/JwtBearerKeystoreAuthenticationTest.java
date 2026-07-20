package com.smbtech.serviceframework.starter.restclient.authentication;

import static org.assertj.core.api.Assertions.assertThat;

import com.smbtech.serviceframework.httpclient.port.out.AccessTokenProvider;
import com.smbtech.serviceframework.starter.restclient.autoconfigure.RestClientAutoConfiguration;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
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

class JwtBearerKeystoreAuthenticationTest {

    private HttpServer server;

    @TempDir Path tempDir;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void createsSignedJwtBearerAssertionFromConfiguredKeyStore() throws Exception {
        Path keyStore = createKeyStore();
        TokenEndpoint endpoint = startTokenEndpoint();

        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(RestClientAutoConfiguration.class))
                .withBean(
                        ClientRegistrationRepository.class,
                        () -> jwtBearerRegistrationRepository(endpoint.url()))
                .withPropertyValues(
                        "smbtech.rest-clients.authentication.key-stores.auth-key.location=file:"
                                + keyStore,
                        "smbtech.rest-clients.authentication.key-stores.auth-key.type=PKCS12",
                        "smbtech.rest-clients.authentication.key-stores.auth-key.password-ref=keystore-password",
                        "smbtech.rest-clients.authentication.key-stores.auth-key.key-alias=auth",
                        "smbtech.rest-clients.authentication.key-stores.auth-key.key-password-ref=key-password",
                        "smbtech.rest-clients.authentication.credentials.keystore-password.value=changeit",
                        "smbtech.rest-clients.authentication.credentials.key-password.value=changeit",
                        "smbtech.rest-clients.authentication.jwt-bearer.payments-api.key-store-id=auth-key",
                        "smbtech.rest-clients.authentication.jwt-bearer.payments-api.issuer=payments-issuer",
                        "smbtech.rest-clients.authentication.jwt-bearer.payments-api.subject=payments-subject",
                        "smbtech.rest-clients.authentication.jwt-bearer.payments-api.audience=https://auth.example/token",
                        "smbtech.rest-clients.authentication.jwt-bearer.payments-api.token-lifetime=2m",
                        "smbtech.rest-clients.authentication.jwt-bearer.payments-api.custom-claims.tenant=payments",
                        "smbtech.rest-clients.authentication.jwt-bearer.payments-api.custom-claims.channel=backend",
                        "smbtech.rest-clients.authentication.jwt-bearer.payments-api.custom-claims.priority=7",
                        "smbtech.rest-clients.authentication.jwt-bearer.payments-api.custom-claims.audit=true")
                .run(
                        context -> {
                            String token =
                                    context.getBean(AccessTokenProvider.class)
                                            .getAccessToken("payments-api", "payments.write");

                            assertThat(token).isEqualTo("jwt-token-from-keystore");
                            assertThat(endpoint.formValues())
                                    .containsEntry(
                                            "grant_type",
                                            "urn:ietf:params:oauth:grant-type:jwt-bearer");
                            assertThat(endpoint.formValues())
                                    .containsEntry("scope", "payments.write");

                            String assertion = endpoint.formValues().get("assertion");
                            assertThat(assertion).isNotBlank();
                            assertThat(verify(assertion, keyStore)).isTrue();
                            assertThat(decodePayload(assertion))
                                    .contains("\"iss\":\"payments-issuer\"")
                                    .contains("\"sub\":\"payments-subject\"")
                                    .contains("\"aud\":\"https://auth.example/token\"")
                                    .contains("\"jti\":")
                                    .contains("\"iat\":")
                                    .contains("\"exp\":")
                                    .contains("\"tenant\":\"payments\"")
                                    .contains("\"channel\":\"backend\"")
                                    .contains("\"priority\":\"7\"")
                                    .contains("\"audit\":\"true\"");
                        });
    }

    @Test
    void createsSignedJwtBearerAssertionFromBase64KeyStore() throws Exception {
        Path keyStore = createKeyStore();
        String keyStoreBase64 = Base64.getEncoder().encodeToString(Files.readAllBytes(keyStore));
        TokenEndpoint endpoint = startTokenEndpoint();

        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(RestClientAutoConfiguration.class))
                .withBean(
                        ClientRegistrationRepository.class,
                        () -> jwtBearerRegistrationRepository(endpoint.url()))
                .withPropertyValues(
                        "smbtech.rest-clients.authentication.key-stores.auth-key.base64="
                                + keyStoreBase64,
                        "smbtech.rest-clients.authentication.key-stores.auth-key.type=PKCS12",
                        "smbtech.rest-clients.authentication.key-stores.auth-key.password-ref=keystore-password",
                        "smbtech.rest-clients.authentication.key-stores.auth-key.key-alias=auth",
                        "smbtech.rest-clients.authentication.key-stores.auth-key.key-password-ref=key-password",
                        "smbtech.rest-clients.authentication.credentials.keystore-password.base64="
                                + encoded("changeit"),
                        "smbtech.rest-clients.authentication.credentials.key-password.base64="
                                + encoded("changeit"),
                        "smbtech.rest-clients.authentication.jwt-bearer.payments-api.key-store-id=auth-key")
                .run(
                        context -> {
                            String token =
                                    context.getBean(AccessTokenProvider.class)
                                            .getAccessToken("payments-api", "payments.write");

                            String assertion = endpoint.formValues().get("assertion");
                            assertThat(token).isEqualTo("jwt-token-from-keystore");
                            assertThat(assertion).isNotBlank();
                            assertThat(verify(assertion, keyStore)).isTrue();
                        });
    }

    @Test
    void createsSignedJwtBearerAssertionFromJksWithDifferentStorePasswordAndKeyPassword()
            throws Exception {
        Path keyStore = createJksKeyStore();
        TokenEndpoint endpoint = startTokenEndpoint();

        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(RestClientAutoConfiguration.class))
                .withBean(
                        ClientRegistrationRepository.class,
                        () -> jwtBearerRegistrationRepository(endpoint.url()))
                .withPropertyValues(
                        "smbtech.rest-clients.authentication.key-stores.auth-key.location=file:"
                                + keyStore,
                        "smbtech.rest-clients.authentication.key-stores.auth-key.type=JKS",
                        "smbtech.rest-clients.authentication.key-stores.auth-key.password-ref=keystore-password",
                        "smbtech.rest-clients.authentication.key-stores.auth-key.key-alias=auth",
                        "smbtech.rest-clients.authentication.key-stores.auth-key.key-password-ref=key-password",
                        "smbtech.rest-clients.authentication.credentials.keystore-password.value=storepass",
                        "smbtech.rest-clients.authentication.credentials.key-password.value=keypass1",
                        "smbtech.rest-clients.authentication.jwt-bearer.payments-api.key-store-id=auth-key")
                .run(
                        context -> {
                            String token =
                                    context.getBean(AccessTokenProvider.class)
                                            .getAccessToken("payments-api", "payments.write");

                            String assertion = endpoint.formValues().get("assertion");
                            assertThat(token).isEqualTo("jwt-token-from-keystore");
                            assertThat(assertion).isNotBlank();
                            assertThat(verify(assertion, keyStore, "JKS", "storepass")).isTrue();
                        });
    }

    private Path createKeyStore() throws Exception {
        Path keyStore = tempDir.resolve("auth.p12");
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
                                keyStore.toString(),
                                "-storepass",
                                "changeit",
                                "-keypass",
                                "changeit",
                                "-dname",
                                "CN=JWT Test",
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
        return keyStore;
    }

    private Path createJksKeyStore() throws Exception {
        Path keyStore = tempDir.resolve("auth.jks");
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
                                "JKS",
                                "-keystore",
                                keyStore.toString(),
                                "-storepass",
                                "storepass",
                                "-keypass",
                                "keypass1",
                                "-dname",
                                "CN=JWT JKS Test",
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
        return keyStore;
    }

    private String executable(String name) {
        return System.getProperty("os.name", "").toLowerCase().contains("win")
                ? name + ".exe"
                : name;
    }

    private ClientRegistrationRepository jwtBearerRegistrationRepository(String tokenUri) {
        ClientRegistration registration =
                ClientRegistration.withRegistrationId("payments-api")
                        .tokenUri(tokenUri)
                        .clientId("payments-client")
                        .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                        .authorizationGrantType(
                                new AuthorizationGrantType(
                                        "urn:ietf:params:oauth:grant-type:jwt-bearer"))
                        .scope("payments.write")
                        .build();
        return new InMemoryClientRegistrationRepository(registration);
    }

    private TokenEndpoint startTokenEndpoint() throws IOException {
        AtomicReference<Map<String, String>> form = new AtomicReference<>(Map.of());
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext(
                "/token",
                exchange -> {
                    form.set(readForm(exchange));
                    byte[] response =
                            """
                    {"access_token":"jwt-token-from-keystore","token_type":"Bearer","expires_in":3600,"scope":"payments.write"}
                    """
                                    .getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, response.length);
                    exchange.getResponseBody().write(response);
                    exchange.close();
                });
        server.setExecutor(Executors.newSingleThreadExecutor());
        server.start();

        return new TokenEndpoint(
                "http://localhost:" + server.getAddress().getPort() + "/token", form);
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

    private boolean verify(String assertion, Path keyStorePath) throws Exception {
        return verify(assertion, keyStorePath, "PKCS12", "changeit");
    }

    private boolean verify(
            String assertion, Path keyStorePath, String keyStoreType, String storePassword)
            throws Exception {
        String[] parts = assertion.split("\\.");
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initVerify(certificate(keyStorePath, keyStoreType, storePassword).getPublicKey());
        signature.update((parts[0] + "." + parts[1]).getBytes(StandardCharsets.UTF_8));
        return signature.verify(Base64.getUrlDecoder().decode(parts[2]));
    }

    private Certificate certificate(Path keyStorePath, String keyStoreType, String storePassword)
            throws Exception {
        KeyStore keyStore = KeyStore.getInstance(keyStoreType);
        try (var inputStream = java.nio.file.Files.newInputStream(keyStorePath)) {
            keyStore.load(inputStream, storePassword.toCharArray());
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

    private record TokenEndpoint(String url, AtomicReference<Map<String, String>> form) {
        public Map<String, String> formValues() {
            return form.get();
        }
    }
}
