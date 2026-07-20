package com.smbtech.serviceframework.starter.restclient.api.oauth2;

import com.smbtech.serviceframework.httpclient.domain.ClientAuthenticationMethod;
import com.smbtech.serviceframework.httpclient.domain.GrantType;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable context for {@link OAuth2TokenRequestCustomizer}.
 *
 * @param registrationId registration id value
 * @param grantType grant type value
 * @param clientAuthenticationMethod client authentication method value
 * @param tokenUri token uri value
 * @param scopes scopes value
 * @param parameters parameters value
 * @param headers headers value
 */
public record OAuth2TokenRequestContext(
        String registrationId,
        GrantType grantType,
        ClientAuthenticationMethod clientAuthenticationMethod,
        URI tokenUri,
        Set<String> scopes,
        Map<String, Object> parameters,
        Map<String, String> headers) {

    /** Creates and validates the record components. */
    public OAuth2TokenRequestContext {
        registrationId = OAuth2ApiSupport.text(registrationId);
        grantType = Objects.requireNonNullElse(grantType, GrantType.CLIENT_CREDENTIALS);
        clientAuthenticationMethod =
                Objects.requireNonNullElse(
                        clientAuthenticationMethod, ClientAuthenticationMethod.CLIENT_SECRET_BASIC);
        scopes = OAuth2ApiSupport.immutableSet(scopes);
        parameters = OAuth2ApiSupport.immutableMap(parameters);
        headers = OAuth2ApiSupport.immutableMap(headers);
    }

    /**
     * Performs the with parameter operation.
     *
     * @param name name value
     * @param value token request parameter value
     * @return with parameter result
     */
    public OAuth2TokenRequestContext withParameter(String name, Object value) {
        LinkedHashMap<String, Object> updated = new LinkedHashMap<>(parameters);
        updated.put(
                OAuth2ApiSupport.name(name, "parameter name"),
                Objects.requireNonNull(value, "value must not be null"));
        return withParameters(updated);
    }

    /**
     * Performs the with header operation.
     *
     * @param name name value
     * @param value token request header value
     * @return with header result
     */
    public OAuth2TokenRequestContext withHeader(String name, String value) {
        LinkedHashMap<String, String> updated = new LinkedHashMap<>(headers);
        updated.put(
                OAuth2ApiSupport.name(name, "header name"),
                Objects.requireNonNull(value, "value must not be null"));
        return withHeaders(updated);
    }

    /**
     * Performs the without parameter operation.
     *
     * @param name name value
     * @return without parameter result
     */
    public OAuth2TokenRequestContext withoutParameter(String name) {
        LinkedHashMap<String, Object> updated = new LinkedHashMap<>(parameters);
        updated.remove(OAuth2ApiSupport.name(name, "parameter name"));
        return withParameters(updated);
    }

    /**
     * Performs the without header operation.
     *
     * @param name name value
     * @return without header result
     */
    public OAuth2TokenRequestContext withoutHeader(String name) {
        LinkedHashMap<String, String> updated = new LinkedHashMap<>(headers);
        updated.remove(OAuth2ApiSupport.name(name, "header name"));
        return withHeaders(updated);
    }

    /**
     * Performs the with parameters operation.
     *
     * @param parameters parameters value
     * @return with parameters result
     */
    public OAuth2TokenRequestContext withParameters(Map<String, Object> parameters) {
        return new OAuth2TokenRequestContext(
                registrationId,
                grantType,
                clientAuthenticationMethod,
                tokenUri,
                scopes,
                parameters,
                headers);
    }

    /**
     * Performs the with headers operation.
     *
     * @param headers headers value
     * @return with headers result
     */
    public OAuth2TokenRequestContext withHeaders(Map<String, String> headers) {
        return new OAuth2TokenRequestContext(
                registrationId,
                grantType,
                clientAuthenticationMethod,
                tokenUri,
                scopes,
                parameters,
                headers);
    }
}
