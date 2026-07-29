package com.smbtech.serviceframework.starter.restclient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.smbtech.serviceframework.httpclient.domain.ClientAuthenticationMethod;
import com.smbtech.serviceframework.httpclient.domain.GrantType;
import com.smbtech.serviceframework.starter.restclient.api.oauth2.AccessTokenCacheKeyContext;
import com.smbtech.serviceframework.starter.restclient.api.oauth2.AccessTokenCacheKeyResolver;
import com.smbtech.serviceframework.starter.restclient.api.oauth2.ClientAssertionContext;
import com.smbtech.serviceframework.starter.restclient.api.oauth2.ClientAssertionCustomizer;
import com.smbtech.serviceframework.starter.restclient.api.oauth2.JwtBearerClaimsContext;
import com.smbtech.serviceframework.starter.restclient.api.oauth2.JwtBearerClaimsContributor;
import com.smbtech.serviceframework.starter.restclient.api.oauth2.OAuth2TokenRequestContext;
import com.smbtech.serviceframework.starter.restclient.api.oauth2.OAuth2TokenRequestCustomizer;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class OAuth2ExtensionApiTest {

    @Test
    void jwtBearerClaimsContributorReceivesImmutableContext() {
        Map<String, Object> configuredClaims = new LinkedHashMap<>();
        configuredClaims.put("channel", "backend");
        Set<String> requestedScopes = new LinkedHashSet<>();
        requestedScopes.add("payment.read");

        JwtBearerClaimsContext context =
                new JwtBearerClaimsContext(
                        " payments-token ",
                        " payments-client ",
                        URI.create("https://auth.example/oauth2/token"),
                        requestedScopes,
                        " payment.read ",
                        configuredClaims,
                        Map.of("customer_id", "17952397-3"),
                        Map.of("channel", "mobile"));
        configuredClaims.put("mutated", true);
        requestedScopes.add("payment.write");

        JwtBearerClaimsContributor contributor =
                current ->
                        Map.of(
                                "customer_id", current.requestContextClaims().get("customer_id"),
                                "channel", current.explicitClaims().get("channel"));

        assertThat(context.registrationId()).isEqualTo("payments-token");
        assertThat(context.clientId()).isEqualTo("payments-client");
        assertThat(context.requestedScopes()).containsExactly("payment.read");
        assertThat(context.configuredClaims()).containsExactly(Map.entry("channel", "backend"));
        assertThat(contributor.contribute(context))
                .containsExactlyInAnyOrderEntriesOf(
                        Map.of(
                                "customer_id", "17952397-3",
                                "channel", "mobile"));
        assertThatThrownBy(() -> context.configuredClaims().put("other", "value"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void clientAssertionCustomizerReturnsCustomizedContextCopies() {
        ClientAssertionContext context =
                new ClientAssertionContext(
                        "payments-token",
                        "payments-client",
                        URI.create("https://auth.example/oauth2/token"),
                        ClientAuthenticationMethod.PRIVATE_KEY_JWT,
                        Duration.ofSeconds(45),
                        Map.of("typ", "JWT"),
                        Map.of("channel", "backend"));

        ClientAssertionCustomizer customizer =
                current ->
                        current.withHeader("kid", "key-1")
                                .withClaim("tenant", "payments")
                                .withoutClaim("channel");

        ClientAssertionContext customized = customizer.customize(context);

        assertThat(context.headers()).containsExactly(Map.entry("typ", "JWT"));
        assertThat(context.claims()).containsExactly(Map.entry("channel", "backend"));
        assertThatThrownBy(() -> context.headers().put("kid", "key-1"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThat(context.headers()).containsExactlyInAnyOrderEntriesOf(Map.of("typ", "JWT"));
        assertThat(customized.headers())
                .containsExactlyInAnyOrderEntriesOf(
                        Map.of(
                                "typ", "JWT",
                                "kid", "key-1"));
        assertThat(customized.claims()).containsExactly(Map.entry("tenant", "payments"));
        assertThat(customized.tokenLifetime()).isEqualTo(Duration.ofSeconds(45));
    }

    @Test
    void oauth2TokenRequestCustomizerReturnsCustomizedContextCopies() {
        OAuth2TokenRequestContext context =
                new OAuth2TokenRequestContext(
                        "payments-token",
                        GrantType.JWT_BEARER,
                        ClientAuthenticationMethod.NONE,
                        URI.create("https://auth.example/oauth2/token"),
                        Set.of("payment.read"),
                        Map.of(
                                "grant_type",
                                GrantType.JWT_BEARER.value(),
                                "client_id",
                                "payments-client"),
                        Map.of("X-Token-Client", "payments"));

        OAuth2TokenRequestCustomizer customizer =
                current ->
                        current.withParameter("resource", "payments-api")
                                .withoutParameter("client_id")
                                .withHeader("X-Channel", "backend");

        OAuth2TokenRequestContext customized = customizer.customize(context);

        assertThat(context.parameters()).containsEntry("client_id", "payments-client");
        assertThat(context.parameters()).doesNotContainKey("resource");
        assertThat(context.headers()).doesNotContainKey("X-Channel");
        assertThat(customized.parameters()).containsEntry("resource", "payments-api");
        assertThat(customized.parameters()).doesNotContainKey("client_id");
        assertThat(customized.headers()).containsEntry("X-Channel", "backend");
        assertThat(context.scopes()).containsExactly("payment.read");
        assertThatThrownBy(() -> context.parameters().put("resource", "payments-api"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> context.scopes().add("payment.write"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void accessTokenCacheKeyResolverReceivesImmutableContext() {
        AccessTokenCacheKeyContext context =
                new AccessTokenCacheKeyContext(
                        " payments-token ",
                        GrantType.JWT_BEARER,
                        " principal ",
                        Set.of("payment.read"),
                        Map.of("customer_id", "17952397-3"));
        AccessTokenCacheKeyResolver resolver =
                current ->
                        current.registrationId()
                                + "::"
                                + current.grantType().value()
                                + "::"
                                + current.authorizationAttributes().get("customer_id");

        assertThat(context.registrationId()).isEqualTo("payments-token");
        assertThat(context.principalName()).isEqualTo("principal");
        assertThat(resolver.resolve(context))
                .isEqualTo(
                        "payments-token::urn:ietf:params:oauth:grant-type:jwt-bearer::17952397-3");
        assertThatThrownBy(() -> context.authorizationAttributes().put("other", "value"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    void oauth2ContextsRecursivelyCopyStructuredValues() {
        List<Object> audiences = new ArrayList<>(List.of("payments"));
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("audiences", audiences);

        AccessTokenCacheKeyContext context =
                new AccessTokenCacheKeyContext(
                        "payments-token",
                        GrantType.JWT_BEARER,
                        "principal",
                        Set.of(),
                        Map.of("claims", claims));
        audiences.add("transfers");

        Map<String, Object> immutableClaims =
                (Map<String, Object>) context.authorizationAttributes().get("claims");
        List<Object> immutableAudiences = (List<Object>) immutableClaims.get("audiences");
        assertThat(immutableAudiences).containsExactly("payments");
        assertThatThrownBy(() -> immutableAudiences.add("transfers"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
