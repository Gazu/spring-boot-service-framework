package com.smbtech.serviceframework.starter.restclient.api.oauth2;

/** Customizes an OAuth2 token endpoint request before it is sent. */
@FunctionalInterface
public interface OAuth2TokenRequestCustomizer {

    /**
     * Returns a customized copy of the current token request context.
     *
     * @param context immutable token request customization context
     * @return customized token request context
     */
    OAuth2TokenRequestContext customize(OAuth2TokenRequestContext context);
}
