package com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.oauth2.client.OAuth2AuthorizationContext;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtBearerCustomClaimsResolverTest {

    private final JwtBearerCustomClaimsResolver resolver = new JwtBearerCustomClaimsResolver();

    @Test
    void dynamicClaimsOverrideStaticClaims() {
        Map<String, Object> staticClaims = new LinkedHashMap<>();
        staticClaims.put("channel", "backend");
        staticClaims.put("tenant", "payments");
        Map<String, Object> dynamicClaims = new LinkedHashMap<>();
        dynamicClaims.put("channel", "mobile");
        dynamicClaims.put("customer_id", "17952397-3");

        Map<String, Object> resolvedClaims = resolver.resolve(staticClaims, dynamicClaims);

        assertThat(resolvedClaims).containsExactly(
                Map.entry("channel", "mobile"),
                Map.entry("tenant", "payments"),
                Map.entry("customer_id", "17952397-3")
        );
    }

    @Test
    void ignoresReservedBlankAndNullClaims() {
        Map<Object, Object> claims = new LinkedHashMap<>();
        claims.put("iss", "malicious-issuer");
        claims.put("sub", "malicious-subject");
        claims.put("aud", "malicious-audience");
        claims.put("jti", "malicious-jti");
        claims.put("iat", 1L);
        claims.put("exp", 9999999999L);
        claims.put("nbf", 2L);
        claims.put(" ", "blank-name");
        claims.put("customer_id", null);
        claims.put(123, "numeric-name");
        claims.put("channel", "backend");

        Map<String, Object> sanitizedClaims = JwtBearerCustomClaimsResolver.sanitize(claims);

        assertThat(sanitizedClaims).containsExactly(Map.entry("channel", "backend"));
    }

    @Test
    void returnsImmutableResolvedClaims() {
        Map<String, Object> resolvedClaims = resolver.resolve(
                Map.of("tenant", "payments"),
                Map.of("customer_id", "17952397-3")
        );

        assertThatThrownBy(() -> resolvedClaims.put("channel", "mobile"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void readsDynamicClaimsFromSpringSecurityAuthorizationContext() {
        Map<String, Object> customClaims = new LinkedHashMap<>();
        customClaims.put("customer_id", "17952397-3");
        customClaims.put("iss", "malicious-issuer");
        OAuth2AuthorizationContext context = OAuth2AuthorizationContext
                .withClientRegistration(jwtBearerRegistration())
                .principal(new TestingAuthenticationToken("principal", "N/A"))
                .attribute(OAuth2AuthorizationAttributes.JWT_BEARER_CUSTOM_CLAIMS, customClaims)
                .build();

        Map<String, Object> dynamicClaims = JwtBearerCustomClaimsResolver.dynamicClaims(context);

        assertThat(dynamicClaims).containsExactly(Map.entry("customer_id", "17952397-3"));
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
