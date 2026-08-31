package com.smbtech.serviceframework.starter.restclient.autoconfigure;

import com.smbtech.serviceframework.httpclient.port.in.HttpClientCatalog;
import com.smbtech.serviceframework.httpclient.port.in.HttpClientDefinitionValidator;
import com.smbtech.serviceframework.httpclient.port.out.CredentialDefinitionSource;
import com.smbtech.serviceframework.httpclient.port.out.CredentialProvider;
import com.smbtech.serviceframework.httpclient.port.out.HttpClientDefinitionSource;
import com.smbtech.serviceframework.httpclient.port.out.KeyStoreDefinitionSource;
import com.smbtech.serviceframework.starter.restclient.api.HttpErrorBodyDecoder;
import java.time.Clock;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

/**
 * Auto-configures named REST clients and their optional authentication, resilience, observability,
 * and extension pipelines.
 */
@AutoConfiguration
@ConditionalOnClass(RestClient.class)
@EnableConfigurationProperties(RestClientProperties.class)
@Import(RestClientConfigurationImportSelector.class)
public class RestClientAutoConfiguration {
    /** Creates a rest client auto configuration instance. */
    public RestClientAutoConfiguration() {}

    @Bean
    RestClientPropertiesMapper restClientPropertiesMapper(CredentialResolver credentialResolver) {
        return new RestClientPropertiesMapper(credentialResolver);
    }

    @Bean
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
    CredentialResolver credentialResolver(CredentialProvider credentialProvider) {
        return new CredentialResolver(credentialProvider);
    }

    @Bean
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
        return HttpClientDefinitionValidator.defaultValidator();
    }

    @Bean
    @ConditionalOnMissingBean
    HttpClientCatalog httpClientCatalog(
            HttpClientDefinitionSource source, HttpClientDefinitionValidator validator) {
        return HttpClientCatalog.from(source, validator);
    }

    @Bean
    @ConditionalOnMissingBean
    KeyStoreDefinitionSource keyStoreDefinitionSource(
            RestClientProperties properties, KeyStorePropertiesMapper mapper) {
        return new PropertiesKeyStoreDefinitionSource(properties, mapper);
    }

    @Bean
    @ConditionalOnMissingBean
    HttpErrorBodyDecoder httpErrorBodyDecoder(ObjectProvider<ObjectMapper> objectMapper) {
        return new HttpErrorBodyDecoder(objectMapper.getIfAvailable(this::fallbackObjectMapper));
    }

    private ObjectMapper fallbackObjectMapper() {
        return new ObjectMapper();
    }
}
