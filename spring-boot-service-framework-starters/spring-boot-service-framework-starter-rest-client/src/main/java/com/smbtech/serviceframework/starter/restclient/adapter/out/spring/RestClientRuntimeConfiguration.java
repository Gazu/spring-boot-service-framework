package com.smbtech.serviceframework.starter.restclient.adapter.out.spring;

import com.smbtech.serviceframework.httpclient.domain.HttpClientDefinition;
import com.smbtech.serviceframework.httpclient.port.in.HttpClientCatalog;
import com.smbtech.serviceframework.httpclient.port.out.CorrelationHeadersProvider;
import com.smbtech.serviceframework.httpclient.port.out.HttpErrorResponseBodyReader;
import com.smbtech.serviceframework.httpclient.port.out.HttpExchangeAuditSink;
import com.smbtech.serviceframework.starter.restclient.api.ApiClientFactory;
import com.smbtech.serviceframework.starter.restclient.api.RequestContextManager;
import com.smbtech.serviceframework.starter.restclient.api.RestClientRegistry;
import com.smbtech.serviceframework.starter.restclient.api.customizer.RestClientAuthenticationConfigurer;
import com.smbtech.serviceframework.starter.restclient.api.customizer.RestClientBuilderCustomizer;
import com.smbtech.serviceframework.starter.restclient.autoconfigure.RestClientProperties;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
class RestClientRuntimeConfiguration {

    @Bean
    @ConditionalOnMissingBean
    RequestContextManager requestContextManager() {
        return new ThreadLocalRequestContextManager();
    }

    @Bean
    HttpErrorResponseMapper httpErrorResponseMapper() {
        return new HttpErrorResponseMapper();
    }

    @Bean
    ResilienceStateRegistry resilienceStateRegistry(Clock clock) {
        return new ResilienceStateRegistry(clock);
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
    ConfiguredRestClientFactory configuredRestClientFactory(
            RestClient.Builder restClientBuilder,
            CorrelationHeadersProvider correlationHeadersProvider,
            RequestContextManager requestContextManager,
            RestClientProperties properties,
            HttpExchangeAuditSink auditSink,
            @Qualifier("restClientRequestFactoryBuilder") Function<HttpClientDefinition, ClientHttpRequestFactory> requestFactoryBuilder,
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
                requestFactoryBuilder,
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

    private static <T> List<T> orderedList(ObjectProvider<T> provider) {
        return provider.orderedStream().toList();
    }

    private static boolean requestContextHeadersEnabled(RestClientProperties properties) {
        RestClientProperties.RequestContext requestContext = requestContext(properties);
        return requestContext.isEnabled() && requestContext.isHeaders();
    }

    private static Set<String> blockedRequestContextHeaders(RestClientProperties properties) {
        return Set.copyOf(
                Objects.requireNonNullElse(
                        requestContext(properties).getBlockedHeaders(), Set.of()));
    }

    private static RestClientProperties.RequestContext requestContext(
            RestClientProperties properties) {
        if (properties == null || properties.getRequestContext() == null) {
            return new RestClientProperties.RequestContext();
        }
        return properties.getRequestContext();
    }
}
