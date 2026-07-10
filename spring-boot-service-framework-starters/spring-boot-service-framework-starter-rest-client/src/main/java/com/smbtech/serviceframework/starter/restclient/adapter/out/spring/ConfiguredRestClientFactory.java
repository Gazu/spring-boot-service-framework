package com.smbtech.serviceframework.starter.restclient.adapter.out.spring;

import com.smbtech.serviceframework.httpclient.domain.AuthenticationType;
import com.smbtech.serviceframework.httpclient.domain.HttpClientDefinition;
import com.smbtech.serviceframework.httpclient.port.out.AccessTokenProvider;
import com.smbtech.serviceframework.httpclient.port.out.CorrelationHeadersProvider;
import com.smbtech.serviceframework.httpclient.port.out.HttpExchangeAuditSink;
import com.smbtech.serviceframework.starter.restclient.adapter.out.apache.HttpClientConfigurator;
import com.smbtech.serviceframework.starter.restclient.adapter.out.interceptor.AuditLogInterceptor;
import com.smbtech.serviceframework.starter.restclient.adapter.out.interceptor.BasicAuthenticationInterceptor;
import com.smbtech.serviceframework.starter.restclient.adapter.out.interceptor.BearerTokenInterceptor;
import com.smbtech.serviceframework.starter.restclient.adapter.out.interceptor.CorrelationHeadersInterceptor;
import com.smbtech.serviceframework.starter.restclient.adapter.out.interceptor.DefaultHeadersInterceptor;
import com.smbtech.serviceframework.starter.restclient.adapter.out.error.HttpErrorResponseMapper;
import com.smbtech.serviceframework.starter.restclient.adapter.out.interceptor.MicrometerHttpClientObservationInterceptor;
import com.smbtech.serviceframework.starter.restclient.adapter.out.interceptor.ResilienceInterceptor;
import com.smbtech.serviceframework.starter.restclient.adapter.out.interceptor.StandardErrorHandlingInterceptor;
import com.smbtech.serviceframework.starter.restclient.adapter.out.resilience.ResilienceStateRegistry;
import com.smbtech.serviceframework.starter.restclient.api.customizer.RestClientBuilderCustomizer;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.util.List;

public final class ConfiguredRestClientFactory {

    private final RestClient.Builder restClientBuilder;
    private final AccessTokenProvider accessTokenProvider;
    private final CorrelationHeadersProvider correlationHeadersProvider;
    private final HttpExchangeAuditSink auditSink;
    private final HttpClientConfigurator httpClientConfigurator;
    private final HttpErrorResponseMapper errorResponseMapper;
    private final ResilienceStateRegistry resilienceStateRegistry;
    private final MeterRegistry meterRegistry;
    private final List<RestClientBuilderCustomizer> customizers;

    public ConfiguredRestClientFactory(
            RestClient.Builder restClientBuilder,
            AccessTokenProvider accessTokenProvider,
            CorrelationHeadersProvider correlationHeadersProvider,
            HttpExchangeAuditSink auditSink,
            HttpClientConfigurator httpClientConfigurator
    ) {
        this(
                restClientBuilder,
                accessTokenProvider,
                correlationHeadersProvider,
                auditSink,
                httpClientConfigurator,
                new HttpErrorResponseMapper(),
                null,
                null,
                List.of()
        );
    }

    public ConfiguredRestClientFactory(
            RestClient.Builder restClientBuilder,
            AccessTokenProvider accessTokenProvider,
            CorrelationHeadersProvider correlationHeadersProvider,
            HttpExchangeAuditSink auditSink,
            HttpClientConfigurator httpClientConfigurator,
            HttpErrorResponseMapper errorResponseMapper,
            List<RestClientBuilderCustomizer> customizers
    ) {
        this(
                restClientBuilder,
                accessTokenProvider,
                correlationHeadersProvider,
                auditSink,
                httpClientConfigurator,
                errorResponseMapper,
                null,
                null,
                customizers
        );
    }

    public ConfiguredRestClientFactory(
            RestClient.Builder restClientBuilder,
            AccessTokenProvider accessTokenProvider,
            CorrelationHeadersProvider correlationHeadersProvider,
            HttpExchangeAuditSink auditSink,
            HttpClientConfigurator httpClientConfigurator,
            HttpErrorResponseMapper errorResponseMapper,
            ResilienceStateRegistry resilienceStateRegistry,
            MeterRegistry meterRegistry,
            List<RestClientBuilderCustomizer> customizers
    ) {
        this.restClientBuilder = restClientBuilder;
        this.accessTokenProvider = accessTokenProvider;
        this.correlationHeadersProvider = correlationHeadersProvider;
        this.auditSink = auditSink;
        this.httpClientConfigurator = httpClientConfigurator;
        this.errorResponseMapper = errorResponseMapper;
        this.resilienceStateRegistry = resilienceStateRegistry;
        this.meterRegistry = meterRegistry;
        this.customizers = List.copyOf(customizers);
    }

    public RestClient create(HttpClientDefinition definition) {
        RestClient.Builder builder = restClientBuilder.clone()
                .baseUrl(definition.baseUrl())
                .defaultStatusHandler(HttpStatusCode::isError, (request, response) -> {
                    // Error handling is centralized in StandardErrorHandlingInterceptor.
                })
                .requestFactory(requestFactory(definition))
                .requestInterceptor(new DefaultHeadersInterceptor(definition.defaultHeaders()))
                .requestInterceptor(new CorrelationHeadersInterceptor(correlationHeadersProvider));

        configureAuthentication(builder, definition);

        if (definition.observability().enabled() && meterRegistry != null) {
            builder.requestInterceptor(new MicrometerHttpClientObservationInterceptor(definition, meterRegistry));
        }

        if (definition.audit().enabled()) {
            builder.requestInterceptor(new AuditLogInterceptor(definition, auditSink));
        }

        if (definition.resilience().enabled() && resilienceStateRegistry != null) {
            builder.requestInterceptor(new ResilienceInterceptor(definition, resilienceStateRegistry));
        }

        builder.requestInterceptor(new StandardErrorHandlingInterceptor(definition, errorResponseMapper));

        customizers.forEach(customizer -> customizer.customize(definition, builder));

        return builder.build();
    }

    private void configureAuthentication(RestClient.Builder builder, HttpClientDefinition definition) {
        if (definition.authenticationType() == AuthenticationType.BASIC_AUTH) {
            builder.requestInterceptor(new BasicAuthenticationInterceptor(definition.basicAuthentication()));
        }
        if (definition.authenticationType() == AuthenticationType.CLIENT_CREDENTIALS
                || definition.authenticationType() == AuthenticationType.JWT_BEARER) {
            builder.requestInterceptor(new BearerTokenInterceptor(
                    accessTokenProvider,
                    definition.credentialTokenRequestorId(),
                    definition.scopes()
            ));
        }
    }

    private ClientHttpRequestFactory requestFactory(HttpClientDefinition definition) {
        ClientHttpRequestFactory delegate = httpClientConfigurator.build(definition);

        if (definition.audit().enabled() && definition.audit().includeBody()) {
            return new BufferingClientHttpRequestFactory(delegate);
        }
        return delegate;
    }
}
