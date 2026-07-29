package com.smbtech.serviceframework.starter.restclient.autoconfigure;

import com.smbtech.serviceframework.httpclient.port.out.AccessTokenProvider;
import com.smbtech.serviceframework.httpclient.service.ScopeValidator;
import com.smbtech.serviceframework.logging.port.in.StructuredLoggerFactory;
import com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.keystore.KeyStoreManager;
import com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.keystore.PrivateKeyLoader;
import com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring.ClientAssertionJwkResolver;
import com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring.GrantAwareOAuth2AuthorizedClientService;
import com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring.OAuth2AuthorizationContextAttributesMapper;
import com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring.OAuth2ExtensionRegistry;
import com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring.OAuth2RestClientConfigurationValidationRunner;
import com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring.OAuth2RestClientConfigurationValidator;
import com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring.OAuth2TokenDiagnosticSanitizer;
import com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring.OAuth2TokenDiagnosticsLogger;
import com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring.SigningJwkResolver;
import com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring.SpringClientRegistrationResolver;
import com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring.SpringOAuth2TokenResponseClientFactory;
import com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring.SpringSecurityAuthorizedClientTokenClient;
import com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring.SpringSecurityJwtBearerAssertionResolver;
import com.smbtech.serviceframework.starter.restclient.adapter.out.logging.Slf4jStructuredLoggerFactory;
import com.smbtech.serviceframework.starter.restclient.api.AccessTokenClient;
import com.smbtech.serviceframework.starter.restclient.api.RequestContextManager;
import com.smbtech.serviceframework.starter.restclient.api.oauth2.AccessTokenCacheKeyResolver;
import com.smbtech.serviceframework.starter.restclient.api.oauth2.ClientAssertionCustomizer;
import com.smbtech.serviceframework.starter.restclient.api.oauth2.JwtBearerClaimsContributor;
import com.smbtech.serviceframework.starter.restclient.api.oauth2.OAuth2TokenRequestCustomizer;
import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ClientCredentialsOAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.JwtBearerOAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

/** Activates Spring Security OAuth2 support when the OAuth2 client is present. */
@AutoConfiguration(
        after = RestClientAutoConfiguration.class,
        afterName = {
            "org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration",
            "org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientAutoConfiguration",
            "org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration"
        })
@ConditionalOnClass(OAuth2AuthorizedClientManager.class)
public class OAuth2RestClientAutoConfiguration {

    /** Creates an OAuth2 REST client auto-configuration instance. */
    public OAuth2RestClientAutoConfiguration() {}

    @Bean
    static OAuth2AuthorizedClientServiceCachePolicyPostProcessor
            oAuth2AuthorizedClientServiceCachePolicyPostProcessor(
                    ObjectProvider<ClientRegistrationRepository> clientRegistrationRepository,
                    ObjectProvider<RestClientProperties> properties) {
        return new OAuth2AuthorizedClientServiceCachePolicyPostProcessor(
                clientRegistrationRepository, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    ScopeValidator scopeValidator() {
        return new ScopeValidator();
    }

    @Bean
    @ConditionalOnMissingBean
    OAuth2TokenDiagnosticSanitizer oAuth2TokenDiagnosticSanitizer() {
        return new OAuth2TokenDiagnosticSanitizer();
    }

    @Bean
    @ConditionalOnMissingBean
    OAuth2TokenDiagnosticsLogger oAuth2TokenDiagnosticsLogger(
            RestClientProperties properties,
            ObjectProvider<StructuredLoggerFactory> structuredLoggerFactory,
            OAuth2TokenDiagnosticSanitizer sanitizer) {
        StructuredLoggerFactory loggerFactory =
                structuredLoggerFactory.getIfAvailable(Slf4jStructuredLoggerFactory::new);
        RestClientProperties.Authentication authentication =
                properties.getAuthentication() == null
                        ? new RestClientProperties.Authentication()
                        : properties.getAuthentication();
        return new OAuth2TokenDiagnosticsLogger(
                loggerFactory.get(OAuth2TokenDiagnosticsLogger.class),
                authentication.getDiagnostics(),
                sanitizer);
    }

    @Bean
    @ConditionalOnMissingBean
    SigningJwkResolver signingJwkResolver(PrivateKeyLoader privateKeyLoader) {
        return new SigningJwkResolver(privateKeyLoader);
    }

    @Bean
    @ConditionalOnMissingBean
    PrivateKeyLoader privateKeyLoader(KeyStoreManager keyStoreManager) {
        return new PrivateKeyLoader(keyStoreManager);
    }

    @Bean
    @ConditionalOnMissingBean
    OAuth2RestClientConfigurationValidator oAuth2RestClientConfigurationValidator(
            ObjectProvider<KeyStoreManager> keyStoreManager,
            ObjectProvider<SigningJwkResolver> signingJwkResolver) {
        return new OAuth2RestClientConfigurationValidator(
                keyStoreManager.getIfAvailable(), signingJwkResolver.getIfAvailable());
    }

    @Bean
    @ConditionalOnMissingBean
    OAuth2ExtensionRegistry oAuth2ExtensionRegistry(
            ObjectProvider<JwtBearerClaimsContributor> jwtBearerClaimsContributors,
            ObjectProvider<ClientAssertionCustomizer> clientAssertionCustomizers,
            ObjectProvider<OAuth2TokenRequestCustomizer> tokenRequestCustomizers,
            ObjectProvider<AccessTokenCacheKeyResolver> accessTokenCacheKeyResolver) {
        return new OAuth2ExtensionRegistry(
                orderedList(jwtBearerClaimsContributors),
                orderedList(clientAssertionCustomizers),
                orderedList(tokenRequestCustomizers),
                accessTokenCacheKeyResolver.getIfAvailable());
    }

    @Bean
    @ConditionalOnMissingBean
    OAuth2RestClientConfigurationValidationRunner oAuth2RestClientConfigurationValidationRunner(
            RestClientProperties properties,
            OAuth2RestClientConfigurationValidator validator,
            ObjectProvider<ClientRegistrationRepository> clientRegistrationRepository,
            ObjectProvider<StructuredLoggerFactory> structuredLoggerFactory) {
        StructuredLoggerFactory loggerFactory =
                structuredLoggerFactory.getIfAvailable(Slf4jStructuredLoggerFactory::new);
        return new OAuth2RestClientConfigurationValidationRunner(
                properties,
                validator,
                clientRegistrationRepository,
                loggerFactory.get(OAuth2RestClientConfigurationValidationRunner.class));
    }

    @Bean
    @ConditionalOnBean(ClientRegistrationRepository.class)
    @ConditionalOnMissingBean
    SpringClientRegistrationResolver springClientRegistrationResolver(
            ClientRegistrationRepository clientRegistrationRepository) {
        return new SpringClientRegistrationResolver(clientRegistrationRepository);
    }

    @Bean
    @ConditionalOnBean(SpringClientRegistrationResolver.class)
    @ConditionalOnMissingBean
    ClientAssertionJwkResolver clientAssertionJwkResolver(
            RestClientProperties properties, SigningJwkResolver signingJwkResolver) {
        return new ClientAssertionJwkResolver(properties, signingJwkResolver);
    }

    @Bean
    @ConditionalOnBean(ClientAssertionJwkResolver.class)
    @ConditionalOnMissingBean
    SpringOAuth2TokenResponseClientFactory springOAuth2TokenResponseClientFactory(
            ClientAssertionJwkResolver clientAssertionJwkResolver,
            OAuth2TokenDiagnosticsLogger diagnosticsLogger,
            Clock clock,
            OAuth2ExtensionRegistry extensionRegistry) {
        return new SpringOAuth2TokenResponseClientFactory(
                clientAssertionJwkResolver, diagnosticsLogger, clock, extensionRegistry);
    }

    @Bean
    @ConditionalOnBean(SpringOAuth2TokenResponseClientFactory.class)
    @ConditionalOnMissingBean
    OAuth2AuthorizedClientProvider springOAuth2AuthorizedClientProvider(
            SpringOAuth2TokenResponseClientFactory tokenResponseClientFactory,
            SpringSecurityJwtBearerAssertionResolver assertionResolver,
            Clock clock) {
        ClientCredentialsOAuth2AuthorizedClientProvider clientCredentials =
                new ClientCredentialsOAuth2AuthorizedClientProvider();
        clientCredentials.setAccessTokenResponseClient(
                tokenResponseClientFactory.createClientCredentials());
        clientCredentials.setClock(clock);

        JwtBearerOAuth2AuthorizedClientProvider jwtBearer =
                new JwtBearerOAuth2AuthorizedClientProvider();
        jwtBearer.setAccessTokenResponseClient(tokenResponseClientFactory.createJwtBearer());
        jwtBearer.setJwtAssertionResolver(assertionResolver::createAssertion);
        jwtBearer.setClock(clock);

        return OAuth2AuthorizedClientProviderBuilder.builder()
                .provider(clientCredentials)
                .provider(jwtBearer)
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    SpringSecurityJwtBearerAssertionResolver springSecurityJwtBearerAssertionResolver(
            RestClientProperties properties,
            SigningJwkResolver signingJwkResolver,
            OAuth2TokenDiagnosticsLogger diagnosticsLogger,
            Clock clock,
            OAuth2ExtensionRegistry extensionRegistry) {
        return new SpringSecurityJwtBearerAssertionResolver(
                properties, signingJwkResolver, diagnosticsLogger, clock, extensionRegistry);
    }

    @Bean
    @ConditionalOnBean(ClientRegistrationRepository.class)
    @ConditionalOnMissingBean
    OAuth2AuthorizedClientService oAuth2AuthorizedClientService(
            ClientRegistrationRepository clientRegistrationRepository,
            RestClientProperties properties,
            OAuth2TokenDiagnosticsLogger diagnosticsLogger) {
        OAuth2AuthorizedClientService delegate =
                new InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository);
        return new GrantAwareOAuth2AuthorizedClientService(
                clientRegistrationRepository, delegate, properties, diagnosticsLogger);
    }

    @Bean
    @ConditionalOnBean(OAuth2AuthorizedClientProvider.class)
    @ConditionalOnMissingBean
    OAuth2AuthorizedClientManager oAuth2AuthorizedClientManager(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientService authorizedClientService,
            OAuth2AuthorizedClientProvider authorizedClientProvider,
            RestClientProperties properties,
            RequestContextManager requestContextManager,
            OAuth2ExtensionRegistry extensionRegistry) {
        AuthorizedClientServiceOAuth2AuthorizedClientManager manager =
                new AuthorizedClientServiceOAuth2AuthorizedClientManager(
                        clientRegistrationRepository, authorizedClientService);
        manager.setAuthorizedClientProvider(authorizedClientProvider);
        manager.setContextAttributesMapper(
                new OAuth2AuthorizationContextAttributesMapper(
                        requestContextManager,
                        requestContextJwtBearerClaimsEnabled(properties),
                        blockedJwtBearerClaims(properties),
                        extensionRegistry));
        return manager;
    }

    @Bean
    @ConditionalOnMissingBean({AccessTokenProvider.class, AccessTokenClient.class})
    SpringSecurityAuthorizedClientTokenClient springSecurityAuthorizedClientTokenClient(
            ObjectProvider<ClientRegistrationRepository> clientRegistrationRepository,
            ObjectProvider<OAuth2AuthorizedClientManager> authorizedClientManager,
            ScopeValidator scopeValidator,
            OAuth2TokenDiagnosticsLogger diagnosticsLogger,
            RestClientProperties properties,
            RequestContextManager requestContextManager,
            OAuth2ExtensionRegistry extensionRegistry) {
        return new SpringSecurityAuthorizedClientTokenClient(
                clientRegistrationRepository.getIfAvailable(),
                authorizedClientManager.getIfAvailable(),
                scopeValidator,
                diagnosticsLogger,
                requestContextManager,
                requestContextJwtBearerClaimsEnabled(properties),
                blockedJwtBearerClaims(properties),
                extensionRegistry);
    }

    private static <T> List<T> orderedList(ObjectProvider<T> provider) {
        return provider.orderedStream().toList();
    }

    private static boolean requestContextJwtBearerClaimsEnabled(RestClientProperties properties) {
        RestClientProperties.RequestContext requestContext = requestContext(properties);
        return requestContext.isEnabled() && requestContext.isJwtBearerClaims();
    }

    private static Set<String> blockedJwtBearerClaims(RestClientProperties properties) {
        return Set.copyOf(
                Objects.requireNonNullElse(
                        requestContext(properties).getBlockedJwtBearerClaims(), Set.of()));
    }

    private static RestClientProperties.RequestContext requestContext(
            RestClientProperties properties) {
        if (properties == null || properties.getRequestContext() == null) {
            return new RestClientProperties.RequestContext();
        }
        return properties.getRequestContext();
    }
}
