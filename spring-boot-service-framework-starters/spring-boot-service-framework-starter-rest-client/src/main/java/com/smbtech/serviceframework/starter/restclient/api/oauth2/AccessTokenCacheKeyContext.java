package com.smbtech.serviceframework.starter.restclient.api.oauth2;

import com.smbtech.serviceframework.httpclient.domain.GrantType;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable context passed to {@link AccessTokenCacheKeyResolver}.
 *
 * @param registrationId registration id value
 * @param grantType grant type value
 * @param principalName principal name value
 * @param scopes scopes value
 * @param authorizationAttributes authorization attributes value
 */
public record AccessTokenCacheKeyContext(
        String registrationId,
        GrantType grantType,
        String principalName,
        Set<String> scopes,
        Map<String, Object> authorizationAttributes) {

    /** Creates and validates the record components. */
    public AccessTokenCacheKeyContext {
        registrationId = OAuth2ApiSupport.text(registrationId);
        grantType = Objects.requireNonNullElse(grantType, GrantType.CLIENT_CREDENTIALS);
        principalName = OAuth2ApiSupport.text(principalName);
        scopes = OAuth2ApiSupport.immutableSet(scopes);
        authorizationAttributes = OAuth2ApiSupport.immutableMap(authorizationAttributes);
    }
}
