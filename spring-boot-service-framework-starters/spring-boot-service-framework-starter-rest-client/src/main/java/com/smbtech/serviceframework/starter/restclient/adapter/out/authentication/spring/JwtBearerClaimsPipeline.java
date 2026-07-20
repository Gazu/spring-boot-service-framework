package com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring;

import com.smbtech.serviceframework.httpclient.domain.HttpClientDefinition;
import com.smbtech.serviceframework.httpclient.domain.TokenRequestDefinition;
import com.smbtech.serviceframework.starter.restclient.api.oauth2.JwtBearerClaimsContext;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.security.oauth2.client.OAuth2AuthorizationContext;
import org.springframework.security.oauth2.client.registration.ClientRegistration;

/** Provides JWT bearer claims pipeline behavior. */
public final class JwtBearerClaimsPipeline {

    private final OAuth2ExtensionRegistry extensionRegistry;
    private final Set<String> blockedClaims;

    /**
     * Creates a JWT bearer claims pipeline instance.
     *
     * @param extensionRegistry extension registry value
     * @param blockedClaims blocked claims value
     */
    public JwtBearerClaimsPipeline(
            OAuth2ExtensionRegistry extensionRegistry, Set<String> blockedClaims) {
        this.extensionRegistry =
                Objects.requireNonNullElseGet(extensionRegistry, OAuth2ExtensionRegistry::empty);
        this.blockedClaims = Set.copyOf(Objects.requireNonNullElse(blockedClaims, Set.of()));
    }

    Map<String, Object> resolveForTokenClient(
            ClientRegistration registration,
            String expectedScopes,
            Map<String, Object> requestContextClaims,
            Map<String, Object> explicitClaims) {
        return resolve(
                new JwtBearerClaimsContext(
                        registration.getRegistrationId(),
                        registration.getClientId(),
                        tokenUri(registration),
                        registration.getScopes(),
                        expectedScopes,
                        Map.of(),
                        requestContextClaims,
                        explicitClaims));
    }

    /**
     * Resolves for rest client.
     *
     * @param definition definition value
     * @param requestContextClaims request context claims value
     * @param explicitClaims explicit claims value
     * @return resolve for rest client result
     */
    public Map<String, Object> resolveForRestClient(
            HttpClientDefinition definition,
            Map<String, Object> requestContextClaims,
            Map<String, Object> explicitClaims) {
        return resolve(
                new JwtBearerClaimsContext(
                        definition.tokenRequestId(),
                        "",
                        null,
                        Set.of(),
                        definition.scopes(),
                        Map.of(),
                        requestContextClaims,
                        explicitClaims));
    }

    Map<String, Object> resolveForAssertion(
            TokenRequestDefinition definition, OAuth2AuthorizationContext context) {
        Map<String, Object> dynamicClaims =
                context == null ? Map.of() : JwtBearerCustomClaimsResolver.dynamicClaims(context);
        return resolve(
                new JwtBearerClaimsContext(
                        definition.id(),
                        definition.clientId(),
                        definition.tokenUri(),
                        definition.scopes(),
                        "",
                        definition.jwtBearer().customClaims(),
                        Map.of(),
                        dynamicClaims));
    }

    Map<String, Object> resolve(JwtBearerClaimsContext context) {
        LinkedHashMap<String, Object> claims = new LinkedHashMap<>();
        claims.putAll(
                JwtBearerCustomClaimsResolver.sanitize(context.configuredClaims(), blockedClaims));
        claims.putAll(
                JwtBearerCustomClaimsResolver.sanitize(
                        context.requestContextClaims(), blockedClaims));
        claims.putAll(
                JwtBearerCustomClaimsResolver.sanitize(context.explicitClaims(), blockedClaims));

        JwtBearerClaimsContext contributorContext =
                new JwtBearerClaimsContext(
                        context.registrationId(),
                        context.clientId(),
                        context.tokenUri(),
                        context.requestedScopes(),
                        context.expectedScopes(),
                        Map.copyOf(claims),
                        JwtBearerCustomClaimsResolver.sanitize(
                                context.requestContextClaims(), blockedClaims),
                        JwtBearerCustomClaimsResolver.sanitize(
                                context.explicitClaims(), blockedClaims));
        extensionRegistry
                .jwtBearerClaimsContributors()
                .forEach(
                        contributor ->
                                claims.putAll(
                                        JwtBearerCustomClaimsResolver.sanitize(
                                                contributor.contribute(contributorContext),
                                                blockedClaims)));
        return JwtBearerCustomClaimsResolver.sanitize(claims, blockedClaims);
    }

    private URI tokenUri(ClientRegistration registration) {
        String tokenUri = registration.getProviderDetails().getTokenUri();
        if (tokenUri == null || tokenUri.isBlank()) {
            return null;
        }
        return URI.create(tokenUri);
    }
}
