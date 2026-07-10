package com.smbtech.serviceframework.starter.restclient.adapter.out.authentication;

import com.smbtech.serviceframework.httpclient.domain.AccessToken;
import com.smbtech.serviceframework.httpclient.domain.ClientAuthenticationMethod;
import com.smbtech.serviceframework.httpclient.domain.GrantType;
import com.smbtech.serviceframework.httpclient.domain.TokenRequestDefinition;
import com.smbtech.serviceframework.httpclient.exception.AuthenticationException;
import com.smbtech.serviceframework.httpclient.port.out.JwtAssertionProvider;
import com.smbtech.serviceframework.httpclient.service.ScopeValidator;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

public final class OAuth2AccessTokenProvider {

    private static final ParameterizedTypeReference<Map<String, Object>> TOKEN_RESPONSE =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient restClient;
    private final JwtAssertionProvider jwtAssertionProvider;
    private final ScopeValidator scopeValidator;
    private final Clock clock;

    public OAuth2AccessTokenProvider(
            RestClient.Builder restClientBuilder,
            JwtAssertionProvider jwtAssertionProvider,
            ScopeValidator scopeValidator,
            Clock clock
    ) {
        this.restClient = restClientBuilder.clone().build();
        this.jwtAssertionProvider = jwtAssertionProvider;
        this.scopeValidator = scopeValidator;
        this.clock = clock;
    }

    public AccessToken fetch(TokenRequestDefinition definition) {
        validate(definition);

        MultiValueMap<String, String> form = form(definition);
        Map<String, Object> response = restClient.post()
                .uri(definition.tokenUri())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .headers(headers -> applyClientAuthentication(headers, form, definition))
                .body(form)
                .retrieve()
                .body(TOKEN_RESPONSE);

        return toAccessToken(definition, response);
    }

    private void validate(TokenRequestDefinition definition) {
        if (definition.tokenUri() == null) {
            throw new AuthenticationException("tokenUri is required for token request: " + definition.id());
        }
        if (definition.clientAuthenticationMethod() != ClientAuthenticationMethod.NONE
                && definition.clientId().isBlank()) {
            throw new AuthenticationException("clientId is required for token request: " + definition.id());
        }
        if (definition.clientAuthenticationMethod() == ClientAuthenticationMethod.CLIENT_SECRET_BASIC
                && definition.clientSecret().isBlank()) {
            throw new AuthenticationException("clientSecret is required for token request: " + definition.id());
        }
        if (definition.clientAuthenticationMethod() == ClientAuthenticationMethod.CLIENT_SECRET_POST
                && definition.clientSecret().isBlank()) {
            throw new AuthenticationException("clientSecret is required for token request: " + definition.id());
        }
    }

    private MultiValueMap<String, String> form(TokenRequestDefinition definition) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", definition.grantType().value());

        if (!definition.scopeValue().isBlank()) {
            form.add("scope", definition.scopeValue());
        }

        if (definition.grantType() == GrantType.JWT_BEARER) {
            form.add("assertion", jwtAssertionProvider.createAssertion(definition));
        }

        return form;
    }

    private void applyClientAuthentication(
            HttpHeaders headers,
            MultiValueMap<String, String> form,
            TokenRequestDefinition definition
    ) {
        if (definition.clientAuthenticationMethod() == ClientAuthenticationMethod.CLIENT_SECRET_BASIC) {
            headers.setBasicAuth(definition.clientId(), definition.clientSecret());
        }
        if (definition.clientAuthenticationMethod() == ClientAuthenticationMethod.CLIENT_SECRET_POST) {
            form.add("client_id", definition.clientId());
            form.add("client_secret", definition.clientSecret());
        }
        if (definition.clientAuthenticationMethod() == ClientAuthenticationMethod.NONE && !definition.clientId().isBlank()) {
            form.add("client_id", definition.clientId());
        }
    }

    private AccessToken toAccessToken(TokenRequestDefinition definition, Map<String, Object> response) {
        if (response == null || response.get("access_token") == null) {
            throw new AuthenticationException("Token endpoint did not return access_token for: " + definition.id());
        }

        String accessToken = response.get("access_token").toString();
        String tokenType = value(response, "token_type", "Bearer");
        long expiresIn = number(response.get("expires_in"), 300L);
        String responseScope = value(response, "scope", definition.scopeValue());
        Set<String> scopes = scopeValidator.parse(responseScope);
        Instant expiresAt = clock.instant()
                .plusSeconds(expiresIn)
                .minus(definition.cacheSkew());

        return new AccessToken(accessToken, tokenType, expiresAt, scopes);
    }

    private String value(Map<String, Object> response, String key, String fallback) {
        Object value = response.get(key);
        return value == null ? fallback : value.toString();
    }

    private long number(Object value, long fallback) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            return Long.parseLong(text);
        }
        return fallback;
    }
}
