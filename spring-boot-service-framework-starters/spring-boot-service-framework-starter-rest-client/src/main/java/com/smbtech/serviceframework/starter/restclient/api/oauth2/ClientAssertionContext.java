package com.smbtech.serviceframework.starter.restclient.api.oauth2;

import com.smbtech.serviceframework.httpclient.domain.ClientAuthenticationMethod;
import java.net.URI;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable context for {@link ClientAssertionCustomizer}.
 *
 * @param registrationId registration id value
 * @param clientId client id value
 * @param tokenUri token uri value
 * @param clientAuthenticationMethod client authentication method value
 * @param tokenLifetime token lifetime value
 * @param headers headers value
 * @param claims claims value
 */
public record ClientAssertionContext(
        String registrationId,
        String clientId,
        URI tokenUri,
        ClientAuthenticationMethod clientAuthenticationMethod,
        Duration tokenLifetime,
        Map<String, Object> headers,
        Map<String, Object> claims) {

    /** Creates and validates the record components. */
    public ClientAssertionContext {
        registrationId = OAuth2ApiSupport.text(registrationId);
        clientId = OAuth2ApiSupport.text(clientId);
        clientAuthenticationMethod =
                Objects.requireNonNullElse(
                        clientAuthenticationMethod, ClientAuthenticationMethod.PRIVATE_KEY_JWT);
        tokenLifetime =
                tokenLifetime == null || tokenLifetime.isZero() || tokenLifetime.isNegative()
                        ? Duration.ofSeconds(60)
                        : tokenLifetime;
        headers = OAuth2ApiSupport.immutableMap(headers);
        claims = OAuth2ApiSupport.immutableMap(claims);
    }

    /**
     * Performs the with header operation.
     *
     * @param name name value
     * @param value JOSE header value
     * @return with header result
     */
    public ClientAssertionContext withHeader(String name, Object value) {
        LinkedHashMap<String, Object> updated = new LinkedHashMap<>(headers);
        updated.put(
                OAuth2ApiSupport.name(name, "header name"),
                Objects.requireNonNull(value, "value must not be null"));
        return withHeaders(updated);
    }

    /**
     * Performs the with claim operation.
     *
     * @param name name value
     * @param value assertion claim value
     * @return with claim result
     */
    public ClientAssertionContext withClaim(String name, Object value) {
        LinkedHashMap<String, Object> updated = new LinkedHashMap<>(claims);
        updated.put(
                OAuth2ApiSupport.name(name, "claim name"),
                Objects.requireNonNull(value, "value must not be null"));
        return withClaims(updated);
    }

    /**
     * Performs the without header operation.
     *
     * @param name name value
     * @return without header result
     */
    public ClientAssertionContext withoutHeader(String name) {
        LinkedHashMap<String, Object> updated = new LinkedHashMap<>(headers);
        updated.remove(OAuth2ApiSupport.name(name, "header name"));
        return withHeaders(updated);
    }

    /**
     * Performs the without claim operation.
     *
     * @param name name value
     * @return without claim result
     */
    public ClientAssertionContext withoutClaim(String name) {
        LinkedHashMap<String, Object> updated = new LinkedHashMap<>(claims);
        updated.remove(OAuth2ApiSupport.name(name, "claim name"));
        return withClaims(updated);
    }

    /**
     * Performs the with headers operation.
     *
     * @param headers headers value
     * @return with headers result
     */
    public ClientAssertionContext withHeaders(Map<String, Object> headers) {
        return new ClientAssertionContext(
                registrationId,
                clientId,
                tokenUri,
                clientAuthenticationMethod,
                tokenLifetime,
                headers,
                claims);
    }

    /**
     * Performs the with claims operation.
     *
     * @param claims claims value
     * @return with claims result
     */
    public ClientAssertionContext withClaims(Map<String, Object> claims) {
        return new ClientAssertionContext(
                registrationId,
                clientId,
                tokenUri,
                clientAuthenticationMethod,
                tokenLifetime,
                headers,
                claims);
    }

    /**
     * Performs the with token lifetime operation.
     *
     * @param tokenLifetime token lifetime value
     * @return with token lifetime result
     */
    public ClientAssertionContext withTokenLifetime(Duration tokenLifetime) {
        return new ClientAssertionContext(
                registrationId,
                clientId,
                tokenUri,
                clientAuthenticationMethod,
                tokenLifetime,
                headers,
                claims);
    }
}
