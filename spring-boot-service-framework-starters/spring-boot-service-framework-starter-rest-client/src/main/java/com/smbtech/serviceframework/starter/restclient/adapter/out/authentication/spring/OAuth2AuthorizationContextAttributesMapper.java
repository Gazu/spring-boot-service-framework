package com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring;

import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

public final class OAuth2AuthorizationContextAttributesMapper
        implements Function<OAuth2AuthorizeRequest, Map<String, Object>> {

    private final AuthorizedClientServiceOAuth2AuthorizedClientManager.DefaultContextAttributesMapper delegate =
            new AuthorizedClientServiceOAuth2AuthorizedClientManager.DefaultContextAttributesMapper();

    @Override
    public Map<String, Object> apply(OAuth2AuthorizeRequest authorizeRequest) {
        Map<String, Object> attributes = new LinkedHashMap<>(delegate.apply(authorizeRequest));
        Object customClaims = authorizeRequest.getAttribute(
                OAuth2AuthorizationAttributes.JWT_BEARER_CUSTOM_CLAIMS
        );
        if (customClaims != null) {
            attributes.put(OAuth2AuthorizationAttributes.JWT_BEARER_CUSTOM_CLAIMS, customClaims);
        }
        return attributes;
    }
}
