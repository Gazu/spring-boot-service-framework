package com.smbtech.serviceframework.starter.restclient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.smbtech.serviceframework.httpclient.port.in.HttpClientCatalog;
import com.smbtech.serviceframework.httpclient.port.out.AccessTokenProvider;
import com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring.ClientAssertionJwkResolver;
import com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring.GrantAwareOAuth2AuthorizedClientService;
import com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring.OAuth2RestClientConfigurationValidationRunner;
import com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring.OAuth2RestClientConfigurationValidator;
import com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring.OAuth2TokenDiagnosticsLogger;
import com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring.SpringClientRegistrationResolver;
import com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring.SpringOAuth2TokenResponseClientFactory;
import com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring.SpringSecurityAuthorizedClientTokenClient;
import com.smbtech.serviceframework.starter.restclient.adapter.out.interceptor.BasicAuthenticationInterceptor;
import com.smbtech.serviceframework.starter.restclient.adapter.out.interceptor.CorrelationHeadersInterceptor;
import com.smbtech.serviceframework.starter.restclient.adapter.out.interceptor.DefaultHeadersInterceptor;
import com.smbtech.serviceframework.starter.restclient.api.AccessTokenClient;
import com.smbtech.serviceframework.starter.restclient.api.ApiClientFactory;
import com.smbtech.serviceframework.starter.restclient.api.HttpErrorBodyDecoder;
import com.smbtech.serviceframework.starter.restclient.api.RequestContextManager;
import com.smbtech.serviceframework.starter.restclient.api.RequestContextScope;
import com.smbtech.serviceframework.starter.restclient.api.RestClientRegistry;
import com.smbtech.serviceframework.starter.restclient.autoconfigure.OAuth2RestClientAuthenticationAutoConfiguration;
import com.smbtech.serviceframework.starter.restclient.autoconfigure.OAuth2RestClientAutoConfiguration;
import com.smbtech.serviceframework.starter.restclient.autoconfigure.RestClientAutoConfiguration;
import com.smbtech.serviceframework.starter.restclient.autoconfigure.RestClientProperties;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.web.client.RestClient;

class RestClientAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withConfiguration(
                            AutoConfigurations.of(
                                    RestClientAutoConfiguration.class,
                                    OAuth2RestClientAutoConfiguration.class,
                                    OAuth2RestClientAuthenticationAutoConfiguration.class))
                    .withPropertyValues(
                            "smbtech.rest-clients.clients.projects.base-url=https://projects.example",
                            "smbtech.rest-clients.clients.projects.default-headers.X-Application-Name=test-service",
                            "smbtech.rest-clients.clients.secure.base-url=https://secure.example",
                            "smbtech.rest-clients.clients.secure.authentication-type=BASIC_AUTH",
                            "smbtech.rest-clients.clients.secure.basic-authentication.username-ref=secure-username",
                            "smbtech.rest-clients.clients.secure.basic-authentication.password-ref=secure-password",
                            "smbtech.rest-clients.authentication.credentials.secure-username.value=demo",
                            "smbtech.rest-clients.authentication.credentials.secure-password.value=secret");

    @Test
    void createsRegistryAndDynamicRestClientBeansFromProperties() {
        contextRunner.run(
                context -> {
                    assertThat(context).hasSingleBean(RestClientRegistry.class);
                    assertThat(context).hasSingleBean(ApiClientFactory.class);
                    assertThat(context).hasSingleBean(AccessTokenClient.class);
                    assertThat(context).hasSingleBean(HttpErrorBodyDecoder.class);
                    assertThat(context).hasSingleBean(RequestContextManager.class);
                    assertThat(context).hasSingleBean(OAuth2TokenDiagnosticsLogger.class);
                    assertThat(context).hasSingleBean(OAuth2RestClientConfigurationValidator.class);
                    assertThat(context)
                            .hasSingleBean(OAuth2RestClientConfigurationValidationRunner.class);
                    assertThat(context).hasBean("projectsRestClient");
                    assertThat(context).hasBean("secureRestClient");

                    RestClientRegistry registry = context.getBean(RestClientRegistry.class);
                    assertThat(registry.names()).containsExactlyInAnyOrder("projects", "secure");
                });
    }

    @Test
    void bindsOAuth2TokenDiagnosticsProperties() {
        contextRunner
                .withPropertyValues(
                        "smbtech.rest-clients.authentication.diagnostics.enabled=true",
                        "smbtech.rest-clients.authentication.diagnostics.include-claims=true",
                        "smbtech.rest-clients.authentication.diagnostics.include-token-preview=true",
                        "smbtech.rest-clients.authentication.diagnostics.token-preview-length=16")
                .run(
                        context -> {
                            RestClientProperties.Diagnostics diagnostics =
                                    context.getBean(RestClientProperties.class)
                                            .getAuthentication()
                                            .getDiagnostics();

                            assertThat(diagnostics.isEnabled()).isTrue();
                            assertThat(diagnostics.isIncludeClaims()).isTrue();
                            assertThat(diagnostics.isIncludeTokenPreview()).isTrue();
                            assertThat(diagnostics.getTokenPreviewLength()).isEqualTo(16);
                        });
    }

    @Test
    void bindsOAuth2ConfigurationValidationProperties() {
        contextRunner
                .withPropertyValues(
                        "smbtech.rest-clients.validation.enabled=false",
                        "smbtech.rest-clients.validation.fail-on-warnings=true",
                        "smbtech.rest-clients.validation.validate-key-store-content=true")
                .run(
                        context -> {
                            RestClientProperties.Validation validation =
                                    context.getBean(RestClientProperties.class).getValidation();

                            assertThat(validation.isEnabled()).isFalse();
                            assertThat(validation.isFailOnWarnings()).isTrue();
                            assertThat(validation.isValidateKeyStoreContent()).isTrue();
                        });
    }

    @Test
    void usesSafeDefaultsForOAuth2ConfigurationValidationProperties() {
        contextRunner.run(
                context -> {
                    RestClientProperties.Validation validation =
                            context.getBean(RestClientProperties.class).getValidation();

                    assertThat(validation.isEnabled()).isTrue();
                    assertThat(validation.isFailOnWarnings()).isFalse();
                    assertThat(validation.isValidateKeyStoreContent()).isFalse();
                });
    }

    @Test
    void bindsRequestContextPropagationProperties() {
        contextRunner
                .withPropertyValues(
                        "smbtech.rest-clients.request-context.enabled=false",
                        "smbtech.rest-clients.request-context.headers=false",
                        "smbtech.rest-clients.request-context.jwt-bearer-claims=false",
                        "smbtech.rest-clients.request-context.blocked-headers[0]=X-Internal-Secret",
                        "smbtech.rest-clients.request-context.blocked-jwt-bearer-claims[0]=customer_secret")
                .run(
                        context -> {
                            RestClientProperties.RequestContext requestContext =
                                    context.getBean(RestClientProperties.class).getRequestContext();

                            assertThat(requestContext.isEnabled()).isFalse();
                            assertThat(requestContext.isHeaders()).isFalse();
                            assertThat(requestContext.isJwtBearerClaims()).isFalse();
                            assertThat(requestContext.getBlockedHeaders())
                                    .containsExactly("X-Internal-Secret");
                            assertThat(requestContext.getBlockedJwtBearerClaims())
                                    .containsExactly("customer_secret");
                        });
    }

    @Test
    void usesSafeDefaultsForRequestContextPropagationProperties() {
        contextRunner.run(
                context -> {
                    RestClientProperties.RequestContext requestContext =
                            context.getBean(RestClientProperties.class).getRequestContext();

                    assertThat(requestContext.isEnabled()).isTrue();
                    assertThat(requestContext.isHeaders()).isTrue();
                    assertThat(requestContext.isJwtBearerClaims()).isTrue();
                });
    }

    @Test
    void failsStartupWhenOAuth2ClientReferencesRegistrationButRepositoryIsMissing() {
        new ApplicationContextRunner()
                .withConfiguration(
                        AutoConfigurations.of(
                                RestClientAutoConfiguration.class,
                                OAuth2RestClientAutoConfiguration.class,
                                OAuth2RestClientAuthenticationAutoConfiguration.class))
                .withPropertyValues(
                        "smbtech.rest-clients.clients.payments.base-url=https://payments.example",
                        "smbtech.rest-clients.clients.payments.authentication-type=CLIENT_CREDENTIALS",
                        "smbtech.rest-clients.clients.payments.token-request-id=payments-token")
                .run(
                        context -> {
                            assertThat(context).hasFailed();
                            assertThat(context.getStartupFailure())
                                    .hasMessageContaining(
                                            "Invalid SMBTech REST client OAuth2 configuration")
                                    .hasMessageContaining("clients.payments.token-request-id")
                                    .hasMessageContaining(
                                            "no ClientRegistrationRepository is available");
                        });
    }

    @Test
    void rejectsRemovedLegacyTokenRequestProperty() {
        new ApplicationContextRunner()
                .withConfiguration(
                        AutoConfigurations.of(
                                RestClientAutoConfiguration.class,
                                OAuth2RestClientAutoConfiguration.class,
                                OAuth2RestClientAuthenticationAutoConfiguration.class))
                .withPropertyValues(
                        "smbtech.rest-clients.clients.payments.base-url=https://payments.example",
                        "smbtech.rest-clients.clients.payments.authentication-type=CLIENT_CREDENTIALS",
                        "smbtech.rest-clients.clients.payments.credential-token-requestor-id=payments-token")
                .run(
                        context -> {
                            assertThat(context).hasFailed();
                            assertThat(context.getStartupFailure())
                                    .hasMessageContaining(
                                            "tokenRequestId is required for HTTP client: payments");
                        });
    }

    @Test
    void skipsStartupOAuth2ConfigurationValidationWhenDisabled() {
        new ApplicationContextRunner()
                .withConfiguration(
                        AutoConfigurations.of(
                                RestClientAutoConfiguration.class,
                                OAuth2RestClientAutoConfiguration.class,
                                OAuth2RestClientAuthenticationAutoConfiguration.class))
                .withPropertyValues(
                        "smbtech.rest-clients.validation.enabled=false",
                        "smbtech.rest-clients.clients.payments.base-url=https://payments.example",
                        "smbtech.rest-clients.clients.payments.authentication-type=CLIENT_CREDENTIALS",
                        "smbtech.rest-clients.clients.payments.token-request-id=payments-token")
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void keepsStartupRunningWhenOAuth2ValidationFindsWarningsOnlyByDefault() {
        contextRunner
                .withPropertyValues(
                        "smbtech.rest-clients.authentication.jwt-bearer.unused-token.key-store-id=unused-signing-key")
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void failsStartupWhenOAuth2ValidationFindsWarningsAndFailOnWarningsIsEnabled() {
        contextRunner
                .withPropertyValues(
                        "smbtech.rest-clients.validation.fail-on-warnings=true",
                        "smbtech.rest-clients.authentication.jwt-bearer.unused-token.key-store-id=unused-signing-key")
                .run(
                        context -> {
                            assertThat(context).hasFailed();
                            assertThat(context.getStartupFailure())
                                    .hasMessageContaining(
                                            "Invalid SMBTech REST client OAuth2 configuration")
                                    .hasMessageContaining("Warnings")
                                    .hasMessageContaining("authentication.jwt-bearer.unused-token");
                        });
    }

    @Test
    void configuredRestClientBeanCanBeResolvedFromContext() {
        contextRunner.run(
                context -> {
                    RestClient restClient = context.getBean("secureRestClient", RestClient.class);
                    assertThat(restClient).isNotNull();
                });
    }

    @Test
    void configuredRestClientAddsRequestContextHeaders() throws Exception {
        HttpServer server = startHeaderCaptureServer();
        AtomicReference<com.sun.net.httpserver.Headers> receivedHeaders = new AtomicReference<>();
        server.createContext(
                "/dummy",
                exchange -> {
                    receivedHeaders.set(exchange.getRequestHeaders());
                    byte[] response = "ok".getBytes(StandardCharsets.UTF_8);
                    exchange.sendResponseHeaders(200, response.length);
                    exchange.getResponseBody().write(response);
                    exchange.close();
                });

        try {
            contextRunner
                    .withPropertyValues(
                            "smbtech.rest-clients.clients.projects.base-url=http://localhost:"
                                    + server.getAddress().getPort())
                    .run(
                            context -> {
                                RequestContextManager requestContextManager =
                                        context.getBean(RequestContextManager.class);
                                RestClient projects =
                                        context.getBean(RestClientRegistry.class).get("projects");

                                try (RequestContextScope ignored =
                                        requestContextManager.open(
                                                contextBuilder ->
                                                        contextBuilder
                                                                .header(
                                                                        "X-Customer-Id",
                                                                        "17952397-3")
                                                                .header("X-Channel", "mobile"))) {
                                    assertThat(
                                                    projects.get()
                                                            .uri("/dummy")
                                                            .retrieve()
                                                            .body(String.class))
                                            .isEqualTo("ok");
                                }

                                assertThat(receivedHeaders.get().getFirst("X-Application-Name"))
                                        .isEqualTo("test-service");
                                assertThat(receivedHeaders.get().getFirst("X-Customer-Id"))
                                        .isEqualTo("17952397-3");
                                assertThat(receivedHeaders.get().getFirst("X-Channel"))
                                        .isEqualTo("mobile");
                            });
        } finally {
            server.stop(0);
        }
    }

    @Test
    void configuredRestClientSkipsRequestContextHeadersWhenDisabled() throws Exception {
        HttpServer server = startHeaderCaptureServer();
        AtomicReference<com.sun.net.httpserver.Headers> receivedHeaders = new AtomicReference<>();
        server.createContext(
                "/dummy",
                exchange -> {
                    receivedHeaders.set(exchange.getRequestHeaders());
                    byte[] response = "ok".getBytes(StandardCharsets.UTF_8);
                    exchange.sendResponseHeaders(200, response.length);
                    exchange.getResponseBody().write(response);
                    exchange.close();
                });

        try {
            contextRunner
                    .withPropertyValues(
                            "smbtech.rest-clients.request-context.headers=false",
                            "smbtech.rest-clients.clients.projects.base-url=http://localhost:"
                                    + server.getAddress().getPort())
                    .run(
                            context -> {
                                RequestContextManager requestContextManager =
                                        context.getBean(RequestContextManager.class);
                                RestClient projects =
                                        context.getBean(RestClientRegistry.class).get("projects");

                                try (RequestContextScope ignored =
                                        requestContextManager.openHeader(
                                                "X-Customer-Id", "17952397-3")) {
                                    assertThat(
                                                    projects.get()
                                                            .uri("/dummy")
                                                            .retrieve()
                                                            .body(String.class))
                                            .isEqualTo("ok");
                                }

                                assertThat(receivedHeaders.get().getFirst("X-Application-Name"))
                                        .isEqualTo("test-service");
                                assertThat(receivedHeaders.get().getFirst("X-Customer-Id"))
                                        .isNull();
                            });
        } finally {
            server.stop(0);
        }
    }

    @Test
    void configuredRestClientSanitizesRequestContextHeaders() throws Exception {
        HttpServer server = startHeaderCaptureServer();
        AtomicReference<com.sun.net.httpserver.Headers> receivedHeaders = new AtomicReference<>();
        server.createContext(
                "/dummy",
                exchange -> {
                    receivedHeaders.set(exchange.getRequestHeaders());
                    byte[] response = "ok".getBytes(StandardCharsets.UTF_8);
                    exchange.sendResponseHeaders(200, response.length);
                    exchange.getResponseBody().write(response);
                    exchange.close();
                });

        try {
            contextRunner
                    .withPropertyValues(
                            "smbtech.rest-clients.request-context.blocked-headers[0]=X-Internal-Secret",
                            "smbtech.rest-clients.clients.projects.base-url=http://localhost:"
                                    + server.getAddress().getPort())
                    .run(
                            context -> {
                                RequestContextManager requestContextManager =
                                        context.getBean(RequestContextManager.class);
                                RestClient projects =
                                        context.getBean(RestClientRegistry.class).get("projects");

                                try (RequestContextScope ignored =
                                        requestContextManager.open(
                                                contextBuilder ->
                                                        contextBuilder
                                                                .header(
                                                                        "Authorization",
                                                                        "Bearer malicious")
                                                                .header("Cookie", "session=abc")
                                                                .header(
                                                                        "X-Internal-Secret",
                                                                        "secret")
                                                                .header(
                                                                        "X-Injected",
                                                                        "safe\r\nX-Other: injected")
                                                                .header("X-Channel", "mobile"))) {
                                    assertThat(
                                                    projects.get()
                                                            .uri("/dummy")
                                                            .retrieve()
                                                            .body(String.class))
                                            .isEqualTo("ok");
                                }

                                assertThat(receivedHeaders.get().getFirst("Authorization"))
                                        .isNull();
                                assertThat(receivedHeaders.get().getFirst("Cookie")).isNull();
                                assertThat(receivedHeaders.get().getFirst("X-Internal-Secret"))
                                        .isNull();
                                assertThat(receivedHeaders.get().getFirst("X-Injected")).isNull();
                                assertThat(receivedHeaders.get().getFirst("X-Channel"))
                                        .isEqualTo("mobile");
                            });
        } finally {
            server.stop(0);
        }
    }

    @Test
    void decodesBase64CredentialsReferencedByBasicAuthenticationProperties() throws Exception {
        contextRunner
                .withPropertyValues(
                        "smbtech.rest-clients.authentication.credentials.secure-username.base64="
                                + basicValue("api-user"),
                        "smbtech.rest-clients.authentication.credentials.secure-password.base64="
                                + basicValue("api-secret"))
                .run(
                        context -> {
                            var definition =
                                    context.getBean(HttpClientCatalog.class)
                                            .requireByName("secure");

                            assertThat(definition.basicAuthentication().username())
                                    .isEqualTo("api-user");
                            assertThat(definition.basicAuthentication().password())
                                    .isEqualTo("api-secret");

                            MockClientHttpRequest request =
                                    new MockClientHttpRequest(
                                            HttpMethod.GET,
                                            URI.create("https://secure.example/ping"));
                            new BasicAuthenticationInterceptor(definition.basicAuthentication())
                                    .intercept(
                                            request,
                                            new byte[0],
                                            (httpRequest, body) ->
                                                    new MockClientHttpResponse(
                                                            "ok".getBytes(StandardCharsets.UTF_8),
                                                            200));

                            assertThat(request.getHeaders().getFirst("Authorization"))
                                    .isEqualTo("Basic " + basic("api-user", "api-secret"));
                        });
    }

    @Test
    void doesNotCreateSpringClientRegistrationResolverWhenRepositoryIsMissing() {
        contextRunner.run(
                context ->
                        assertThat(context)
                                .doesNotHaveBean(SpringClientRegistrationResolver.class));
    }

    @Test
    void doesNotBindSpringOAuth2PropertiesWithoutSpringBootOAuth2AutoConfiguration() {
        new ApplicationContextRunner()
                .withConfiguration(
                        AutoConfigurations.of(
                                RestClientAutoConfiguration.class,
                                OAuth2RestClientAutoConfiguration.class,
                                OAuth2RestClientAuthenticationAutoConfiguration.class))
                .withPropertyValues(
                        "spring.security.oauth2.client.provider.my-provider.token-uri=https://auth.example/oauth2/token",
                        "spring.security.oauth2.client.registration.payments-token.provider=my-provider",
                        "spring.security.oauth2.client.registration.payments-token.client-id=payments-client",
                        "spring.security.oauth2.client.registration.payments-token.client-authentication-method=private_key_jwt",
                        "spring.security.oauth2.client.registration.payments-token.authorization-grant-type=client_credentials",
                        "smbtech.rest-clients.validation.enabled=false",
                        "smbtech.rest-clients.clients.payments.base-url=https://payments.example",
                        "smbtech.rest-clients.clients.payments.authentication-type=CLIENT_CREDENTIALS",
                        "smbtech.rest-clients.clients.payments.token-request-id=payments-token")
                .run(
                        context -> {
                            assertThat(context).doesNotHaveBean(ClientRegistrationRepository.class);
                            assertThat(context)
                                    .doesNotHaveBean(SpringClientRegistrationResolver.class);
                            assertThat(context).doesNotHaveBean(ClientAssertionJwkResolver.class);
                            assertThat(context)
                                    .doesNotHaveBean(SpringOAuth2TokenResponseClientFactory.class);
                            assertThat(context)
                                    .doesNotHaveBean(OAuth2AuthorizedClientManager.class);
                        });
    }

    @Test
    void createsSpringClientRegistrationResolverWhenRepositoryExists() {
        contextRunner
                .withUserConfiguration(OAuth2ClientRegistrationConfiguration.class)
                .run(
                        context -> {
                            assertThat(context).hasSingleBean(ClientRegistrationRepository.class);
                            assertThat(context)
                                    .hasSingleBean(SpringClientRegistrationResolver.class);

                            SpringClientRegistrationResolver resolver =
                                    context.getBean(SpringClientRegistrationResolver.class);

                            ClientRegistration registration =
                                    resolver.requireByRegistrationId("payments-token");
                            assertThat(registration.getRegistrationId())
                                    .isEqualTo("payments-token");
                            assertThat(registration.getProviderDetails().getTokenUri())
                                    .isEqualTo("https://auth.example/oauth2/token");
                            assertThat(registration.getClientId()).isEqualTo("payments-client");
                            assertThat(registration.getAuthorizationGrantType())
                                    .isEqualTo(AuthorizationGrantType.CLIENT_CREDENTIALS);
                            assertThat(registration.getClientAuthenticationMethod())
                                    .isEqualTo(ClientAuthenticationMethod.CLIENT_SECRET_BASIC);
                            assertThat(registration.getScopes())
                                    .containsExactlyInAnyOrder("payments.read", "payments.write");

                            assertThat(resolver.findByRegistrationId("missing-token")).isEmpty();
                            assertThatThrownBy(
                                            () -> resolver.requireByRegistrationId("missing-token"))
                                    .isInstanceOf(
                                            com.smbtech.serviceframework.httpclient.exception
                                                    .HttpClientAuthenticationException.class)
                                    .hasMessageContaining(
                                            "OAuth2 client registration not found: missing-token");
                        });
    }

    @Test
    void
            createsSpringSecurityTokenProviderWhenSpringBootOAuth2AutoConfigurationCreatesRegistrationRepository() {
        new ApplicationContextRunner()
                .withConfiguration(
                        AutoConfigurations.of(
                                RestClientAutoConfiguration.class,
                                OAuth2RestClientAutoConfiguration.class,
                                OAuth2RestClientAuthenticationAutoConfiguration.class,
                                OAuth2ClientAutoConfiguration.class))
                .withPropertyValues(
                        "spring.security.oauth2.client.provider.my-provider.token-uri=https://auth.example/oauth2/token",
                        "spring.security.oauth2.client.provider.my-provider.jwk-set-uri=https://auth.example/oauth2/jwks",
                        "spring.security.oauth2.client.registration.payments-token.provider=my-provider",
                        "spring.security.oauth2.client.registration.payments-token.client-id=payments-client",
                        "spring.security.oauth2.client.registration.payments-token.client-authentication-method=private_key_jwt",
                        "spring.security.oauth2.client.registration.payments-token.authorization-grant-type=client_credentials",
                        "spring.security.oauth2.client.registration.payments-token.scope=cl:core:profile:read",
                        "smbtech.rest-clients.clients.payments.base-url=https://payments.example",
                        "smbtech.rest-clients.clients.payments.authentication-type=CLIENT_CREDENTIALS",
                        "smbtech.rest-clients.clients.payments.token-request-id=payments-token",
                        "smbtech.rest-clients.clients.payments.scopes=cl:core:profile:read",
                        "smbtech.rest-clients.authentication.client-assertions.payments-token.key-store-id=payments-signing-key",
                        "smbtech.rest-clients.authentication.client-assertions.payments-token.token-lifetime=60s")
                .run(
                        context -> {
                            assertThat(context).hasSingleBean(ClientRegistrationRepository.class);
                            assertThat(context)
                                    .hasSingleBean(SpringClientRegistrationResolver.class);
                            assertThat(context).hasSingleBean(ClientAssertionJwkResolver.class);
                            assertThat(context)
                                    .hasSingleBean(SpringOAuth2TokenResponseClientFactory.class);
                            assertThat(context).hasSingleBean(OAuth2AuthorizedClientManager.class);
                            assertThat(context).hasSingleBean(OAuth2AuthorizedClientService.class);
                            assertThat(context.getBean(OAuth2AuthorizedClientService.class))
                                    .isInstanceOf(GrantAwareOAuth2AuthorizedClientService.class);
                            assertThat(context)
                                    .hasSingleBean(SpringSecurityAuthorizedClientTokenClient.class);
                            assertThat(context).hasSingleBean(AccessTokenProvider.class);

                            SpringClientRegistrationResolver resolver =
                                    context.getBean(SpringClientRegistrationResolver.class);
                            ClientRegistration registration =
                                    resolver.requireByRegistrationId("payments-token");

                            assertThat(registration.getRegistrationId())
                                    .isEqualTo("payments-token");
                            assertThat(registration.getProviderDetails().getTokenUri())
                                    .isEqualTo("https://auth.example/oauth2/token");
                            assertThat(registration.getClientId()).isEqualTo("payments-client");
                            assertThat(registration.getAuthorizationGrantType())
                                    .isEqualTo(AuthorizationGrantType.CLIENT_CREDENTIALS);
                            assertThat(registration.getClientAuthenticationMethod())
                                    .isEqualTo(ClientAuthenticationMethod.PRIVATE_KEY_JWT);
                            assertThat(registration.getScopes())
                                    .containsExactly("cl:core:profile:read");
                        });
    }

    @Test
    void createsGrantAwareAuthorizedClientServiceWhenSpringBootDoesNotProvideOne() {
        contextRunner
                .withUserConfiguration(OAuth2ClientRegistrationConfiguration.class)
                .run(
                        context -> {
                            assertThat(context).hasSingleBean(OAuth2AuthorizedClientService.class);
                            assertThat(context.getBean(OAuth2AuthorizedClientService.class))
                                    .isInstanceOf(GrantAwareOAuth2AuthorizedClientService.class);
                        });
    }

    @Test
    void mapsOptionalResiliencePropertiesToHttpClientDefinition() {
        contextRunner
                .withPropertyValues(
                        "smbtech.rest-clients.clients.projects.resilience.retry.enabled=true",
                        "smbtech.rest-clients.clients.projects.resilience.retry.max-attempts=4",
                        "smbtech.rest-clients.clients.projects.resilience.retry.backoff=250ms",
                        "smbtech.rest-clients.clients.projects.resilience.retry.retry-on-statuses[0]=429",
                        "smbtech.rest-clients.clients.projects.resilience.circuit-breaker.enabled=true",
                        "smbtech.rest-clients.clients.projects.resilience.circuit-breaker.failure-threshold=5",
                        "smbtech.rest-clients.clients.projects.resilience.circuit-breaker.open-duration=45s")
                .run(
                        context -> {
                            var definition =
                                    context.getBean(HttpClientCatalog.class)
                                            .requireByName("projects");

                            assertThat(definition.resilience().enabled()).isTrue();
                            assertThat(definition.resilience().retry().enabled()).isTrue();
                            assertThat(definition.resilience().retry().maxAttempts()).isEqualTo(4);
                            assertThat(definition.resilience().retry().backoff()).hasMillis(250);
                            assertThat(definition.resilience().retry().retryOnStatuses())
                                    .containsExactly(429);
                            assertThat(definition.resilience().circuitBreaker().enabled()).isTrue();
                            assertThat(definition.resilience().circuitBreaker().failureThreshold())
                                    .isEqualTo(5);
                            assertThat(definition.resilience().circuitBreaker().openDuration())
                                    .hasSeconds(45);
                        });
    }

    @Test
    void mapsErrorHandlingBehaviorPropertiesToHttpClientDefinition() {
        contextRunner
                .withPropertyValues(
                        "smbtech.rest-clients.clients.projects.error-handling.include-body=true",
                        "smbtech.rest-clients.clients.projects.error-handling.include-headers=false",
                        "smbtech.rest-clients.clients.projects.error-handling.include-notification-metadata=false",
                        "smbtech.rest-clients.clients.projects.error-handling.notification-code-prefix=E_PROJECTS_HTTP")
                .run(
                        context -> {
                            var errorHandling =
                                    context.getBean(HttpClientCatalog.class)
                                            .requireByName("projects")
                                            .errorHandling();

                            assertThat(errorHandling.includeBody()).isTrue();
                            assertThat(errorHandling.includeHeaders()).isFalse();
                            assertThat(errorHandling.includeNotificationMetadata()).isFalse();
                            assertThat(errorHandling.notificationCodePrefix())
                                    .isEqualTo("E_PROJECTS_HTTP_");
                        });
    }

    @Test
    void bindsTargetOAuth2ExtensionPropertiesForSpringSecurityTokenFlow() {
        contextRunner
                .withPropertyValues(
                        "smbtech.rest-clients.validation.enabled=false",
                        "smbtech.rest-clients.clients.payments.base-url=https://payments.example",
                        "smbtech.rest-clients.clients.payments.authentication-type=CLIENT_CREDENTIALS",
                        "smbtech.rest-clients.clients.payments.token-request-id=payments-token",
                        "smbtech.rest-clients.authentication.client-assertions.payments-token.key-store-id=payments-signing-key",
                        "smbtech.rest-clients.authentication.client-assertions.payments-token.token-lifetime=75s",
                        "smbtech.rest-clients.authentication.client-assertions.payments-token.custom-claims.acgp=acgp.ct",
                        "smbtech.rest-clients.authentication.client-assertions.payments-token.custom-claims.channel=backend",
                        "smbtech.rest-clients.authentication.jwt-bearer.jwt-bearer-token.key-store-id=jwt-bearer-signing-key",
                        "smbtech.rest-clients.authentication.jwt-bearer.jwt-bearer-token.issuer=payments-issuer",
                        "smbtech.rest-clients.authentication.jwt-bearer.jwt-bearer-token.subject=payments-subject",
                        "smbtech.rest-clients.authentication.jwt-bearer.jwt-bearer-token.audience=https://my-provider.example/oauth2/v1/token",
                        "smbtech.rest-clients.authentication.jwt-bearer.jwt-bearer-token.token-lifetime=2m",
                        "smbtech.rest-clients.authentication.jwt-bearer.jwt-bearer-token.custom-claims.tenant=payments",
                        "smbtech.rest-clients.authentication.token-cache.client-credentials=false",
                        "smbtech.rest-clients.authentication.token-cache.jwt-bearer=true",
                        "smbtech.rest-clients.authentication.key-stores.payments-signing-key.base64=ZmFrZS1qa3M=",
                        "smbtech.rest-clients.authentication.key-stores.payments-signing-key.type=JKS",
                        "smbtech.rest-clients.authentication.key-stores.payments-signing-key.password-ref=signing-store-password",
                        "smbtech.rest-clients.authentication.key-stores.payments-signing-key.key-alias=auth",
                        "smbtech.rest-clients.authentication.key-stores.payments-signing-key.key-password-ref=signing-key-password",
                        "smbtech.rest-clients.authentication.credentials.signing-store-password.value=changeit",
                        "smbtech.rest-clients.authentication.credentials.signing-key-password.value=changeit")
                .run(
                        context -> {
                            RestClientProperties properties =
                                    context.getBean(RestClientProperties.class);
                            RestClientProperties.Authentication authentication =
                                    properties.getAuthentication();

                            assertThat(authentication.getClientAssertions())
                                    .containsOnlyKeys("payments-token");
                            RestClientProperties.ClientAssertion assertion =
                                    authentication.getClientAssertions().get("payments-token");
                            assertThat(assertion.getKeyStoreId()).isEqualTo("payments-signing-key");
                            assertThat(assertion.getTokenLifetime()).hasSeconds(75);
                            assertThat(assertion.getCustomClaims())
                                    .containsEntry("acgp", "acgp.ct")
                                    .containsEntry("channel", "backend");

                            assertThat(authentication.getJwtBearer())
                                    .containsOnlyKeys("jwt-bearer-token");
                            RestClientProperties.JwtBearer jwtBearer =
                                    authentication.getJwtBearer().get("jwt-bearer-token");
                            assertThat(jwtBearer.getKeyStoreId())
                                    .isEqualTo("jwt-bearer-signing-key");
                            assertThat(jwtBearer.getIssuer()).isEqualTo("payments-issuer");
                            assertThat(jwtBearer.getSubject()).isEqualTo("payments-subject");
                            assertThat(jwtBearer.getAudience())
                                    .isEqualTo("https://my-provider.example/oauth2/v1/token");
                            assertThat(jwtBearer.getTokenLifetime()).hasMinutes(2);
                            assertThat(jwtBearer.getCustomClaims())
                                    .containsEntry("tenant", "payments");
                            assertThat(authentication.getTokenCache().isClientCredentials())
                                    .isFalse();
                            assertThat(authentication.getTokenCache().isJwtBearer()).isTrue();

                            RestClientProperties.KeyStore signingKey =
                                    authentication.getKeyStores().get("payments-signing-key");
                            assertThat(signingKey.getBase64()).isEqualTo("ZmFrZS1qa3M=");
                            assertThat(signingKey.getType()).isEqualTo("JKS");
                            assertThat(signingKey.getPasswordRef())
                                    .isEqualTo("signing-store-password");
                            assertThat(signingKey.getKeyAlias()).isEqualTo("auth");
                            assertThat(signingKey.getKeyPasswordRef())
                                    .isEqualTo("signing-key-password");
                        });
    }

    @Test
    void defaultsOAuth2TokenCachePropertiesToEnabled() {
        contextRunner.run(
                context -> {
                    RestClientProperties properties = context.getBean(RestClientProperties.class);

                    assertThat(properties.getAuthentication().getTokenCache().isClientCredentials())
                            .isTrue();
                    assertThat(properties.getAuthentication().getTokenCache().isJwtBearer())
                            .isTrue();
                });
    }

    @Test
    void privateKeyJwtClientAssertionRequiresSigningKeyStoreId() {
        contextRunner
                .withUserConfiguration(OAuth2ClientRegistrationConfiguration.class)
                .withPropertyValues(
                        "smbtech.rest-clients.authentication.client-assertions.payments-token.token-lifetime=60s")
                .run(
                        context -> {
                            ClientAssertionJwkResolver jwkResolver =
                                    context.getBean(ClientAssertionJwkResolver.class);
                            ClientRegistration registration =
                                    context.getBean(ClientRegistrationRepository.class)
                                            .findByRegistrationId("payments-token");

                            assertThatThrownBy(() -> jwkResolver.resolve(registration))
                                    .isInstanceOf(
                                            com.smbtech.serviceframework.httpclient.exception
                                                    .HttpClientAuthenticationException.class)
                                    .hasMessageContaining(
                                            "key-store-id is required for private_key_jwt client assertion: payments-token");
                        });
    }

    @Test
    void interceptorsAddDefaultBasicAndCorrelationHeaders() throws Exception {
        MockClientHttpRequest request =
                new MockClientHttpRequest(
                        HttpMethod.GET, URI.create("https://secure.example/ping"));

        MDC.put("traceId", "4bf92f3577b34da6a3ce929d0e0e4736");
        MDC.put("spanId", "a3ce929d0e0e4736");
        MDC.put("transactionId", "tx-123");

        try {
            new DefaultHeadersInterceptor(Map.of("X-Application-Name", "test-service"))
                    .intercept(
                            request,
                            new byte[0],
                            (httpRequest, body) ->
                                    new MockClientHttpResponse(
                                            "ok".getBytes(StandardCharsets.UTF_8), 200));

            new BasicAuthenticationInterceptor(
                            new com.smbtech.serviceframework.httpclient.domain.BasicAuthentication(
                                    "demo", "secret"))
                    .intercept(
                            request,
                            new byte[0],
                            (httpRequest, body) ->
                                    new MockClientHttpResponse(
                                            "ok".getBytes(StandardCharsets.UTF_8), 200));

            new CorrelationHeadersInterceptor(
                            new com.smbtech.serviceframework.starter.restclient.adapter.out.spring
                                    .MdcCorrelationHeadersProvider())
                    .intercept(
                            request,
                            new byte[0],
                            (httpRequest, body) ->
                                    new MockClientHttpResponse(
                                            "ok".getBytes(StandardCharsets.UTF_8), 200));

            HttpHeaders headers = request.getHeaders();
            assertThat(headers.getFirst("X-Application-Name")).isEqualTo("test-service");
            assertThat(headers.getFirst("Authorization"))
                    .isEqualTo("Basic " + basic("demo", "secret"));
            assertThat(headers.getFirst("X-B3-TraceId"))
                    .isEqualTo("4bf92f3577b34da6a3ce929d0e0e4736");
            assertThat(headers.getFirst("X-B3-SpanId")).isEqualTo("a3ce929d0e0e4736");
            assertThat(headers.getFirst("X-Transaction-Id")).isEqualTo("tx-123");
        } finally {
            MDC.clear();
        }
    }

    private String basic(String username, String password) {
        return Base64.getEncoder()
                .encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
    }

    private String basicValue(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private HttpServer startHeaderCaptureServer() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.start();
        return server;
    }

    static class OAuth2ClientRegistrationConfiguration {

        @Bean
        ClientRegistrationRepository clientRegistrationRepository() {
            ClientRegistration payments =
                    ClientRegistration.withRegistrationId("payments-token")
                            .tokenUri("https://auth.example/oauth2/token")
                            .clientId("payments-client")
                            .clientSecret("payments-secret")
                            .clientAuthenticationMethod(
                                    ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                            .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                            .scope("payments.read", "payments.write")
                            .build();

            return new InMemoryClientRegistrationRepository(payments);
        }
    }
}
