package com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring;

import com.smbtech.serviceframework.httpclient.domain.AccessToken;
import com.smbtech.serviceframework.httpclient.service.ScopeValidator;
import com.smbtech.serviceframework.starter.restclient.api.JwtBearerTokenRequest;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AccessToken;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class SpringSecurityAuthorizedClientTokenClientTest {

    @Test
    void jwtBearerPassesDynamicCustomClaimsAsSpringSecurityAuthorizationAttributes() {
        ClientRegistration registration = jwtBearerRegistration();
        AtomicReference<OAuth2AuthorizeRequest> capturedRequest = new AtomicReference<>();
        OAuth2AuthorizedClientManager manager = request -> {
            capturedRequest.set(request);
            return new OAuth2AuthorizedClient(
                    registration,
                    request.getPrincipal().getName(),
                    new OAuth2AccessToken(
                            OAuth2AccessToken.TokenType.BEARER,
                            "jwt-token",
                            Instant.now(),
                            Instant.now().plusSeconds(60),
                            Set.of("payment.read")
                    )
            );
        };
        SpringSecurityAuthorizedClientTokenClient client = new SpringSecurityAuthorizedClientTokenClient(
                new InMemoryClientRegistrationRepository(registration),
                manager,
                new ScopeValidator()
        );

        AccessToken token = client.jwtBearer(new JwtBearerTokenRequest(
                "payments-api",
                "payment.read",
                Map.of("customer_id", "17952397-3")
        ));

        assertThat(token.value()).isEqualTo("jwt-token");
        assertThat(capturedRequest.get().getAttributes())
                .containsEntry(
                        OAuth2AuthorizationAttributes.JWT_BEARER_CUSTOM_CLAIMS,
                        Map.of("customer_id", "17952397-3")
                );
    }

    @Test
    void jwtBearerDoesNotPassDynamicAttributesWhenClaimsAreNotUsable() {
        ClientRegistration registration = jwtBearerRegistration();
        AtomicReference<OAuth2AuthorizeRequest> capturedRequest = new AtomicReference<>();
        OAuth2AuthorizedClientManager manager = request -> {
            capturedRequest.set(request);
            return new OAuth2AuthorizedClient(
                    registration,
                    request.getPrincipal().getName(),
                    new OAuth2AccessToken(
                            OAuth2AccessToken.TokenType.BEARER,
                            "jwt-token",
                            Instant.now(),
                            Instant.now().plusSeconds(60),
                            Set.of("payment.read")
                    )
            );
        };
        SpringSecurityAuthorizedClientTokenClient client = new SpringSecurityAuthorizedClientTokenClient(
                new InMemoryClientRegistrationRepository(registration),
                manager,
                new ScopeValidator()
        );

        client.jwtBearer("payments-api", "payment.read", Map.of("iss", "malicious-issuer"));

        assertThat(capturedRequest.get().getPrincipal().getName()).isEqualTo("spring-boot-service-framework");
        assertThat(capturedRequest.get().getAttributes())
                .doesNotContainKey(OAuth2AuthorizationAttributes.JWT_BEARER_CUSTOM_CLAIMS);
    }

    @Test
    void jwtBearerUsesDifferentPrincipalsForDifferentDynamicClaims() {
        ClientRegistration registration = jwtBearerRegistration();
        AtomicReference<String> firstPrincipal = new AtomicReference<>();
        AtomicReference<String> secondPrincipal = new AtomicReference<>();
        OAuth2AuthorizedClientManager manager = request -> {
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
                            Set.of("payment.read")
                    )
            );
        };
        SpringSecurityAuthorizedClientTokenClient client = new SpringSecurityAuthorizedClientTokenClient(
                new InMemoryClientRegistrationRepository(registration),
                manager,
                new ScopeValidator()
        );

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
        OAuth2AuthorizedClientManager manager = request -> {
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
                            Set.of("payment.read")
                    )
            );
        };
        SpringSecurityAuthorizedClientTokenClient client = new SpringSecurityAuthorizedClientTokenClient(
                new InMemoryClientRegistrationRepository(registration),
                manager,
                new ScopeValidator()
        );

        client.jwtBearer("payments-api", "payment.read", Map.of("a", "b&c=d"));
        client.jwtBearer("payments-api", "payment.read", Map.of("a", "b", "c", "d"));

        assertThat(secondPrincipal.get()).isNotEqualTo(firstPrincipal.get());
    }

    @Test
    void jwtBearerUsesSamePrincipalForSameClaimsWithDifferentInsertionOrder() {
        ClientRegistration registration = jwtBearerRegistration();
        AtomicReference<String> firstPrincipal = new AtomicReference<>();
        AtomicReference<String> secondPrincipal = new AtomicReference<>();
        OAuth2AuthorizedClientManager manager = request -> {
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
                            Set.of("payment.read")
                    )
            );
        };
        SpringSecurityAuthorizedClientTokenClient client = new SpringSecurityAuthorizedClientTokenClient(
                new InMemoryClientRegistrationRepository(registration),
                manager,
                new ScopeValidator()
        );
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
        return ClientRegistration
                .withRegistrationId("payments-api")
                .tokenUri("https://auth.example/token")
                .clientId("payments-client")
                .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                .authorizationGrantType(new AuthorizationGrantType("urn:ietf:params:oauth:grant-type:jwt-bearer"))
                .scope("payment.read")
                .build();
    }
}
