package com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring;

import static org.assertj.core.api.Assertions.assertThat;

import com.smbtech.serviceframework.httpclient.domain.AccessToken;
import com.smbtech.serviceframework.starter.restclient.adapter.out.spring.RestClientRuntimeTestFixtures;
import com.smbtech.serviceframework.starter.restclient.api.JwtBearerTokenRequest;
import com.smbtech.serviceframework.starter.restclient.api.RequestContextManager;
import com.smbtech.serviceframework.starter.restclient.api.RequestContextScope;
import com.smbtech.serviceframework.starter.restclient.api.oauth2.AccessTokenCacheKeyResolver;
import com.smbtech.serviceframework.starter.restclient.api.oauth2.JwtBearerClaimsContributor;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AccessToken;

class SpringSecurityAuthorizedClientTokenClientTest {

    @Test
    void jwtBearerPassesDynamicCustomClaimsAsSpringSecurityAuthorizationAttributes() {
        ClientRegistration registration = jwtBearerRegistration();
        AtomicReference<OAuth2AuthorizeRequest> capturedRequest = new AtomicReference<>();
        OAuth2AuthorizedClientManager manager =
                request -> {
                    capturedRequest.set(request);
                    return new OAuth2AuthorizedClient(
                            registration,
                            request.getPrincipal().getName(),
                            new OAuth2AccessToken(
                                    OAuth2AccessToken.TokenType.BEARER,
                                    "jwt-token",
                                    Instant.now(),
                                    Instant.now().plusSeconds(60),
                                    Set.of("payment.read")));
                };
        SpringSecurityAuthorizedClientTokenClient client =
                new SpringSecurityAuthorizedClientTokenClient(
                        new InMemoryClientRegistrationRepository(registration),
                        manager,
                        new ScopeValidator());

        AccessToken token =
                client.jwtBearer(
                        new JwtBearerTokenRequest(
                                "payments-api",
                                "payment.read",
                                Map.of("customer_id", "17952397-3")));

        assertThat(token.value()).isEqualTo("jwt-token");
        assertThat(capturedRequest.get().getAttributes())
                .containsEntry(
                        OAuth2AuthorizationAttributes.JWT_BEARER_CUSTOM_CLAIMS,
                        Map.of("customer_id", "17952397-3"));
    }

    @Test
    void jwtBearerMergesRequestContextClaimsWithExplicitClaims() {
        ClientRegistration registration = jwtBearerRegistration();
        RequestContextManager requestContextManager =
                RestClientRuntimeTestFixtures.requestContextManager();
        AtomicReference<OAuth2AuthorizeRequest> capturedRequest = new AtomicReference<>();
        OAuth2AuthorizedClientManager manager =
                request -> {
                    capturedRequest.set(request);
                    return new OAuth2AuthorizedClient(
                            registration,
                            request.getPrincipal().getName(),
                            new OAuth2AccessToken(
                                    OAuth2AccessToken.TokenType.BEARER,
                                    "jwt-token",
                                    Instant.now(),
                                    Instant.now().plusSeconds(60),
                                    Set.of("payment.read")));
                };
        SpringSecurityAuthorizedClientTokenClient client =
                new SpringSecurityAuthorizedClientTokenClient(
                        new InMemoryClientRegistrationRepository(registration),
                        manager,
                        new ScopeValidator(),
                        OAuth2TokenDiagnosticsLogger.disabled(),
                        requestContextManager);

        try (RequestContextScope ignored =
                requestContextManager.open(
                        context ->
                                context.jwtBearerClaim("customer_id", "17952397-3")
                                        .jwtBearerClaim("channel", "web")
                                        .jwtBearerClaim("iss", "ignored"))) {
            client.jwtBearer(
                    "payments-api",
                    "payment.read",
                    Map.of("channel", "mobile", "operation_id", "op-123"));
        }

        assertThat(capturedRequest.get().getPrincipal().getName())
                .startsWith("spring-boot-service-framework:");
        assertThat(capturedRequest.get().getAttributes())
                .containsEntry(
                        OAuth2AuthorizationAttributes.JWT_BEARER_CUSTOM_CLAIMS,
                        Map.of(
                                "customer_id", "17952397-3",
                                "channel", "mobile",
                                "operation_id", "op-123"));
    }

    @Test
    void jwtBearerAppliesClaimsContributorsAfterContextAndExplicitClaims() {
        ClientRegistration registration = jwtBearerRegistration();
        AtomicReference<OAuth2AuthorizeRequest> capturedRequest = new AtomicReference<>();
        OAuth2AuthorizedClientManager manager =
                request -> {
                    capturedRequest.set(request);
                    return new OAuth2AuthorizedClient(
                            registration,
                            request.getPrincipal().getName(),
                            new OAuth2AccessToken(
                                    OAuth2AccessToken.TokenType.BEARER,
                                    "jwt-token",
                                    Instant.now(),
                                    Instant.now().plusSeconds(60),
                                    Set.of("payment.read")));
                };
        JwtBearerClaimsContributor contributor =
                context ->
                        Map.of(
                                "tenant", "payments",
                                "customer_id", context.configuredClaims().get("customer_id"),
                                "password", "must-not-be-used");
        SpringSecurityAuthorizedClientTokenClient client =
                new SpringSecurityAuthorizedClientTokenClient(
                        new InMemoryClientRegistrationRepository(registration),
                        manager,
                        new ScopeValidator(),
                        OAuth2TokenDiagnosticsLogger.disabled(),
                        null,
                        true,
                        Set.of(),
                        new OAuth2ExtensionRegistry(
                                List.of(contributor), List.of(), List.of(), null));

        client.jwtBearer("payments-api", "payment.read", Map.of("customer_id", "17952397-3"));

        assertThat(capturedRequest.get().getAttributes())
                .containsEntry(
                        OAuth2AuthorizationAttributes.JWT_BEARER_CUSTOM_CLAIMS,
                        Map.of(
                                "customer_id", "17952397-3",
                                "tenant", "payments"));
    }

    @Test
    void clientCredentialsUsesAccessTokenCacheKeyResolverPrincipal() {
        ClientRegistration registration = clientCredentialsRegistration();
        AtomicReference<OAuth2AuthorizeRequest> capturedRequest = new AtomicReference<>();
        OAuth2AuthorizedClientManager manager =
                request -> {
                    capturedRequest.set(request);
                    return new OAuth2AuthorizedClient(
                            registration,
                            request.getPrincipal().getName(),
                            new OAuth2AccessToken(
                                    OAuth2AccessToken.TokenType.BEARER,
                                    "client-token",
                                    Instant.now(),
                                    Instant.now().plusSeconds(60),
                                    Set.of("payment.read")));
                };
        AccessTokenCacheKeyResolver resolver =
                context -> "company-cache:" + context.registrationId();
        SpringSecurityAuthorizedClientTokenClient client =
                new SpringSecurityAuthorizedClientTokenClient(
                        new InMemoryClientRegistrationRepository(registration),
                        manager,
                        new ScopeValidator(),
                        OAuth2TokenDiagnosticsLogger.disabled(),
                        null,
                        true,
                        Set.of(),
                        new OAuth2ExtensionRegistry(List.of(), List.of(), List.of(), resolver));

        AccessToken token = client.clientCredentials("payments-api", "payment.read");

        assertThat(token.value()).isEqualTo("client-token");
        assertThat(capturedRequest.get().getPrincipal().getName())
                .isEqualTo("company-cache:payments-api");
    }

    @Test
    void jwtBearerCanOverrideDynamicClaimCacheIdentityWithResolver() {
        ClientRegistration registration = jwtBearerRegistration();
        AtomicReference<String> firstPrincipal = new AtomicReference<>();
        AtomicReference<String> secondPrincipal = new AtomicReference<>();
        OAuth2AuthorizedClientManager manager =
                request -> {
                    if (firstPrincipal.get() == null) {
                        firstPrincipal.set(request.getPrincipal().getName());
                    } else {
                        secondPrincipal.set(request.getPrincipal().getName());
                    }
                    return new OAuth2AuthorizedClient(
                            registration,
                            request.getPrincipal().getName(),
                            new OAuth2AccessToken(
                                    OAuth2AccessToken.TokenType.BEARER,
                                    "jwt-token",
                                    Instant.now(),
                                    Instant.now().plusSeconds(60),
                                    Set.of("payment.read")));
                };
        AccessTokenCacheKeyResolver resolver =
                context -> "shared-jwt-cache:" + context.registrationId();
        SpringSecurityAuthorizedClientTokenClient client =
                new SpringSecurityAuthorizedClientTokenClient(
                        new InMemoryClientRegistrationRepository(registration),
                        manager,
                        new ScopeValidator(),
                        OAuth2TokenDiagnosticsLogger.disabled(),
                        null,
                        true,
                        Set.of(),
                        new OAuth2ExtensionRegistry(List.of(), List.of(), List.of(), resolver));

        client.jwtBearer("payments-api", "payment.read", Map.of("customer_id", "17952397-3"));
        client.jwtBearer("payments-api", "payment.read", Map.of("customer_id", "88888888-8"));

        assertThat(firstPrincipal.get()).isEqualTo("shared-jwt-cache:payments-api");
        assertThat(secondPrincipal.get()).isEqualTo(firstPrincipal.get());
    }

    @Test
    void jwtBearerIgnoresRequestContextClaimsWhenDisabled() {
        ClientRegistration registration = jwtBearerRegistration();
        RequestContextManager requestContextManager =
                RestClientRuntimeTestFixtures.requestContextManager();
        AtomicReference<OAuth2AuthorizeRequest> capturedRequest = new AtomicReference<>();
        OAuth2AuthorizedClientManager manager =
                request -> {
                    capturedRequest.set(request);
                    return new OAuth2AuthorizedClient(
                            registration,
                            request.getPrincipal().getName(),
                            new OAuth2AccessToken(
                                    OAuth2AccessToken.TokenType.BEARER,
                                    "jwt-token",
                                    Instant.now(),
                                    Instant.now().plusSeconds(60),
                                    Set.of("payment.read")));
                };
        SpringSecurityAuthorizedClientTokenClient client =
                new SpringSecurityAuthorizedClientTokenClient(
                        new InMemoryClientRegistrationRepository(registration),
                        manager,
                        new ScopeValidator(),
                        OAuth2TokenDiagnosticsLogger.disabled(),
                        requestContextManager,
                        false);

        try (RequestContextScope ignored =
                requestContextManager.openJwtBearerClaim("customer_id", "17952397-3")) {
            client.jwtBearer("payments-api", "payment.read", Map.of("operation_id", "op-123"));
        }

        assertThat(capturedRequest.get().getAttributes())
                .containsEntry(
                        OAuth2AuthorizationAttributes.JWT_BEARER_CUSTOM_CLAIMS,
                        Map.of("operation_id", "op-123"));
    }

    @Test
    void jwtBearerSanitizesContextAndExplicitClaimsWithConfiguredBlockedClaims() {
        ClientRegistration registration = jwtBearerRegistration();
        RequestContextManager requestContextManager =
                RestClientRuntimeTestFixtures.requestContextManager();
        AtomicReference<OAuth2AuthorizeRequest> capturedRequest = new AtomicReference<>();
        OAuth2AuthorizedClientManager manager =
                request -> {
                    capturedRequest.set(request);
                    return new OAuth2AuthorizedClient(
                            registration,
                            request.getPrincipal().getName(),
                            new OAuth2AccessToken(
                                    OAuth2AccessToken.TokenType.BEARER,
                                    "jwt-token",
                                    Instant.now(),
                                    Instant.now().plusSeconds(60),
                                    Set.of("payment.read")));
                };
        SpringSecurityAuthorizedClientTokenClient client =
                new SpringSecurityAuthorizedClientTokenClient(
                        new InMemoryClientRegistrationRepository(registration),
                        manager,
                        new ScopeValidator(),
                        OAuth2TokenDiagnosticsLogger.disabled(),
                        requestContextManager,
                        true,
                        Set.of("customer_id"));

        try (RequestContextScope ignored =
                requestContextManager.open(
                        context ->
                                context.jwtBearerClaim("customer_id", "17952397-3")
                                        .jwtBearerClaim("channel", "web")
                                        .jwtBearerClaim("client_secret", "secret"))) {
            client.jwtBearer(
                    "payments-api",
                    "payment.read",
                    Map.of(
                            "customer_id", "88888888-8",
                            "operation_id", "op-123",
                            "password", "secret"));
        }

        assertThat(capturedRequest.get().getAttributes())
                .containsEntry(
                        OAuth2AuthorizationAttributes.JWT_BEARER_CUSTOM_CLAIMS,
                        Map.of(
                                "channel", "web",
                                "operation_id", "op-123"));
    }

    @Test
    void jwtBearerDoesNotPassDynamicAttributesWhenClaimsAreNotUsable() {
        ClientRegistration registration = jwtBearerRegistration();
        AtomicReference<OAuth2AuthorizeRequest> capturedRequest = new AtomicReference<>();
        OAuth2AuthorizedClientManager manager =
                request -> {
                    capturedRequest.set(request);
                    return new OAuth2AuthorizedClient(
                            registration,
                            request.getPrincipal().getName(),
                            new OAuth2AccessToken(
                                    OAuth2AccessToken.TokenType.BEARER,
                                    "jwt-token",
                                    Instant.now(),
                                    Instant.now().plusSeconds(60),
                                    Set.of("payment.read")));
                };
        SpringSecurityAuthorizedClientTokenClient client =
                new SpringSecurityAuthorizedClientTokenClient(
                        new InMemoryClientRegistrationRepository(registration),
                        manager,
                        new ScopeValidator());

        client.jwtBearer("payments-api", "payment.read", Map.of("iss", "malicious-issuer"));

        assertThat(capturedRequest.get().getPrincipal().getName())
                .isEqualTo("spring-boot-service-framework");
        assertThat(capturedRequest.get().getAttributes())
                .doesNotContainKey(OAuth2AuthorizationAttributes.JWT_BEARER_CUSTOM_CLAIMS);
    }

    @Test
    void jwtBearerUsesDifferentPrincipalsForDifferentDynamicClaims() {
        ClientRegistration registration = jwtBearerRegistration();
        AtomicReference<String> firstPrincipal = new AtomicReference<>();
        AtomicReference<String> secondPrincipal = new AtomicReference<>();
        OAuth2AuthorizedClientManager manager =
                request -> {
                    if (firstPrincipal.get() == null) {
                        firstPrincipal.set(request.getPrincipal().getName());
                    } else {
                        secondPrincipal.set(request.getPrincipal().getName());
                    }
                    return new OAuth2AuthorizedClient(
                            registration,
                            request.getPrincipal().getName(),
                            new OAuth2AccessToken(
                                    OAuth2AccessToken.TokenType.BEARER,
                                    "jwt-token",
                                    Instant.now(),
                                    Instant.now().plusSeconds(60),
                                    Set.of("payment.read")));
                };
        SpringSecurityAuthorizedClientTokenClient client =
                new SpringSecurityAuthorizedClientTokenClient(
                        new InMemoryClientRegistrationRepository(registration),
                        manager,
                        new ScopeValidator());

        client.jwtBearer("payments-api", "payment.read", Map.of("customer_id", "17952397-3"));
        client.jwtBearer("payments-api", "payment.read", Map.of("customer_id", "88888888-8"));

        assertThat(firstPrincipal.get()).startsWith("spring-boot-service-framework:");
        assertThat(secondPrincipal.get()).startsWith("spring-boot-service-framework:");
        assertThat(secondPrincipal.get()).isNotEqualTo(firstPrincipal.get());
    }

    @Test
    void jwtBearerUsesDifferentPrincipalsForDynamicClaimsWithAmbiguousTextRepresentation() {
        ClientRegistration registration = jwtBearerRegistration();
        AtomicReference<String> firstPrincipal = new AtomicReference<>();
        AtomicReference<String> secondPrincipal = new AtomicReference<>();
        OAuth2AuthorizedClientManager manager =
                request -> {
                    if (firstPrincipal.get() == null) {
                        firstPrincipal.set(request.getPrincipal().getName());
                    } else {
                        secondPrincipal.set(request.getPrincipal().getName());
                    }
                    return new OAuth2AuthorizedClient(
                            registration,
                            request.getPrincipal().getName(),
                            new OAuth2AccessToken(
                                    OAuth2AccessToken.TokenType.BEARER,
                                    "jwt-token",
                                    Instant.now(),
                                    Instant.now().plusSeconds(60),
                                    Set.of("payment.read")));
                };
        SpringSecurityAuthorizedClientTokenClient client =
                new SpringSecurityAuthorizedClientTokenClient(
                        new InMemoryClientRegistrationRepository(registration),
                        manager,
                        new ScopeValidator());

        client.jwtBearer("payments-api", "payment.read", Map.of("a", "b&c=d"));
        client.jwtBearer("payments-api", "payment.read", Map.of("a", "b", "c", "d"));

        assertThat(secondPrincipal.get()).isNotEqualTo(firstPrincipal.get());
    }

    @Test
    void jwtBearerUsesSamePrincipalForSameClaimsWithDifferentInsertionOrder() {
        ClientRegistration registration = jwtBearerRegistration();
        AtomicReference<String> firstPrincipal = new AtomicReference<>();
        AtomicReference<String> secondPrincipal = new AtomicReference<>();
        OAuth2AuthorizedClientManager manager =
                request -> {
                    if (firstPrincipal.get() == null) {
                        firstPrincipal.set(request.getPrincipal().getName());
                    } else {
                        secondPrincipal.set(request.getPrincipal().getName());
                    }
                    return new OAuth2AuthorizedClient(
                            registration,
                            request.getPrincipal().getName(),
                            new OAuth2AccessToken(
                                    OAuth2AccessToken.TokenType.BEARER,
                                    "jwt-token",
                                    Instant.now(),
                                    Instant.now().plusSeconds(60),
                                    Set.of("payment.read")));
                };
        SpringSecurityAuthorizedClientTokenClient client =
                new SpringSecurityAuthorizedClientTokenClient(
                        new InMemoryClientRegistrationRepository(registration),
                        manager,
                        new ScopeValidator());
        Map<String, Object> firstClaims = new java.util.LinkedHashMap<>();
        firstClaims.put("customer_id", "17952397-3");
        firstClaims.put("channel", "mobile");
        Map<String, Object> secondClaims = new java.util.LinkedHashMap<>();
        secondClaims.put("channel", "mobile");
        secondClaims.put("customer_id", "17952397-3");

        client.jwtBearer("payments-api", "payment.read", firstClaims);
        client.jwtBearer("payments-api", "payment.read", secondClaims);

        assertThat(secondPrincipal.get()).isEqualTo(firstPrincipal.get());
    }

    private ClientRegistration jwtBearerRegistration() {
        return ClientRegistration.withRegistrationId("payments-api")
                .tokenUri("https://auth.example/token")
                .clientId("payments-client")
                .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                .authorizationGrantType(
                        new AuthorizationGrantType("urn:ietf:params:oauth:grant-type:jwt-bearer"))
                .scope("payment.read")
                .build();
    }

    private ClientRegistration clientCredentialsRegistration() {
        return ClientRegistration.withRegistrationId("payments-api")
                .tokenUri("https://auth.example/token")
                .clientId("payments-client")
                .clientSecret("payments-secret")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .scope("payment.read")
                .build();
    }
}
