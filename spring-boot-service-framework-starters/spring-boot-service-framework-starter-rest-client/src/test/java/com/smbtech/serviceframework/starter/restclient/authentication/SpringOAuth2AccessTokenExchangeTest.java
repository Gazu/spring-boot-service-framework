package com.smbtech.serviceframework.starter.restclient.authentication;

import com.smbtech.serviceframework.httpclient.domain.AccessToken;
import com.smbtech.serviceframework.httpclient.exception.AuthenticationException;
import com.smbtech.serviceframework.httpclient.port.out.AccessTokenProvider;
import com.smbtech.serviceframework.starter.restclient.api.AccessTokenClient;
import com.smbtech.serviceframework.starter.restclient.api.RestClientRegistry;
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
import java.util.function.IntFunction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SpringOAuth2AccessTokenExchangeTest {

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
                    assertThat(second).isEqualTo(first);
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
                    assertThat(second).isEqualTo(first);
                    assertThat(endpoint.requestCount()).isEqualTo(1);
                    assertThat(endpoint.authorizationHeader()).isEqualTo("Basic ZGVtby1jbGllbnQ6ZGVtby1zZWNyZXQ=");
                    assertThat(endpoint.formValues()).containsEntry("grant_type", "client_credentials");
                    assertThat(endpoint.formValues()).containsEntry("scope", "customer.read customer.write");
                });
    }

    @Test
    void doesNotCacheClientCredentialsTokenWhenDisabled() throws Exception {
        TokenEndpoint endpoint = startTokenEndpoint(requestNumber -> """
                {"access_token":"client-credentials-token-%d","token_type":"Bearer","expires_in":3600,"scope":"customer.read customer.write"}
                """.formatted(requestNumber));

        springClientCredentialsContextRunner(endpoint.url())
                .withPropertyValues("smbtech.rest-clients.authentication.token-cache.client-credentials=false")
                .run(context -> {
                    AccessTokenClient client = context.getBean(AccessTokenClient.class);

                    AccessToken first = client.clientCredentials("customer-api", "customer.read");
                    AccessToken second = client.clientCredentials("customer-api", "customer.read");

                    assertThat(first.value()).isEqualTo("client-credentials-token-1");
                    assertThat(second.value()).isEqualTo("client-credentials-token-2");
                    assertThat(endpoint.requestCount()).isEqualTo(2);
                    assertThat(endpoint.formValues()).containsEntry("grant_type", "client_credentials");
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
    void configuredRestClientUsesSpringSecurityOAuth2Interceptor() throws Exception {
        ProtectedResourceEndpoint endpoint = startProtectedResourceEndpoint("""
                {"access_token":"resource-token-123","token_type":"Bearer","expires_in":3600,"scope":"customer.read customer.write"}
                """);

        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(RestClientAutoConfiguration.class))
                .withBean(ClientRegistrationRepository.class, () -> clientRegistrationRepository(endpoint.tokenUrl()))
                .withPropertyValues(
                        "smbtech.rest-clients.clients.customer.base-url=" + endpoint.baseUrl(),
                        "smbtech.rest-clients.clients.customer.authentication-type=CLIENT_CREDENTIALS",
                        "smbtech.rest-clients.clients.customer.credential-token-requestor-id=customer-api",
                        "smbtech.rest-clients.clients.customer.scopes=customer.read"
                )
                .run(context -> {
                    RestClientRegistry registry = context.getBean(RestClientRegistry.class);

                    String body = registry.get("customer")
                            .get()
                            .uri("/resource")
                            .retrieve()
                            .body(String.class);

                    assertThat(body).isEqualTo("protected-ok");
                    assertThat(endpoint.resourceAuthorizationHeader()).isEqualTo("Bearer resource-token-123");
                    assertThat(endpoint.requestCount()).isEqualTo(1);
                    assertThat(endpoint.formValues()).containsEntry("grant_type", "client_credentials");
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
    void obtainsJwtBearerGrantTokenWithPrivateKeyJwtClientAuthentication() throws Exception {
        Path keyStore = createKeyStore();
        TokenEndpoint endpoint = startTokenEndpoint("""
                {"access_token":"jwt-bearer-private-key-jwt-token-123","token_type":"Bearer","expires_in":3600,"scope":"payments.write payments.read"}
                """);

        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(RestClientAutoConfiguration.class))
                .withBean(
                        ClientRegistrationRepository.class,
                        () -> privateKeyJwtBearerClientRegistrationRepository(endpoint.url())
                )
                .withPropertyValues(
                        "smbtech.rest-clients.clients.payments.base-url=https://payments.example",
                        "smbtech.rest-clients.clients.payments.authentication-type=CLIENT_CREDENTIALS",
                        "smbtech.rest-clients.clients.payments.credential-token-requestor-id=payments-api",
                        "smbtech.rest-clients.clients.payments.scopes=payments.write",
                        "smbtech.rest-clients.authentication.client-assertions.payments-api.key-store-id=auth-key",
                        "smbtech.rest-clients.authentication.client-assertions.payments-api.token-lifetime=75s",
                        "smbtech.rest-clients.authentication.client-assertions.payments-api.custom-claims.client-auth=true",
                        "smbtech.rest-clients.authentication.jwt-bearer.payments-api.key-store-id=auth-key",
                        "smbtech.rest-clients.authentication.jwt-bearer.payments-api.issuer=payments-issuer",
                        "smbtech.rest-clients.authentication.jwt-bearer.payments-api.subject=payments-subject",
                        "smbtech.rest-clients.authentication.jwt-bearer.payments-api.audience=https://auth.example/token",
                        "smbtech.rest-clients.authentication.jwt-bearer.payments-api.token-lifetime=2m",
                        "smbtech.rest-clients.authentication.jwt-bearer.payments-api.custom-claims.tenant=payments",
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

                    String token = provider.getAccessToken("payments-api", "payments.write");

                    assertThat(token).isEqualTo("jwt-bearer-private-key-jwt-token-123");
                    assertThat(endpoint.authorizationHeader()).isNull();
                    assertThat(endpoint.formValues()).containsEntry(
                            "grant_type",
                            "urn:ietf:params:oauth:grant-type:jwt-bearer"
                    );
                    assertThat(endpoint.formValues()).containsEntry("client_id", "payments-client");
                    assertThat(endpoint.formValues()).containsEntry(
                            "client_assertion_type",
                            "urn:ietf:params:oauth:client-assertion-type:jwt-bearer"
                    );

                    String grantAssertion = endpoint.formValues().get("assertion");
                    String clientAssertion = endpoint.formValues().get("client_assertion");
                    assertThat(grantAssertion).isNotBlank();
                    assertThat(clientAssertion).isNotBlank();
                    assertThat(clientAssertion).isNotEqualTo(grantAssertion);
                    assertThat(verify(grantAssertion, keyStore)).isTrue();
                    assertThat(verify(clientAssertion, keyStore)).isTrue();
                    assertThat(decodePayload(grantAssertion))
                            .contains("\"iss\":\"payments-issuer\"")
                            .contains("\"sub\":\"payments-subject\"")
                            .contains("\"aud\":\"https://auth.example/token\"")
                            .contains("\"tenant\":\"payments\"");
                    assertThat(decodePayload(clientAssertion))
                            .contains("\"iss\":\"payments-client\"")
                            .contains("\"sub\":\"payments-client\"")
                            .contains("\"aud\":\"" + endpoint.url() + "\"")
                            .contains("\"client-auth\":\"true\"");
                });
    }

    @Test
    void obtainsJwtBearerGrantTokenWithDynamicCustomClaims() throws Exception {
        Path keyStore = createKeyStore();
        TokenEndpoint endpoint = startTokenEndpoint("""
                {"access_token":"spring-jwt-bearer-token-456","token_type":"Bearer","expires_in":3600,"scope":"payments.write payments.read"}
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
                    AccessTokenClient client = context.getBean(AccessTokenClient.class);
                    Map<String, Object> dynamicClaims = new LinkedHashMap<>();
                    dynamicClaims.put("customer_id", "17952397-3");
                    dynamicClaims.put("channel", "mobile");
                    dynamicClaims.put("iss", "must-not-override-issuer");
                    dynamicClaims.put("ignored", null);

                    AccessToken token = client.jwtBearer(
                            "payments-api",
                            "payments.write",
                            dynamicClaims
                    );

                    assertThat(token.value()).isEqualTo("spring-jwt-bearer-token-456");
                    String assertion = endpoint.formValues().get("assertion");
                    assertThat(assertion).isNotBlank();
                    assertThat(verify(assertion, keyStore)).isTrue();
                    assertThat(decodePayload(assertion))
                            .contains("\"iss\":\"payments-issuer\"")
                            .contains("\"sub\":\"payments-subject\"")
                            .contains("\"aud\":\"https://auth.example/token\"")
                            .contains("\"channel\":\"mobile\"")
                            .contains("\"customer_id\":\"17952397-3\"")
                            .doesNotContain("backend")
                            .doesNotContain("must-not-override-issuer")
                            .doesNotContain("ignored");
                });
    }

    @Test
    void cachesJwtBearerGrantTokensByResolvedDynamicClaims() throws Exception {
        Path keyStore = createKeyStore();
        TokenEndpoint endpoint = startTokenEndpoint(requestNumber -> """
                {"access_token":"spring-jwt-bearer-token-%d","token_type":"Bearer","expires_in":3600,"scope":"payments.write payments.read"}
                """.formatted(requestNumber));

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
                        "smbtech.rest-clients.authentication.key-stores.auth-key.location=file:" + keyStore,
                        "smbtech.rest-clients.authentication.key-stores.auth-key.type=PKCS12",
                        "smbtech.rest-clients.authentication.key-stores.auth-key.password-ref=keystore-password",
                        "smbtech.rest-clients.authentication.key-stores.auth-key.key-alias=auth",
                        "smbtech.rest-clients.authentication.key-stores.auth-key.key-password-ref=key-password",
                        "smbtech.rest-clients.authentication.credentials.keystore-password.base64=" + encoded("changeit"),
                        "smbtech.rest-clients.authentication.credentials.key-password.base64=" + encoded("changeit")
                )
                .run(context -> {
                    AccessTokenClient client = context.getBean(AccessTokenClient.class);
                    Map<String, Object> firstClaims = Map.of("a", "b&c=d");
                    Map<String, Object> secondClaims = new LinkedHashMap<>();
                    secondClaims.put("a", "b");
                    secondClaims.put("c", "d");

                    AccessToken first = client.jwtBearer("payments-api", "payments.write", firstClaims);
                    AccessToken second = client.jwtBearer("payments-api", "payments.write", secondClaims);
                    AccessToken firstAgain = client.jwtBearer("payments-api", "payments.write", firstClaims);

                    assertThat(first.value()).isEqualTo("spring-jwt-bearer-token-1");
                    assertThat(second.value()).isEqualTo("spring-jwt-bearer-token-2");
                    assertThat(firstAgain.value()).isEqualTo("spring-jwt-bearer-token-1");
                    assertThat(endpoint.requestCount()).isEqualTo(2);
                });
    }

    @Test
    void doesNotCacheJwtBearerGrantTokensWhenDisabled() throws Exception {
        Path keyStore = createKeyStore();
        TokenEndpoint endpoint = startTokenEndpoint(requestNumber -> """
                {"access_token":"spring-jwt-bearer-token-%d","token_type":"Bearer","expires_in":3600,"scope":"payments.write payments.read"}
                """.formatted(requestNumber));

        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(RestClientAutoConfiguration.class))
                .withBean(ClientRegistrationRepository.class, () -> jwtBearerClientRegistrationRepository(endpoint.url()))
                .withPropertyValues(
                        "smbtech.rest-clients.clients.payments.base-url=https://payments.example",
                        "smbtech.rest-clients.clients.payments.authentication-type=CLIENT_CREDENTIALS",
                        "smbtech.rest-clients.clients.payments.credential-token-requestor-id=payments-api",
                        "smbtech.rest-clients.clients.payments.scopes=payments.write",
                        "smbtech.rest-clients.authentication.token-cache.jwt-bearer=false",
                        "smbtech.rest-clients.authentication.jwt-bearer.payments-api.key-store-id=auth-key",
                        "smbtech.rest-clients.authentication.jwt-bearer.payments-api.issuer=payments-issuer",
                        "smbtech.rest-clients.authentication.jwt-bearer.payments-api.subject=payments-subject",
                        "smbtech.rest-clients.authentication.jwt-bearer.payments-api.audience=https://auth.example/token",
                        "smbtech.rest-clients.authentication.jwt-bearer.payments-api.token-lifetime=2m",
                        "smbtech.rest-clients.authentication.key-stores.auth-key.location=file:" + keyStore,
                        "smbtech.rest-clients.authentication.key-stores.auth-key.type=PKCS12",
                        "smbtech.rest-clients.authentication.key-stores.auth-key.password-ref=keystore-password",
                        "smbtech.rest-clients.authentication.key-stores.auth-key.key-alias=auth",
                        "smbtech.rest-clients.authentication.key-stores.auth-key.key-password-ref=key-password",
                        "smbtech.rest-clients.authentication.credentials.keystore-password.base64=" + encoded("changeit"),
                        "smbtech.rest-clients.authentication.credentials.key-password.base64=" + encoded("changeit")
                )
                .run(context -> {
                    AccessTokenClient client = context.getBean(AccessTokenClient.class);
                    Map<String, Object> dynamicClaims = Map.of("customer_id", "17952397-3");

                    AccessToken first = client.jwtBearer("payments-api", "payments.write", dynamicClaims);
                    AccessToken second = client.jwtBearer("payments-api", "payments.write", dynamicClaims);

                    assertThat(first.value()).isEqualTo("spring-jwt-bearer-token-1");
                    assertThat(second.value()).isEqualTo("spring-jwt-bearer-token-2");
                    assertThat(endpoint.requestCount()).isEqualTo(2);
                    assertThat(endpoint.formValues()).containsEntry(
                            "grant_type",
                            "urn:ietf:params:oauth:grant-type:jwt-bearer"
                    );
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

    private ClientRegistrationRepository privateKeyJwtBearerClientRegistrationRepository(String tokenUri) {
        ClientRegistration paymentsApi = ClientRegistration
                .withRegistrationId("payments-api")
                .tokenUri(tokenUri)
                .clientId("payments-client")
                .clientAuthenticationMethod(ClientAuthenticationMethod.PRIVATE_KEY_JWT)
                .authorizationGrantType(new AuthorizationGrantType("urn:ietf:params:oauth:grant-type:jwt-bearer"))
                .scope("payments.write", "payments.read")
                .build();
        return new InMemoryClientRegistrationRepository(paymentsApi);
    }

    private TokenEndpoint startTokenEndpoint(String responseBody) throws IOException {
        return startTokenEndpoint(ignored -> responseBody);
    }

    private TokenEndpoint startTokenEndpoint(IntFunction<String> responseBody) throws IOException {
        AtomicInteger requests = new AtomicInteger();
        AtomicReference<String> authorization = new AtomicReference<>();
        AtomicReference<Map<String, String>> form = new AtomicReference<>(Map.of());

        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/token", exchange -> {
            int requestNumber = requests.incrementAndGet();
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            form.set(readForm(exchange));
            byte[] response = responseBody.apply(requestNumber).getBytes(StandardCharsets.UTF_8);
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

    private ProtectedResourceEndpoint startProtectedResourceEndpoint(String tokenResponseBody) throws IOException {
        AtomicInteger requests = new AtomicInteger();
        AtomicReference<String> authorization = new AtomicReference<>();
        AtomicReference<Map<String, String>> form = new AtomicReference<>(Map.of());
        AtomicReference<String> resourceAuthorization = new AtomicReference<>();

        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/token", exchange -> {
            requests.incrementAndGet();
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            form.set(readForm(exchange));
            byte[] response = tokenResponseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.createContext("/resource", exchange -> {
            resourceAuthorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] response = "protected-ok".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.setExecutor(Executors.newSingleThreadExecutor());
        server.start();

        String baseUrl = "http://localhost:" + server.getAddress().getPort();
        return new ProtectedResourceEndpoint(
                baseUrl,
                baseUrl + "/token",
                requests,
                authorization,
                form,
                resourceAuthorization
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

    private record ProtectedResourceEndpoint(
            String baseUrl,
            String tokenUrl,
            AtomicInteger requests,
            AtomicReference<String> authorization,
            AtomicReference<Map<String, String>> form,
            AtomicReference<String> resourceAuthorization
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

        public String resourceAuthorizationHeader() {
            return resourceAuthorization.get();
        }
    }
}
