package com.smbtech.serviceframework.starter.restclient.api.oauth2;

/** Resolves the cache identity used for OAuth2 authorized client token reuse. */
@FunctionalInterface
public interface AccessTokenCacheKeyResolver {

    /**
     * Resolves a stable cache key for the current token request.
     *
     * @param context token cache key context
     * @return cache key
     */
    String resolve(AccessTokenCacheKeyContext context);
}
