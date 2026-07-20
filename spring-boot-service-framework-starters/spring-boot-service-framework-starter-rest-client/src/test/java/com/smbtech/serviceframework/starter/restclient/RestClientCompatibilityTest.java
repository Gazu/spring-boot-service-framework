package com.smbtech.serviceframework.starter.restclient;

import static org.assertj.core.api.Assertions.assertThat;

import com.smbtech.serviceframework.httpclient.domain.AccessToken;
import com.smbtech.serviceframework.httpclient.domain.GrantType;
import com.smbtech.serviceframework.httpclient.port.out.AccessTokenProvider;
import com.smbtech.serviceframework.logging.port.in.StructuredLoggerFactory;
import com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring.GrantAwareOAuth2AuthorizedClientService;
import com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring.OAuth2ExtensionRegistry;
import com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring.OAuth2RestClientConfigurationValidationRunner;
import com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring.SpringSecurityAuthorizedClientTokenClient;
import com.smbtech.serviceframework.starter.restclient.adapter.out.spring.DefaultApiClientFactory;
import com.smbtech.serviceframework.starter.restclient.adapter.out.spring.DefaultRestClientRegistry;
import com.smbtech.serviceframework.starter.restclient.api.AccessTokenClient;
import com.smbtech.serviceframework.starter.restclient.api.ApiClientFactory;
import com.smbtech.serviceframework.starter.restclient.api.JwtBearerTokenRequest;
import com.smbtech.serviceframework.starter.restclient.api.RequestContext;
import com.smbtech.serviceframework.starter.restclient.api.RequestContextManager;
import com.smbtech.serviceframework.starter.restclient.api.RequestContextScope;
import com.smbtech.serviceframework.starter.restclient.api.RestClientRegistry;
import com.smbtech.serviceframework.starter.restclient.api.oauth2.AccessTokenCacheKeyContext;
import com.smbtech.serviceframework.starter.restclient.api.oauth2.AccessTokenCacheKeyResolver;
import com.smbtech.serviceframework.starter.restclient.api.oauth2.ClientAssertionContext;
import com.smbtech.serviceframework.starter.restclient.api.oauth2.ClientAssertionCustomizer;
import com.smbtech.serviceframework.starter.restclient.api.oauth2.JwtBearerClaimsContext;
import com.smbtech.serviceframework.starter.restclient.api.oauth2.JwtBearerClaimsContributor;
import com.smbtech.serviceframework.starter.restclient.api.oauth2.OAuth2TokenRequestContext;
import com.smbtech.serviceframework.starter.restclient.api.oauth2.OAuth2TokenRequestCustomizer;
import com.smbtech.serviceframework.starter.restclient.autoconfigure.RestClientAutoConfiguration;
import com.smbtech.serviceframework.starter.restclient.autoconfigure.RestClientProperties;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AccessToken;

class RestClientCompatibilityTest {

    private static final String PRINCIPAL_NAME = "spring-boot-service-framework";

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withConfiguration(
                            AutoConfigurations.of(
                                    RestClientAutoConfiguration.class,
                                    OAuth2ClientAutoConfiguration.class));

    @Test
    void validationCanBeDisabledForLegacyOAuth2Configurations() {
        contextRunner
                .withPropertyValues(
                        "spring.security.oauth2.client.provider.my-provider.token-uri=https://auth.example/oauth2/token",
                        "spring.security.oauth2.client.registration.payments-token.provider=my-provider",
                        "spring.security.oauth2.client.registration.payments-token.client-id=payments-client",
                        "spring.security.oauth2.client.registration.payments-token.client-authentication-method=none",
                        "spring.security.oauth2.client.registration.payments-token.authorization-grant-type="
                                + "urn:ietf:params:oauth:grant-type:jwt-bearer",
                        "smbtech.rest-clients.validation.enabled=false",
                        "smbtech.rest-clients.clients.payments.base-url=https://payments.example",
                        "smbtech.rest-clients.clients.payments.authentication-type=CLIENT_CREDENTIALS",
                        "smbtech.rest-clients.clients.payments.token-request-id=payments-token")
                .run(
                        context -> {
                            assertThat(context).hasNotFailed();
                            assertThat(context)
                                    .hasSingleBean(
                                            OAuth2RestClientConfigurationValidationRunner.class);
                        });
    }

    @Test
    void customOAuth2AuthorizedClientServiceIsKeptCompatibleThroughCachePolicyWrapper() {
        AtomicReference<RecordingOAuth2AuthorizedClientService> delegate = new AtomicReference<>();

        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(RestClientAutoConfiguration.class))
                .withBean(ClientRegistrationRepository.class, this::clientRegistrationRepository)
                .withBean(
                        OAuth2AuthorizedClientService.class,
                        () -> {
                            RecordingOAuth2AuthorizedClientService service =
                                    new RecordingOAuth2AuthorizedClientService();
                            delegate.set(service);
                            return service;
                        })
                .run(
                        context -> {
                            assertThat(context).hasNotFailed();
                            assertThat(context).hasSingleBean(OAuth2AuthorizedClientService.class);
                            assertThat(context.getBean(OAuth2AuthorizedClientService.class))
                                    .isInstanceOf(GrantAwareOAuth2AuthorizedClientService.class);

                            OAuth2AuthorizedClientService service =
                                    context.getBean(OAuth2AuthorizedClientService.class);
                            OAuth2AuthorizedClient authorizedClient =
                                    authorizedClient(registration());
                            Authentication principal =
                                    new TestingAuthenticationToken(PRINCIPAL_NAME, "N/A");

                            service.saveAuthorizedClient(authorizedClient, principal);

                            assertThat(delegate.get().savedClients)
                                    .containsExactly(authorizedClient);
                        });
    }

    @Test
    void OAuth2ValidationDoesNotRequireLoggingStarterStructuredLoggerFactoryBean() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(RestClientAutoConfiguration.class))
                .withPropertyValues(
                        "smbtech.rest-clients.authentication.jwt-bearer.unused-token.key-store-id=unused-signing-key")
                .run(
                        context -> {
                            assertThat(context).hasNotFailed();
                            assertThat(context).doesNotHaveBean(StructuredLoggerFactory.class);
                            assertThat(context)
                                    .hasSingleBean(
                                            OAuth2RestClientConfigurationValidationRunner.class);
                        });
    }

    @Test
    void requestContextPublicApiRemainsUsableForConsumers() {
        RequestContext context =
                RequestContext.builder()
                        .header("X-Correlation-Id", "correlation-123")
                        .jwtBearerClaim("customer_id", "17952397-3")
                        .build()
                        .withHeader("X-Channel", "mobile")
                        .withJwtBearerClaim("channel", "mobile");

        assertThat(context.headers()).containsEntry("X-Correlation-Id", "correlation-123");
        assertThat(context.headers()).containsEntry("X-Channel", "mobile");
        assertThat(context.jwtBearerClaims()).containsEntry("customer_id", "17952397-3");
        assertThat(context.jwtBearerClaims()).containsEntry("channel", "mobile");
        assertThat(context.toBuilder().build()).isEqualTo(context);
        assertThat(RequestContext.ofHeaders(Map.of("X-Tenant", "payments")).headers())
                .containsEntry("X-Tenant", "payments");
        assertThat(RequestContext.ofJwtBearerClaims(Map.of("tenant", "payments")).jwtBearerClaims())
                .containsEntry("tenant", "payments");

        contextRunner.run(
                applicationContext -> {
                    assertThat(applicationContext).hasSingleBean(RequestContextManager.class);
                    RequestContextManager manager =
                            applicationContext.getBean(RequestContextManager.class);

                    assertThat(manager.current()).isEqualTo(RequestContext.empty());
                    try (RequestContextScope scope =
                            manager.open(
                                    current ->
                                            current.header("X-Request-Id", "request-123")
                                                    .jwtBearerClaim("customer_id", "17952397-3"))) {
                        assertThat(scope.context().headers())
                                .containsEntry("X-Request-Id", "request-123");
                        assertThat(manager.currentHeaders())
                                .containsEntry("X-Request-Id", "request-123");
                        assertThat(manager.currentJwtBearerClaims())
                                .containsEntry("customer_id", "17952397-3");
                    }
                    assertThat(manager.current()).isEqualTo(RequestContext.empty());
                });
    }

    @Test
    void requestContextPropertiesKeepCompatibleDefaultsAndBindableOverrides() {
        contextRunner
                .withPropertyValues(
                        "smbtech.rest-clients.request-context.enabled=false",
                        "smbtech.rest-clients.request-context.headers=false",
                        "smbtech.rest-clients.request-context.jwt-bearer-claims=false",
                        "smbtech.rest-clients.request-context.blocked-headers[0]=X-Internal-Secret",
                        "smbtech.rest-clients.request-context.blocked-jwt-bearer-claims[0]=customer_secret")
                .run(
                        context -> {
                            assertThat(context).hasNotFailed();

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

        contextRunner.run(
                context -> {
                    RestClientProperties.RequestContext requestContext =
                            context.getBean(RestClientProperties.class).getRequestContext();
                    assertThat(requestContext.isEnabled()).isTrue();
                    assertThat(requestContext.isHeaders()).isTrue();
                    assertThat(requestContext.isJwtBearerClaims()).isTrue();
                    assertThat(requestContext.getBlockedHeaders()).isEmpty();
                    assertThat(requestContext.getBlockedJwtBearerClaims()).isEmpty();
                });
    }

    @Test
    void customRequestContextManagerBeanIsPreserved() {
        RequestContextManager customManager =
                new FixedRequestContextManager(
                        RequestContext.ofHeaders(Map.of("X-Custom-Context", "enabled")));

        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(RestClientAutoConfiguration.class))
                .withBean(RequestContextManager.class, () -> customManager)
                .run(
                        context -> {
                            assertThat(context).hasNotFailed();
                            assertThat(context).hasSingleBean(RequestContextManager.class);
                            assertThat(context.getBean(RequestContextManager.class))
                                    .isSameAs(customManager);
                            assertThat(
                                            context.getBean(RequestContextManager.class)
                                                    .currentHeaders())
                                    .containsEntry("X-Custom-Context", "enabled");
                        });
    }

    @Test
    void customAccessTokenClientBeanReplacesDefaultTokenClient() {
        AccessTokenClient customClient = new FixedAccessTokenClient("custom-client-token");

        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(RestClientAutoConfiguration.class))
                .withBean(AccessTokenClient.class, () -> customClient)
                .run(
                        context -> {
                            assertThat(context).hasNotFailed();
                            assertThat(context).hasSingleBean(AccessTokenClient.class);
                            assertThat(context.getBean(AccessTokenClient.class))
                                    .isSameAs(customClient);
                            assertThat(context)
                                    .doesNotHaveBean(
                                            SpringSecurityAuthorizedClientTokenClient.class);
                            assertThat(
                                            context.getBean(AccessTokenClient.class)
                                                    .clientCredentials("payments-token")
                                                    .value())
                                    .isEqualTo("custom-client-token");
                        });
    }

    @Test
    void customAccessTokenProviderBeanReplacesDefaultTokenProviderAndTokenClient() {
        AccessTokenProvider customProvider =
                (tokenRequestId, scopes) ->
                        "custom-provider-token:" + tokenRequestId + ":" + scopes;

        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(RestClientAutoConfiguration.class))
                .withBean(AccessTokenProvider.class, () -> customProvider)
                .run(
                        context -> {
                            assertThat(context).hasNotFailed();
                            assertThat(context).hasSingleBean(AccessTokenProvider.class);
                            assertThat(context.getBean(AccessTokenProvider.class))
                                    .isSameAs(customProvider);
                            assertThat(context).doesNotHaveBean(AccessTokenClient.class);
                            assertThat(context)
                                    .doesNotHaveBean(
                                            SpringSecurityAuthorizedClientTokenClient.class);
                            assertThat(
                                            context.getBean(AccessTokenProvider.class)
                                                    .getAccessToken(
                                                            "payments-token", "payment.read"))
                                    .isEqualTo("custom-provider-token:payments-token:payment.read");
                        });
    }

    @Test
    void customRestClientRegistryBeanReplacesDefaultRegistry() {
        RestClientRegistry customRegistry = new FixedRestClientRegistry();

        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(RestClientAutoConfiguration.class))
                .withBean(RestClientRegistry.class, () -> customRegistry)
                .run(
                        context -> {
                            assertThat(context).hasNotFailed();
                            assertThat(context).hasSingleBean(RestClientRegistry.class);
                            assertThat(context.getBean(RestClientRegistry.class))
                                    .isSameAs(customRegistry);
                            assertThat(context).doesNotHaveBean(DefaultRestClientRegistry.class);
                            assertThat(context.getBean(RestClientRegistry.class).names())
                                    .containsExactly("fixed");
                        });
    }

    @Test
    void customApiClientFactoryBeanReplacesDefaultApiClientFactory() {
        ApiClientFactory customFactory = new FixedApiClientFactory();

        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(RestClientAutoConfiguration.class))
                .withBean(ApiClientFactory.class, () -> customFactory)
                .run(
                        context -> {
                            assertThat(context).hasNotFailed();
                            assertThat(context).hasSingleBean(ApiClientFactory.class);
                            assertThat(context.getBean(ApiClientFactory.class))
                                    .isSameAs(customFactory);
                            assertThat(context).doesNotHaveBean(DefaultApiClientFactory.class);
                        });
    }

    @Test
    void customOAuth2AuthorizedClientManagerBeanReplacesDefaultManager() {
        OAuth2AuthorizedClientManager customManager = request -> null;

        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(RestClientAutoConfiguration.class))
                .withBean(ClientRegistrationRepository.class, this::clientRegistrationRepository)
                .withBean(OAuth2AuthorizedClientManager.class, () -> customManager)
                .run(
                        context -> {
                            assertThat(context).hasNotFailed();
                            assertThat(context).hasSingleBean(OAuth2AuthorizedClientManager.class);
                            assertThat(context.getBean(OAuth2AuthorizedClientManager.class))
                                    .isSameAs(customManager);
                        });
    }

    @Test
    void customOAuth2AuthorizedClientProviderBeanReplacesDefaultProviderComposition() {
        OAuth2AuthorizedClientProvider customProvider = context -> null;

        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(RestClientAutoConfiguration.class))
                .withBean(ClientRegistrationRepository.class, this::clientRegistrationRepository)
                .withBean(OAuth2AuthorizedClientProvider.class, () -> customProvider)
                .run(
                        context -> {
                            assertThat(context).hasNotFailed();
                            assertThat(context).hasSingleBean(OAuth2AuthorizedClientProvider.class);
                            assertThat(context.getBean(OAuth2AuthorizedClientProvider.class))
                                    .isSameAs(customProvider);
                            assertThat(context).hasSingleBean(OAuth2AuthorizedClientManager.class);
                        });
    }

    @Test
    void oauth2ExtensionBeansAreDiscoveredAndOrdered() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(RestClientAutoConfiguration.class))
                .withUserConfiguration(OAuth2ExtensionBeansConfiguration.class)
                .run(
                        context -> {
                            assertThat(context).hasNotFailed();
                            assertThat(context).hasSingleBean(OAuth2ExtensionRegistry.class);

                            OAuth2ExtensionRegistry registry =
                                    context.getBean(OAuth2ExtensionRegistry.class);
                            JwtBearerClaimsContext claimsContext =
                                    new JwtBearerClaimsContext(
                                            "payments-token",
                                            "payments-client",
                                            java.net.URI.create(
                                                    "https://auth.example/oauth2/token"),
                                            Set.of("payment.read"),
                                            "payment.read",
                                            Map.of(),
                                            Map.of(),
                                            Map.of());
                            assertThat(registry.jwtBearerClaimsContributors())
                                    .extracting(
                                            contributor ->
                                                    contributor
                                                            .contribute(claimsContext)
                                                            .get("order"))
                                    .containsExactly("first", "second");

                            ClientAssertionContext assertionContext =
                                    new ClientAssertionContext(
                                            "payments-token",
                                            "payments-client",
                                            java.net.URI.create(
                                                    "https://auth.example/oauth2/token"),
                                            com.smbtech.serviceframework.httpclient.domain
                                                    .ClientAuthenticationMethod.PRIVATE_KEY_JWT,
                                            java.time.Duration.ofSeconds(60),
                                            Map.of(),
                                            Map.of());
                            assertThat(registry.clientAssertionCustomizers())
                                    .extracting(
                                            customizer ->
                                                    customizer
                                                            .customize(assertionContext)
                                                            .claims()
                                                            .get("order"))
                                    .containsExactly("first", "second");

                            OAuth2TokenRequestContext tokenRequestContext =
                                    new OAuth2TokenRequestContext(
                                            "payments-token",
                                            GrantType.CLIENT_CREDENTIALS,
                                            com.smbtech.serviceframework.httpclient.domain
                                                    .ClientAuthenticationMethod.CLIENT_SECRET_BASIC,
                                            java.net.URI.create(
                                                    "https://auth.example/oauth2/token"),
                                            Set.of("payment.read"),
                                            Map.of(),
                                            Map.of());
                            assertThat(registry.tokenRequestCustomizers())
                                    .extracting(
                                            customizer ->
                                                    customizer
                                                            .customize(tokenRequestContext)
                                                            .parameters()
                                                            .get("order"))
                                    .containsExactly("first", "second");

                            AccessTokenCacheKeyContext cacheKeyContext =
                                    new AccessTokenCacheKeyContext(
                                            "payments-token",
                                            GrantType.CLIENT_CREDENTIALS,
                                            "principal",
                                            Set.of("payment.read"),
                                            Map.of());
                            assertThat(registry.accessTokenCacheKeyResolver())
                                    .hasValueSatisfying(
                                            resolver ->
                                                    assertThat(resolver.resolve(cacheKeyContext))
                                                            .isEqualTo(
                                                                    "payments-token::client_credentials"));
                        });
    }

    private ClientRegistrationRepository clientRegistrationRepository() {
        return new InMemoryClientRegistrationRepository(registration());
    }

    private OAuth2AuthorizedClient authorizedClient(ClientRegistration registration) {
        return new OAuth2AuthorizedClient(
                registration,
                PRINCIPAL_NAME,
                new OAuth2AccessToken(
                        OAuth2AccessToken.TokenType.BEARER,
                        "token-" + registration.getRegistrationId(),
                        Instant.now(),
                        Instant.now().plusSeconds(60),
                        Set.of("payment.read")));
    }

    private ClientRegistration registration() {
        return ClientRegistration.withRegistrationId("payments-token")
                .tokenUri("https://auth.example/oauth2/token")
                .clientId("payments-client")
                .clientSecret("payments-secret")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .scope("payment.read")
                .build();
    }

    private static final class RecordingOAuth2AuthorizedClientService
            implements OAuth2AuthorizedClientService {

        private final List<OAuth2AuthorizedClient> savedClients = new ArrayList<>();

        @Override
        @SuppressWarnings("unchecked")
        public <T extends OAuth2AuthorizedClient> T loadAuthorizedClient(
                String clientRegistrationId, String principalName) {
            return (T)
                    savedClients.stream()
                            .filter(
                                    client ->
                                            client.getClientRegistration()
                                                    .getRegistrationId()
                                                    .equals(clientRegistrationId))
                            .filter(client -> client.getPrincipalName().equals(principalName))
                            .findFirst()
                            .orElse(null);
        }

        @Override
        public void saveAuthorizedClient(
                OAuth2AuthorizedClient authorizedClient, Authentication principal) {
            savedClients.add(authorizedClient);
        }

        @Override
        public void removeAuthorizedClient(String clientRegistrationId, String principalName) {
            savedClients.removeIf(
                    client ->
                            client.getClientRegistration()
                                            .getRegistrationId()
                                            .equals(clientRegistrationId)
                                    && client.getPrincipalName().equals(principalName));
        }
    }

    private static final class FixedRequestContextManager implements RequestContextManager {

        private final RequestContext context;

        private FixedRequestContextManager(RequestContext context) {
            this.context = context;
        }

        @Override
        public RequestContext current() {
            return context;
        }

        @Override
        public RequestContextScope open(RequestContext context) {
            return new FixedRequestContextScope(context);
        }
    }

    private record FixedRequestContextScope(RequestContext context) implements RequestContextScope {

        @Override
        public boolean isClosed() {
            return false;
        }

        @Override
        public void close() {}
    }

    private record FixedAccessTokenClient(String tokenValue) implements AccessTokenClient {

        @Override
        public AccessToken clientCredentials(String tokenRequestId) {
            return accessToken();
        }

        @Override
        public AccessToken clientCredentials(String tokenRequestId, String expectedScopes) {
            return accessToken();
        }

        @Override
        public AccessToken jwtBearer(String tokenRequestId) {
            return accessToken();
        }

        @Override
        public AccessToken jwtBearer(String tokenRequestId, String expectedScopes) {
            return accessToken();
        }

        @Override
        public AccessToken jwtBearer(JwtBearerTokenRequest request) {
            return accessToken();
        }

        private AccessToken accessToken() {
            return new AccessToken(
                    tokenValue, "Bearer", Instant.now().plusSeconds(60), Set.of("payment.read"));
        }
    }

    private static final class FixedRestClientRegistry implements RestClientRegistry {

        private final org.springframework.web.client.RestClient restClient =
                org.springframework.web.client.RestClient.builder()
                        .baseUrl("https://fixed.example")
                        .build();

        @Override
        public org.springframework.web.client.RestClient get(String name) {
            return restClient;
        }

        @Override
        public Set<String> names() {
            return Set.of("fixed");
        }

        @Override
        public Map<String, org.springframework.web.client.RestClient> all() {
            return Map.of("fixed", restClient);
        }
    }

    private static final class FixedApiClientFactory implements ApiClientFactory {

        @Override
        public <T> T create(String clientName, Class<T> apiType) {
            throw new UnsupportedOperationException("fixed factory");
        }

        @Override
        public <T> T create(Class<T> apiType) {
            throw new UnsupportedOperationException("fixed factory");
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class OAuth2ExtensionBeansConfiguration {

        @Bean
        @Order(2)
        JwtBearerClaimsContributor secondJwtBearerClaimsContributor() {
            return context -> Map.of("order", "second");
        }

        @Bean
        @Order(1)
        JwtBearerClaimsContributor firstJwtBearerClaimsContributor() {
            return context -> Map.of("order", "first");
        }

        @Bean
        @Order(2)
        ClientAssertionCustomizer secondClientAssertionCustomizer() {
            return context -> context.withClaim("order", "second");
        }

        @Bean
        @Order(1)
        ClientAssertionCustomizer firstClientAssertionCustomizer() {
            return context -> context.withClaim("order", "first");
        }

        @Bean
        @Order(2)
        OAuth2TokenRequestCustomizer secondTokenRequestCustomizer() {
            return context -> context.withParameter("order", "second");
        }

        @Bean
        @Order(1)
        OAuth2TokenRequestCustomizer firstTokenRequestCustomizer() {
            return context -> context.withParameter("order", "first");
        }

        @Bean
        AccessTokenCacheKeyResolver accessTokenCacheKeyResolver() {
            return context -> context.registrationId() + "::" + context.grantType().value();
        }
    }
}
