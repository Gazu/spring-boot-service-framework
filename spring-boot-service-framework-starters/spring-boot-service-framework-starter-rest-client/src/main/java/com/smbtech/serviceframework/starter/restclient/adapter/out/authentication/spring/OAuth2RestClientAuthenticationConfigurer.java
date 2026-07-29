package com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring;

import com.smbtech.serviceframework.httpclient.domain.AuthenticationType;
import com.smbtech.serviceframework.httpclient.domain.GrantType;
import com.smbtech.serviceframework.httpclient.domain.HttpClientDefinition;
import com.smbtech.serviceframework.httpclient.exception.HttpClientAuthenticationException;
import com.smbtech.serviceframework.starter.restclient.api.RequestContextManager;
import com.smbtech.serviceframework.starter.restclient.api.customizer.RestClientAuthenticationConfigurer;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;
import org.springframework.security.oauth2.client.web.client.RequestAttributeClientRegistrationIdResolver;
import org.springframework.security.oauth2.client.web.client.RequestAttributePrincipalResolver;
import org.springframework.web.client.RestClient;

/** Applies Spring Security OAuth2 authentication to configured REST clients. */
public final class OAuth2RestClientAuthenticationConfigurer
        implements RestClientAuthenticationConfigurer {

    private static final String PRINCIPAL_NAME = "spring-boot-service-framework";

    private final RequestContextManager requestContextManager;
    private final boolean requestContextJwtBearerClaimsEnabled;
    private final Set<String> blockedJwtBearerClaims;
    private final JwtBearerClaimsPipeline jwtBearerClaimsPipeline;
    private final AccessTokenCacheKeyPipeline accessTokenCacheKeyPipeline;
    private final ClientHttpRequestInterceptor interceptor;

    /**
     * Creates the Spring Security OAuth2 authentication configurer.
     *
     * @param authorizedClientManager authorized client manager
     * @param authorizedClientService authorized client service
     * @param requestContextManager request context manager
     * @param requestContextJwtBearerClaimsEnabled whether contextual JWT claims are enabled
     * @param blockedJwtBearerClaims claims that cannot be propagated
     * @param extensionRegistry OAuth2 extension registry
     */
    public OAuth2RestClientAuthenticationConfigurer(
            OAuth2AuthorizedClientManager authorizedClientManager,
            OAuth2AuthorizedClientService authorizedClientService,
            RequestContextManager requestContextManager,
            boolean requestContextJwtBearerClaimsEnabled,
            Set<String> blockedJwtBearerClaims,
            OAuth2ExtensionRegistry extensionRegistry) {
        this.requestContextManager = requestContextManager;
        this.requestContextJwtBearerClaimsEnabled = requestContextJwtBearerClaimsEnabled;
        this.blockedJwtBearerClaims =
                Set.copyOf(Objects.requireNonNullElse(blockedJwtBearerClaims, Set.of()));
        OAuth2ExtensionRegistry safeExtensionRegistry =
                Objects.requireNonNull(extensionRegistry, "extensionRegistry must not be null");
        this.jwtBearerClaimsPipeline =
                new JwtBearerClaimsPipeline(safeExtensionRegistry, this.blockedJwtBearerClaims);
        this.accessTokenCacheKeyPipeline = new AccessTokenCacheKeyPipeline(safeExtensionRegistry);
        this.interceptor = oauth2Interceptor(authorizedClientManager, authorizedClientService);
    }

    @Override
    public boolean supports(AuthenticationType authenticationType) {
        return authenticationType == AuthenticationType.CLIENT_CREDENTIALS
                || authenticationType == AuthenticationType.JWT_BEARER;
    }

    @Override
    public void configure(HttpClientDefinition definition, RestClient.Builder builder) {
        if (!supports(definition.authenticationType())) {
            throw new HttpClientAuthenticationException(
                    "Unsupported OAuth2 authentication type for HTTP client: " + definition.name());
        }
        builder.defaultRequest(
                        request ->
                                request.attributes(
                                        attributes -> configureAttributes(definition, attributes)))
                .requestInterceptor(interceptor);
    }

    private void configureAttributes(
            HttpClientDefinition definition, Map<String, Object> attributes) {
        RequestAttributeClientRegistrationIdResolver.clientRegistrationId(
                        definition.tokenRequestId())
                .accept(attributes);
        if (requestContextJwtBearerClaimsEnabled
                && definition.authenticationType() == AuthenticationType.JWT_BEARER) {
            addJwtBearerRequestContextAttributes(definition, attributes);
            return;
        }
        RequestAttributePrincipalResolver.principal(
                        accessTokenCacheKeyPipeline.resolve(
                                definition.tokenRequestId(),
                                grantType(definition),
                                PRINCIPAL_NAME,
                                scopes(definition),
                                Map.of()))
                .accept(attributes);
    }

    private void addJwtBearerRequestContextAttributes(
            HttpClientDefinition definition, Map<String, Object> attributes) {
        Map<String, Object> requestContextClaims =
                requestContextManager == null
                        ? Map.of()
                        : requestContextManager.currentJwtBearerClaims();
        Map<String, Object> customClaims =
                jwtBearerClaimsPipeline.resolveForRestClient(
                        definition, requestContextClaims, Map.of());
        Map<String, Object> authorizationAttributes =
                JwtBearerAuthorizationAttributes.authorizationAttributes(
                        customClaims, blockedJwtBearerClaims);
        attributes.putAll(authorizationAttributes);
        String principalName =
                JwtBearerAuthorizationAttributes.cachePrincipalName(
                        PRINCIPAL_NAME, customClaims, blockedJwtBearerClaims);
        RequestAttributePrincipalResolver.principal(
                        accessTokenCacheKeyPipeline.resolve(
                                definition.tokenRequestId(),
                                GrantType.JWT_BEARER,
                                principalName,
                                scopes(definition),
                                authorizationAttributes))
                .accept(attributes);
    }

    private Set<String> scopes(HttpClientDefinition definition) {
        String scopes = definition.scopes();
        if (scopes == null || scopes.isBlank()) {
            return Set.of();
        }
        return Stream.of(scopes.split("[,\\s]+"))
                .map(String::trim)
                .filter(scope -> !scope.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }

    private GrantType grantType(HttpClientDefinition definition) {
        return definition.authenticationType() == AuthenticationType.JWT_BEARER
                ? GrantType.JWT_BEARER
                : GrantType.CLIENT_CREDENTIALS;
    }

    private ClientHttpRequestInterceptor oauth2Interceptor(
            OAuth2AuthorizedClientManager authorizedClientManager,
            OAuth2AuthorizedClientService authorizedClientService) {
        OAuth2ClientHttpRequestInterceptor oauth2Interceptor =
                new OAuth2ClientHttpRequestInterceptor(
                        Objects.requireNonNull(
                                authorizedClientManager,
                                "authorizedClientManager must not be null"));
        RequestAttributeClientRegistrationIdResolver registrationIdResolver =
                new RequestAttributeClientRegistrationIdResolver();
        oauth2Interceptor.setClientRegistrationIdResolver(
                request -> {
                    if (request.getHeaders().containsHeader(HttpHeaders.AUTHORIZATION)) {
                        return null;
                    }
                    return registrationIdResolver.resolve(request);
                });
        oauth2Interceptor.setPrincipalResolver(new RequestAttributePrincipalResolver());
        if (authorizedClientService != null) {
            oauth2Interceptor.setAuthorizationFailureHandler(
                    OAuth2ClientHttpRequestInterceptor.authorizationFailureHandler(
                            authorizedClientService));
        }
        return oauth2Interceptor;
    }
}
