package com.smbtech.serviceframework.starter.restclient.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smbtech.serviceframework.httpclient.port.in.HttpClientCatalog;
import com.smbtech.serviceframework.httpclient.port.in.HttpClientDefinitionValidator;
import com.smbtech.serviceframework.httpclient.port.out.AccessTokenProvider;
import com.smbtech.serviceframework.httpclient.port.out.CorrelationHeadersProvider;
import com.smbtech.serviceframework.httpclient.port.out.CredentialDefinitionSource;
import com.smbtech.serviceframework.httpclient.port.out.CredentialProvider;
import com.smbtech.serviceframework.httpclient.port.out.HttpClientDefinitionSource;
import com.smbtech.serviceframework.httpclient.port.out.HttpErrorResponseBodyReader;
import com.smbtech.serviceframework.httpclient.port.out.HttpExchangeAuditSink;
import com.smbtech.serviceframework.httpclient.port.out.KeyStoreDefinitionSource;
import com.smbtech.serviceframework.httpclient.service.DefaultHttpClientCatalog;
import com.smbtech.serviceframework.httpclient.service.DefaultHttpClientDefinitionValidator;
import com.smbtech.serviceframework.httpclient.service.ScopeValidator;
import com.smbtech.serviceframework.starter.restclient.adapter.out.apache.ApacheHttpClientConfigurator;
import com.smbtech.serviceframework.starter.restclient.adapter.out.apache.ConnectionReuseStrategyConfigurator;
import com.smbtech.serviceframework.starter.restclient.adapter.out.apache.HostnameVerifierConfigurator;
import com.smbtech.serviceframework.starter.restclient.adapter.out.apache.HttpClientConfigurator;
import com.smbtech.serviceframework.starter.restclient.adapter.out.apache.HttpClientConnectionManagerConfigurator;
import com.smbtech.serviceframework.starter.restclient.adapter.out.apache.KeepAliveStrategyConfigurator;
import com.smbtech.serviceframework.starter.restclient.adapter.out.apache.RegistryConfigurator;
import com.smbtech.serviceframework.starter.restclient.adapter.out.apache.RequestConfigConfigurator;
import com.smbtech.serviceframework.starter.restclient.adapter.out.apache.SocketConfigConfigurator;
import com.smbtech.serviceframework.starter.restclient.adapter.out.apache.SslContextFactory;
import com.smbtech.serviceframework.starter.restclient.adapter.out.apache.SslConnectionSocketFactoryConfigurator;
import com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.PropertiesCredentialDefinitionSource;
import com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.PropertiesCredentialProvider;
import com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.PropertiesKeyStoreDefinitionSource;
import com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.keystore.KeyStoreManager;
import com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.keystore.PrivateKeyLoader;
import com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring.ClientAssertionJwkResolver;
import com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring.GrantAwareOAuth2AuthorizedClientService;
import com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring.OAuth2AuthorizationContextAttributesMapper;
import com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring.SigningJwkResolver;
import com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring.SpringClientRegistrationResolver;
import com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring.SpringOAuth2TokenResponseClientFactory;
import com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring.SpringSecurityAuthorizedClientTokenClient;
import com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring.SpringSecurityJwtBearerAssertionResolver;
import com.smbtech.serviceframework.starter.restclient.adapter.out.error.HttpErrorResponseMapper;
import com.smbtech.serviceframework.starter.restclient.adapter.out.resilience.ResilienceStateRegistry;
import com.smbtech.serviceframework.starter.restclient.adapter.out.source.PropertiesHttpClientDefinitionSource;
import com.smbtech.serviceframework.starter.restclient.adapter.out.spring.ConfiguredRestClientFactory;
import com.smbtech.serviceframework.starter.restclient.adapter.out.spring.DefaultApiClientFactory;
import com.smbtech.serviceframework.starter.restclient.adapter.out.spring.DefaultRestClientRegistry;
import com.smbtech.serviceframework.starter.restclient.adapter.out.spring.MdcCorrelationHeadersProvider;
import com.smbtech.serviceframework.starter.restclient.adapter.out.spring.Slf4jHttpExchangeAuditSink;
import com.smbtech.serviceframework.starter.restclient.api.AccessTokenClient;
import com.smbtech.serviceframework.starter.restclient.api.ApiClientFactory;
import com.smbtech.serviceframework.starter.restclient.api.HttpErrorBodyDecoder;
import com.smbtech.serviceframework.starter.restclient.api.RestClientRegistry;
import com.smbtech.serviceframework.starter.restclient.api.customizer.ApacheHttpClientBuilderCustomizer;
import com.smbtech.serviceframework.starter.restclient.api.customizer.ClientHttpRequestFactoryCustomizer;
import com.smbtech.serviceframework.starter.restclient.api.customizer.RestClientBuilderCustomizer;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Role;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ClientCredentialsOAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.JwtBearerOAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.web.client.RestClient;

import javax.net.ssl.SSLContext;
import java.time.Clock;
import java.util.List;

import static org.springframework.beans.factory.config.BeanDefinition.ROLE_INFRASTRUCTURE;

@AutoConfiguration(afterName = {
        "org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration",
        "org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientAutoConfiguration",
        "org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration"
})
@ConditionalOnClass(RestClient.class)
@EnableConfigurationProperties(RestClientProperties.class)
public class RestClientAutoConfiguration {

    @Bean
    @Role(ROLE_INFRASTRUCTURE)
    static RestClientBeanRegistrar restClientBeanRegistrar() {
        return new RestClientBeanRegistrar();
    }

    @Bean
    static OAuth2AuthorizedClientServiceCachePolicyPostProcessor oAuth2AuthorizedClientServiceCachePolicyPostProcessor(
            ObjectProvider<ClientRegistrationRepository> clientRegistrationRepository,
            ObjectProvider<RestClientProperties> properties
    ) {
        return new OAuth2AuthorizedClientServiceCachePolicyPostProcessor(clientRegistrationRepository, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    RestClientPropertiesMapper restClientPropertiesMapper(CredentialResolver credentialResolver) {
        return new RestClientPropertiesMapper(credentialResolver);
    }

    @Bean
    @ConditionalOnMissingBean
    CredentialPropertiesMapper credentialPropertiesMapper() {
        return new CredentialPropertiesMapper();
    }

    @Bean
    @ConditionalOnMissingBean
    CredentialDefinitionSource credentialDefinitionSource(
            RestClientProperties properties,
            CredentialPropertiesMapper mapper
    ) {
        return new PropertiesCredentialDefinitionSource(properties, mapper);
    }

    @Bean
    @ConditionalOnMissingBean
    CredentialProvider credentialProvider(CredentialDefinitionSource source) {
        return new PropertiesCredentialProvider(source);
    }

    @Bean
    @ConditionalOnMissingBean
    CredentialResolver credentialResolver(CredentialProvider credentialProvider) {
        return new CredentialResolver(credentialProvider);
    }

    @Bean
    @ConditionalOnMissingBean
    KeyStorePropertiesMapper keyStorePropertiesMapper(CredentialResolver credentialResolver) {
        return new KeyStorePropertiesMapper(credentialResolver);
    }

    @Bean
    @ConditionalOnMissingBean
    RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    @ConditionalOnMissingBean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    @ConditionalOnMissingBean
    HttpClientDefinitionSource httpClientDefinitionSource(
            RestClientProperties properties,
            RestClientPropertiesMapper mapper
    ) {
        return new PropertiesHttpClientDefinitionSource(properties, mapper);
    }

    @Bean
    @ConditionalOnMissingBean
    HttpClientDefinitionValidator httpClientDefinitionValidator() {
        return new DefaultHttpClientDefinitionValidator();
    }

    @Bean
    @ConditionalOnMissingBean
    HttpClientCatalog httpClientCatalog(
            HttpClientDefinitionSource source,
            HttpClientDefinitionValidator validator
    ) {
        return new DefaultHttpClientCatalog(source, validator);
    }

    @Bean
    @ConditionalOnMissingBean
    KeyStoreDefinitionSource keyStoreDefinitionSource(
            RestClientProperties properties,
            KeyStorePropertiesMapper mapper
    ) {
        return new PropertiesKeyStoreDefinitionSource(properties, mapper);
    }

    @Bean
    @ConditionalOnMissingBean
    ScopeValidator scopeValidator() {
        return new ScopeValidator();
    }

    @Bean
    @ConditionalOnBean(ClientRegistrationRepository.class)
    @ConditionalOnMissingBean
    SpringClientRegistrationResolver springClientRegistrationResolver(
            ClientRegistrationRepository clientRegistrationRepository
    ) {
        return new SpringClientRegistrationResolver(clientRegistrationRepository);
    }

    @Bean
    @ConditionalOnMissingBean
    SigningJwkResolver signingJwkResolver(PrivateKeyLoader privateKeyLoader) {
        return new SigningJwkResolver(privateKeyLoader);
    }

    @Bean
    @ConditionalOnBean(SpringClientRegistrationResolver.class)
    @ConditionalOnMissingBean
    ClientAssertionJwkResolver clientAssertionJwkResolver(
            RestClientProperties properties,
            SigningJwkResolver signingJwkResolver
    ) {
        return new ClientAssertionJwkResolver(properties, signingJwkResolver);
    }

    @Bean
    @ConditionalOnBean(ClientAssertionJwkResolver.class)
    @ConditionalOnMissingBean
    SpringOAuth2TokenResponseClientFactory springOAuth2TokenResponseClientFactory(
            ClientAssertionJwkResolver clientAssertionJwkResolver,
            Clock clock
    ) {
        return new SpringOAuth2TokenResponseClientFactory(clientAssertionJwkResolver, clock);
    }

    @Bean
    @ConditionalOnBean(SpringOAuth2TokenResponseClientFactory.class)
    @ConditionalOnMissingBean
    OAuth2AuthorizedClientProvider springOAuth2AuthorizedClientProvider(
            SpringOAuth2TokenResponseClientFactory tokenResponseClientFactory,
            SpringSecurityJwtBearerAssertionResolver assertionResolver,
            Clock clock
    ) {
        ClientCredentialsOAuth2AuthorizedClientProvider clientCredentials =
                new ClientCredentialsOAuth2AuthorizedClientProvider();
        clientCredentials.setAccessTokenResponseClient(tokenResponseClientFactory.createClientCredentials());
        clientCredentials.setClock(clock);

        JwtBearerOAuth2AuthorizedClientProvider jwtBearer = new JwtBearerOAuth2AuthorizedClientProvider();
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
            Clock clock
    ) {
        return new SpringSecurityJwtBearerAssertionResolver(properties, signingJwkResolver, clock);
    }

    @Bean
    @ConditionalOnBean(ClientRegistrationRepository.class)
    @ConditionalOnMissingBean
    OAuth2AuthorizedClientService oAuth2AuthorizedClientService(
            ClientRegistrationRepository clientRegistrationRepository,
            RestClientProperties properties
    ) {
        OAuth2AuthorizedClientService delegate =
                new InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository);
        return new GrantAwareOAuth2AuthorizedClientService(
                clientRegistrationRepository,
                delegate,
                properties
        );
    }

    @Bean
    @ConditionalOnBean(OAuth2AuthorizedClientProvider.class)
    @ConditionalOnMissingBean
    OAuth2AuthorizedClientManager oAuth2AuthorizedClientManager(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientService authorizedClientService,
            OAuth2AuthorizedClientProvider authorizedClientProvider
    ) {
        AuthorizedClientServiceOAuth2AuthorizedClientManager manager =
                new AuthorizedClientServiceOAuth2AuthorizedClientManager(
                        clientRegistrationRepository,
                        authorizedClientService
                );
        manager.setAuthorizedClientProvider(authorizedClientProvider);
        manager.setContextAttributesMapper(new OAuth2AuthorizationContextAttributesMapper());
        return manager;
    }

    @Bean
    @ConditionalOnMissingBean
    KeyStoreManager keyStoreManager(
            KeyStoreDefinitionSource keyStoreDefinitionSource,
            ResourceLoader resourceLoader
    ) {
        return new KeyStoreManager(keyStoreDefinitionSource, resourceLoader);
    }

    @Bean
    @ConditionalOnMissingBean
    PrivateKeyLoader privateKeyLoader(KeyStoreManager keyStoreManager) {
        return new PrivateKeyLoader(keyStoreManager);
    }

    @Bean
    @ConditionalOnMissingBean({AccessTokenProvider.class, AccessTokenClient.class})
    SpringSecurityAuthorizedClientTokenClient springSecurityAuthorizedClientTokenClient(
            ObjectProvider<ClientRegistrationRepository> clientRegistrationRepository,
            ObjectProvider<OAuth2AuthorizedClientManager> authorizedClientManager,
            ScopeValidator scopeValidator
    ) {
        return new SpringSecurityAuthorizedClientTokenClient(
                clientRegistrationRepository.getIfAvailable(),
                authorizedClientManager.getIfAvailable(),
                scopeValidator
        );
    }

    @Bean
    @ConditionalOnMissingBean
    CorrelationHeadersProvider correlationHeadersProvider() {
        return new MdcCorrelationHeadersProvider();
    }

    @Bean
    @ConditionalOnMissingBean
    HttpExchangeAuditSink httpExchangeAuditSink() {
        return new Slf4jHttpExchangeAuditSink();
    }

    @Bean
    @ConditionalOnMissingBean
    HttpErrorResponseMapper httpErrorResponseMapper() {
        return new HttpErrorResponseMapper();
    }

    @Bean
    @ConditionalOnMissingBean
    HttpErrorBodyDecoder httpErrorBodyDecoder(ObjectProvider<ObjectMapper> objectMapper) {
        return new HttpErrorBodyDecoder(objectMapper.getIfAvailable(this::fallbackObjectMapper));
    }

    @Bean
    @ConditionalOnMissingBean
    ResilienceStateRegistry resilienceStateRegistry(Clock clock) {
        return new ResilienceStateRegistry(clock);
    }

    @Bean
    @ConditionalOnMissingBean
    HostnameVerifierConfigurator hostnameVerifierConfigurator() {
        return new HostnameVerifierConfigurator();
    }

    @Bean
    @ConditionalOnMissingBean
    SslContextFactory sslContextFactory(KeyStoreManager keyStoreManager) {
        return new SslContextFactory(keyStoreManager);
    }

    @Bean
    @ConditionalOnMissingBean
    SslConnectionSocketFactoryConfigurator sslConnectionSocketFactoryConfigurator(
            HostnameVerifierConfigurator hostnameVerifierConfigurator,
            SslContextFactory sslContextFactory,
            ObjectProvider<SSLContext> sslContext
    ) {
        return new SslConnectionSocketFactoryConfigurator(
                hostnameVerifierConfigurator,
                sslContextFactory,
                sslContext.getIfAvailable()
        );
    }

    @Bean
    @ConditionalOnMissingBean
    RegistryConfigurator registryConfigurator(
            SslConnectionSocketFactoryConfigurator sslConnectionSocketFactoryConfigurator
    ) {
        return new RegistryConfigurator(sslConnectionSocketFactoryConfigurator);
    }

    @Bean
    @ConditionalOnMissingBean
    SocketConfigConfigurator socketConfigConfigurator() {
        return new SocketConfigConfigurator();
    }

    @Bean
    @ConditionalOnMissingBean
    HttpClientConnectionManagerConfigurator httpClientConnectionManagerConfigurator(
            RegistryConfigurator registryConfigurator,
            SocketConfigConfigurator socketConfigConfigurator
    ) {
        return new HttpClientConnectionManagerConfigurator(registryConfigurator, socketConfigConfigurator);
    }

    @Bean
    @ConditionalOnMissingBean
    ConnectionReuseStrategyConfigurator connectionReuseStrategyConfigurator() {
        return new ConnectionReuseStrategyConfigurator();
    }

    @Bean
    @ConditionalOnMissingBean
    KeepAliveStrategyConfigurator keepAliveStrategyConfigurator() {
        return new KeepAliveStrategyConfigurator();
    }

    @Bean
    @ConditionalOnMissingBean
    RequestConfigConfigurator requestConfigConfigurator() {
        return new RequestConfigConfigurator();
    }

    @Bean
    @ConditionalOnMissingBean
    ApacheHttpClientConfigurator apacheHttpClientConfigurator(
            HttpClientConnectionManagerConfigurator connectionManagerConfigurator,
            ConnectionReuseStrategyConfigurator connectionReuseStrategyConfigurator,
            KeepAliveStrategyConfigurator keepAliveStrategyConfigurator,
            RequestConfigConfigurator requestConfigConfigurator,
            ObjectProvider<ApacheHttpClientBuilderCustomizer> customizers
    ) {
        return new ApacheHttpClientConfigurator(
                connectionManagerConfigurator,
                connectionReuseStrategyConfigurator,
                keepAliveStrategyConfigurator,
                requestConfigConfigurator,
                orderedList(customizers)
        );
    }

    @Bean
    @ConditionalOnMissingBean
    HttpClientConfigurator httpClientConfigurator(
            ApacheHttpClientConfigurator apacheHttpClientConfigurator,
            ObjectProvider<ClientHttpRequestFactoryCustomizer> customizers
    ) {
        return new HttpClientConfigurator(apacheHttpClientConfigurator, orderedList(customizers));
    }

    @Bean
    @ConditionalOnMissingBean
    ConfiguredRestClientFactory configuredRestClientFactory(
            RestClient.Builder restClientBuilder,
            ObjectProvider<OAuth2AuthorizedClientManager> authorizedClientManager,
            ObjectProvider<OAuth2AuthorizedClientService> authorizedClientService,
            CorrelationHeadersProvider correlationHeadersProvider,
            HttpExchangeAuditSink auditSink,
            HttpClientConfigurator httpClientConfigurator,
            HttpErrorResponseMapper errorResponseMapper,
            HttpErrorResponseBodyReader errorResponseBodyReader,
            ResilienceStateRegistry resilienceStateRegistry,
            ObjectProvider<MeterRegistry> meterRegistry,
            ObjectProvider<RestClientBuilderCustomizer> customizers
    ) {
        return new ConfiguredRestClientFactory(
                restClientBuilder,
                authorizedClientManager.getIfAvailable(),
                authorizedClientService.getIfAvailable(),
                correlationHeadersProvider,
                auditSink,
                httpClientConfigurator,
                errorResponseMapper,
                errorResponseBodyReader,
                resilienceStateRegistry,
                meterRegistry.getIfAvailable(),
                orderedList(customizers)
        );
    }

    @Bean
    @ConditionalOnMissingBean
    RestClientRegistry restClientRegistry(
            HttpClientCatalog catalog,
            ConfiguredRestClientFactory factory
    ) {
        return new DefaultRestClientRegistry(catalog, factory);
    }

    @Bean
    @ConditionalOnMissingBean
    ApiClientFactory apiClientFactory(RestClientRegistry restClientRegistry) {
        return new DefaultApiClientFactory(restClientRegistry);
    }

    private <T> List<T> orderedList(ObjectProvider<T> provider) {
        return provider.orderedStream().toList();
    }

    private ObjectMapper fallbackObjectMapper() {
        return new ObjectMapper().findAndRegisterModules();
    }
}
