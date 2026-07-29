package com.smbtech.serviceframework.starter.restclient.autoconfigure;

import static org.springframework.beans.factory.config.BeanDefinition.ROLE_INFRASTRUCTURE;

import com.smbtech.serviceframework.httpclient.port.in.HttpClientCatalog;
import com.smbtech.serviceframework.httpclient.port.in.HttpClientDefinitionValidator;
import com.smbtech.serviceframework.httpclient.port.out.CorrelationHeadersProvider;
import com.smbtech.serviceframework.httpclient.port.out.CredentialDefinitionSource;
import com.smbtech.serviceframework.httpclient.port.out.CredentialProvider;
import com.smbtech.serviceframework.httpclient.port.out.HttpClientDefinitionSource;
import com.smbtech.serviceframework.httpclient.port.out.HttpErrorResponseBodyReader;
import com.smbtech.serviceframework.httpclient.port.out.HttpExchangeAuditSink;
import com.smbtech.serviceframework.httpclient.port.out.KeyStoreDefinitionSource;
import com.smbtech.serviceframework.httpclient.service.DefaultHttpClientCatalog;
import com.smbtech.serviceframework.httpclient.service.DefaultHttpClientDefinitionValidator;
import com.smbtech.serviceframework.starter.restclient.adapter.out.apache.ApacheHttpClientConfigurator;
import com.smbtech.serviceframework.starter.restclient.adapter.out.apache.ConnectionReuseStrategyConfigurator;
import com.smbtech.serviceframework.starter.restclient.adapter.out.apache.HostnameVerifierConfigurator;
import com.smbtech.serviceframework.starter.restclient.adapter.out.apache.HttpClientConfigurator;
import com.smbtech.serviceframework.starter.restclient.adapter.out.apache.HttpClientConnectionManagerConfigurator;
import com.smbtech.serviceframework.starter.restclient.adapter.out.apache.KeepAliveStrategyConfigurator;
import com.smbtech.serviceframework.starter.restclient.adapter.out.apache.RegistryConfigurator;
import com.smbtech.serviceframework.starter.restclient.adapter.out.apache.RequestConfigConfigurator;
import com.smbtech.serviceframework.starter.restclient.adapter.out.apache.SocketConfigConfigurator;
import com.smbtech.serviceframework.starter.restclient.adapter.out.apache.SslConnectionSocketFactoryConfigurator;
import com.smbtech.serviceframework.starter.restclient.adapter.out.apache.SslContextFactory;
import com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.PropertiesCredentialDefinitionSource;
import com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.PropertiesCredentialProvider;
import com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.PropertiesKeyStoreDefinitionSource;
import com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.keystore.KeyStoreManager;
import com.smbtech.serviceframework.starter.restclient.adapter.out.context.ThreadLocalRequestContextManager;
import com.smbtech.serviceframework.starter.restclient.adapter.out.error.HttpErrorResponseMapper;
import com.smbtech.serviceframework.starter.restclient.adapter.out.resilience.ResilienceStateRegistry;
import com.smbtech.serviceframework.starter.restclient.adapter.out.source.PropertiesHttpClientDefinitionSource;
import com.smbtech.serviceframework.starter.restclient.adapter.out.spring.ConfiguredRestClientFactory;
import com.smbtech.serviceframework.starter.restclient.adapter.out.spring.DefaultApiClientFactory;
import com.smbtech.serviceframework.starter.restclient.adapter.out.spring.DefaultRestClientRegistry;
import com.smbtech.serviceframework.starter.restclient.adapter.out.spring.MdcCorrelationHeadersProvider;
import com.smbtech.serviceframework.starter.restclient.adapter.out.spring.Slf4jHttpExchangeAuditSink;
import com.smbtech.serviceframework.starter.restclient.api.ApiClientFactory;
import com.smbtech.serviceframework.starter.restclient.api.HttpErrorBodyDecoder;
import com.smbtech.serviceframework.starter.restclient.api.RequestContextManager;
import com.smbtech.serviceframework.starter.restclient.api.RestClientRegistry;
import com.smbtech.serviceframework.starter.restclient.api.customizer.ApacheHttpClientBuilderCustomizer;
import com.smbtech.serviceframework.starter.restclient.api.customizer.ClientHttpRequestFactoryCustomizer;
import com.smbtech.serviceframework.starter.restclient.api.customizer.RestClientAuthenticationConfigurer;
import com.smbtech.serviceframework.starter.restclient.api.customizer.RestClientBuilderCustomizer;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.util.List;
import javax.net.ssl.SSLContext;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.context.annotation.Role;
import org.springframework.core.io.ResourceLoader;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

/**
 * Auto-configures named REST clients and their optional authentication, resilience, observability,
 * and extension pipelines.
 */
@AutoConfiguration
@ConditionalOnClass(RestClient.class)
@EnableConfigurationProperties(RestClientProperties.class)
@ImportRuntimeHints(RestClientRuntimeHints.class)
public class RestClientAutoConfiguration {
    /** Creates a rest client auto configuration instance. */
    public RestClientAutoConfiguration() {}

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
            RestClientProperties properties, CredentialPropertiesMapper mapper) {
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
            RestClientProperties properties, RestClientPropertiesMapper mapper) {
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
            HttpClientDefinitionSource source, HttpClientDefinitionValidator validator) {
        return new DefaultHttpClientCatalog(source, validator);
    }

    @Bean
    @ConditionalOnMissingBean
    KeyStoreDefinitionSource keyStoreDefinitionSource(
            RestClientProperties properties, KeyStorePropertiesMapper mapper) {
        return new PropertiesKeyStoreDefinitionSource(properties, mapper);
    }

    @Bean
    @ConditionalOnMissingBean
    RequestContextManager requestContextManager() {
        return new ThreadLocalRequestContextManager();
    }

    @Bean
    @ConditionalOnMissingBean
    KeyStoreManager keyStoreManager(
            KeyStoreDefinitionSource keyStoreDefinitionSource, ResourceLoader resourceLoader) {
        return new KeyStoreManager(keyStoreDefinitionSource, resourceLoader);
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
            ObjectProvider<SSLContext> sslContext) {
        return new SslConnectionSocketFactoryConfigurator(
                hostnameVerifierConfigurator, sslContextFactory, sslContext.getIfAvailable());
    }

    @Bean
    @ConditionalOnMissingBean
    RegistryConfigurator registryConfigurator(
            SslConnectionSocketFactoryConfigurator sslConnectionSocketFactoryConfigurator) {
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
            SocketConfigConfigurator socketConfigConfigurator) {
        return new HttpClientConnectionManagerConfigurator(
                registryConfigurator, socketConfigConfigurator);
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
            ObjectProvider<ApacheHttpClientBuilderCustomizer> customizers) {
        return new ApacheHttpClientConfigurator(
                connectionManagerConfigurator,
                connectionReuseStrategyConfigurator,
                keepAliveStrategyConfigurator,
                requestConfigConfigurator,
                orderedList(customizers));
    }

    @Bean
    @ConditionalOnMissingBean
    HttpClientConfigurator httpClientConfigurator(
            ApacheHttpClientConfigurator apacheHttpClientConfigurator,
            ObjectProvider<ClientHttpRequestFactoryCustomizer> customizers) {
        return new HttpClientConfigurator(apacheHttpClientConfigurator, orderedList(customizers));
    }

    @Bean
    @ConditionalOnMissingBean
    ConfiguredRestClientFactory configuredRestClientFactory(
            RestClient.Builder restClientBuilder,
            CorrelationHeadersProvider correlationHeadersProvider,
            RequestContextManager requestContextManager,
            RestClientProperties properties,
            HttpExchangeAuditSink auditSink,
            HttpClientConfigurator httpClientConfigurator,
            HttpErrorResponseMapper errorResponseMapper,
            HttpErrorResponseBodyReader errorResponseBodyReader,
            ResilienceStateRegistry resilienceStateRegistry,
            ObjectProvider<MeterRegistry> meterRegistry,
            ObjectProvider<RestClientAuthenticationConfigurer> authenticationConfigurers,
            ObjectProvider<RestClientBuilderCustomizer> customizers) {
        return new ConfiguredRestClientFactory(
                restClientBuilder,
                correlationHeadersProvider,
                requestContextManager,
                requestContextHeadersEnabled(properties),
                blockedRequestContextHeaders(properties),
                auditSink,
                httpClientConfigurator,
                errorResponseMapper,
                errorResponseBodyReader,
                resilienceStateRegistry,
                meterRegistry.getIfAvailable(),
                orderedList(authenticationConfigurers),
                orderedList(customizers));
    }

    @Bean
    @ConditionalOnMissingBean
    RestClientRegistry restClientRegistry(
            HttpClientCatalog catalog, ConfiguredRestClientFactory factory) {
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

    private boolean requestContextHeadersEnabled(RestClientProperties properties) {
        RestClientProperties.RequestContext requestContext = requestContext(properties);
        return requestContext.isEnabled() && requestContext.isHeaders();
    }

    private RestClientProperties.RequestContext requestContext(RestClientProperties properties) {
        if (properties == null || properties.getRequestContext() == null) {
            return new RestClientProperties.RequestContext();
        }
        return properties.getRequestContext();
    }

    private java.util.Set<String> blockedRequestContextHeaders(RestClientProperties properties) {
        return java.util.Set.copyOf(
                java.util.Objects.requireNonNullElse(
                        requestContext(properties).getBlockedHeaders(), java.util.Set.of()));
    }

    private ObjectMapper fallbackObjectMapper() {
        return new ObjectMapper();
    }
}
