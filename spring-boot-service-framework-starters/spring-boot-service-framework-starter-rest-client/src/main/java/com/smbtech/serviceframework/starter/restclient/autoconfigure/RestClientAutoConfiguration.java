package com.smbtech.serviceframework.starter.restclient.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smbtech.serviceframework.httpclient.port.in.HttpClientCatalog;
import com.smbtech.serviceframework.httpclient.port.in.HttpClientDefinitionValidator;
import com.smbtech.serviceframework.httpclient.port.out.AccessTokenProvider;
import com.smbtech.serviceframework.httpclient.port.out.CorrelationHeadersProvider;
import com.smbtech.serviceframework.httpclient.port.out.CredentialDefinitionSource;
import com.smbtech.serviceframework.httpclient.port.out.CredentialProvider;
import com.smbtech.serviceframework.httpclient.port.out.HttpClientDefinitionSource;
import com.smbtech.serviceframework.httpclient.port.out.HttpExchangeAuditSink;
import com.smbtech.serviceframework.httpclient.port.out.JwtAssertionProvider;
import com.smbtech.serviceframework.httpclient.port.out.KeyStoreDefinitionSource;
import com.smbtech.serviceframework.httpclient.port.out.TokenCache;
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
import com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.CachedAccessTokenProvider;
import com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.DefaultAccessTokenClient;
import com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.InMemoryTokenCache;
import com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.OAuth2AccessTokenProvider;
import com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.PropertiesCredentialDefinitionSource;
import com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.PropertiesCredentialProvider;
import com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.PropertiesKeyStoreDefinitionSource;
import com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.jwt.JwtAssertionFactory;
import com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.jwt.JwtBearerAccessTokenProvider;
import com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.keystore.KeyStoreManager;
import com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.keystore.PrivateKeyLoader;
import com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.keystore.RsaKeyFactory;
import com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring.SpringClientRegistrationResolver;
import com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring.ClientAssertionJwkResolver;
import com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring.SpringClientCredentialsTokenResponseClientFactory;
import com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring.SpringSecurityClientCredentialsAccessTokenProvider;
import com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring.SpringSecurityJwtBearerAccessTokenProvider;
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
    TokenCache tokenCache() {
        return new InMemoryTokenCache();
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
    @ConditionalOnBean(SpringClientRegistrationResolver.class)
    @ConditionalOnMissingBean
    ClientAssertionJwkResolver clientAssertionJwkResolver(
            RestClientProperties properties,
            KeyStoreManager keyStoreManager,
            PrivateKeyLoader privateKeyLoader
    ) {
        return new ClientAssertionJwkResolver(properties, keyStoreManager, privateKeyLoader);
    }

    @Bean
    @ConditionalOnBean(ClientAssertionJwkResolver.class)
    @ConditionalOnMissingBean
    SpringClientCredentialsTokenResponseClientFactory springClientCredentialsTokenResponseClientFactory(
            ClientAssertionJwkResolver clientAssertionJwkResolver,
            Clock clock
    ) {
        return new SpringClientCredentialsTokenResponseClientFactory(clientAssertionJwkResolver, clock);
    }

    @Bean
    @ConditionalOnBean(SpringClientCredentialsTokenResponseClientFactory.class)
    @ConditionalOnMissingBean
    SpringSecurityClientCredentialsAccessTokenProvider springSecurityClientCredentialsAccessTokenProvider(
            SpringClientRegistrationResolver springClientRegistrationResolver,
            SpringClientCredentialsTokenResponseClientFactory tokenResponseClientFactory,
            Clock clock
    ) {
        return new SpringSecurityClientCredentialsAccessTokenProvider(
                springClientRegistrationResolver,
                tokenResponseClientFactory,
                clock
        );
    }

    @Bean
    @ConditionalOnBean(SpringClientRegistrationResolver.class)
    @ConditionalOnMissingBean
    SpringSecurityJwtBearerAccessTokenProvider springSecurityJwtBearerAccessTokenProvider(
            SpringClientRegistrationResolver springClientRegistrationResolver,
            RestClientProperties properties,
            OAuth2AccessTokenProvider oauth2AccessTokenProvider
    ) {
        return new SpringSecurityJwtBearerAccessTokenProvider(
                springClientRegistrationResolver,
                properties,
                oauth2AccessTokenProvider
        );
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
    @ConditionalOnMissingBean
    RsaKeyFactory rsaKeyFactory(PrivateKeyLoader privateKeyLoader) {
        return new RsaKeyFactory(privateKeyLoader);
    }

    @Bean
    @ConditionalOnMissingBean
    JwtAssertionFactory jwtAssertionFactory(RsaKeyFactory rsaKeyFactory) {
        return new JwtAssertionFactory(rsaKeyFactory);
    }

    @Bean
    @ConditionalOnMissingBean
    JwtAssertionProvider jwtAssertionProvider(
            JwtAssertionFactory jwtAssertionFactory,
            Clock clock
    ) {
        return new JwtBearerAccessTokenProvider(jwtAssertionFactory, clock);
    }

    @Bean
    @ConditionalOnMissingBean
    OAuth2AccessTokenProvider oauth2AccessTokenProvider(
            RestClient.Builder restClientBuilder,
            JwtAssertionProvider jwtAssertionProvider,
            ScopeValidator scopeValidator,
            Clock clock
    ) {
        return new OAuth2AccessTokenProvider(restClientBuilder, jwtAssertionProvider, scopeValidator, clock);
    }

    @Bean
    @ConditionalOnMissingBean
    AccessTokenProvider accessTokenProvider(
            TokenCache tokenCache,
            ObjectProvider<SpringSecurityClientCredentialsAccessTokenProvider> springClientCredentialsProvider,
            ObjectProvider<SpringSecurityJwtBearerAccessTokenProvider> springJwtBearerProvider,
            ScopeValidator scopeValidator,
            Clock clock
    ) {
        return new CachedAccessTokenProvider(
                tokenCache,
                springClientCredentialsProvider.getIfAvailable(),
                springJwtBearerProvider.getIfAvailable(),
                scopeValidator,
                clock
        );
    }

    @Bean
    @ConditionalOnMissingBean
    AccessTokenClient accessTokenClient(
            TokenCache tokenCache,
            ObjectProvider<SpringSecurityClientCredentialsAccessTokenProvider> springClientCredentialsProvider,
            ObjectProvider<SpringSecurityJwtBearerAccessTokenProvider> springJwtBearerProvider,
            ScopeValidator scopeValidator,
            Clock clock
    ) {
        return new DefaultAccessTokenClient(
                tokenCache,
                springClientCredentialsProvider.getIfAvailable(),
                springJwtBearerProvider.getIfAvailable(),
                scopeValidator,
                clock
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
            AccessTokenProvider accessTokenProvider,
            CorrelationHeadersProvider correlationHeadersProvider,
            HttpExchangeAuditSink auditSink,
            HttpClientConfigurator httpClientConfigurator,
            HttpErrorResponseMapper errorResponseMapper,
            ResilienceStateRegistry resilienceStateRegistry,
            ObjectProvider<MeterRegistry> meterRegistry,
            ObjectProvider<RestClientBuilderCustomizer> customizers
    ) {
        return new ConfiguredRestClientFactory(
                restClientBuilder,
                accessTokenProvider,
                correlationHeadersProvider,
                auditSink,
                httpClientConfigurator,
                errorResponseMapper,
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
