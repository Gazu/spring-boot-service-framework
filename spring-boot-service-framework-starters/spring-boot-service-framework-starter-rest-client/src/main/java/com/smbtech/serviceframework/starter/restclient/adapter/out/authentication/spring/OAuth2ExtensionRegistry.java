package com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring;

import com.smbtech.serviceframework.starter.restclient.api.oauth2.AccessTokenCacheKeyResolver;
import com.smbtech.serviceframework.starter.restclient.api.oauth2.ClientAssertionCustomizer;
import com.smbtech.serviceframework.starter.restclient.api.oauth2.JwtBearerClaimsContributor;
import com.smbtech.serviceframework.starter.restclient.api.oauth2.OAuth2TokenRequestCustomizer;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Provides OAuth2 extension registry behavior. */
final class OAuth2ExtensionRegistry {

    private static final OAuth2ExtensionRegistry EMPTY =
            new OAuth2ExtensionRegistry(List.of(), List.of(), List.of(), null);

    private final List<JwtBearerClaimsContributor> jwtBearerClaimsContributors;
    private final List<ClientAssertionCustomizer> clientAssertionCustomizers;
    private final List<OAuth2TokenRequestCustomizer> tokenRequestCustomizers;
    private final AccessTokenCacheKeyResolver accessTokenCacheKeyResolver;

    /**
     * Creates a OAuth2 extension registry instance.
     *
     * @param jwtBearerClaimsContributors JWT bearer claims contributors value
     * @param clientAssertionCustomizers client assertion customizers value
     * @param tokenRequestCustomizers token request customizers value
     * @param accessTokenCacheKeyResolver access token cache key resolver value
     */
    public OAuth2ExtensionRegistry(
            List<JwtBearerClaimsContributor> jwtBearerClaimsContributors,
            List<ClientAssertionCustomizer> clientAssertionCustomizers,
            List<OAuth2TokenRequestCustomizer> tokenRequestCustomizers,
            AccessTokenCacheKeyResolver accessTokenCacheKeyResolver) {
        this.jwtBearerClaimsContributors =
                List.copyOf(Objects.requireNonNullElse(jwtBearerClaimsContributors, List.of()));
        this.clientAssertionCustomizers =
                List.copyOf(Objects.requireNonNullElse(clientAssertionCustomizers, List.of()));
        this.tokenRequestCustomizers =
                List.copyOf(Objects.requireNonNullElse(tokenRequestCustomizers, List.of()));
        this.accessTokenCacheKeyResolver = accessTokenCacheKeyResolver;
    }

    /**
     * Performs the empty operation.
     *
     * @return empty result
     */
    public static OAuth2ExtensionRegistry empty() {
        return EMPTY;
    }

    /**
     * Performs the JWT bearer claims contributors operation.
     *
     * @return JWT bearer claims contributors result
     */
    public List<JwtBearerClaimsContributor> jwtBearerClaimsContributors() {
        return jwtBearerClaimsContributors;
    }

    /**
     * Performs the client assertion customizers operation.
     *
     * @return client assertion customizers result
     */
    public List<ClientAssertionCustomizer> clientAssertionCustomizers() {
        return clientAssertionCustomizers;
    }

    /**
     * Performs the token request customizers operation.
     *
     * @return token request customizers result
     */
    public List<OAuth2TokenRequestCustomizer> tokenRequestCustomizers() {
        return tokenRequestCustomizers;
    }

    /**
     * Performs the access token cache key resolver operation.
     *
     * @return access token cache key resolver result
     */
    public Optional<AccessTokenCacheKeyResolver> accessTokenCacheKeyResolver() {
        return Optional.ofNullable(accessTokenCacheKeyResolver);
    }

    /**
     * Reports whether empty.
     *
     * @return is empty result
     */
    public boolean isEmpty() {
        return jwtBearerClaimsContributors.isEmpty()
                && clientAssertionCustomizers.isEmpty()
                && tokenRequestCustomizers.isEmpty()
                && accessTokenCacheKeyResolver == null;
    }
}
