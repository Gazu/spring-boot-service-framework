package com.smbtech.serviceframework.starter.restclient.adapter.out.spring;

import com.smbtech.serviceframework.httpclient.domain.AuthenticationType;
import com.smbtech.serviceframework.httpclient.domain.HttpClientDefinition;
import com.smbtech.serviceframework.httpclient.domain.HttpErrorNotificationMapper;
import com.smbtech.serviceframework.httpclient.exception.HttpClientAuthenticationException;
import com.smbtech.serviceframework.httpclient.port.out.CorrelationHeadersProvider;
import com.smbtech.serviceframework.httpclient.port.out.HttpErrorResponseBodyReader;
import com.smbtech.serviceframework.httpclient.port.out.HttpExchangeAuditSink;
import com.smbtech.serviceframework.starter.restclient.api.RequestContextManager;
import com.smbtech.serviceframework.starter.restclient.api.customizer.RestClientAuthenticationConfigurer;
import com.smbtech.serviceframework.starter.restclient.api.customizer.RestClientBuilderCustomizer;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** Creates configured REST clients without depending on a concrete authentication provider. */
final class ConfiguredRestClientFactory {

    private final RestClient.Builder restClientBuilder;
    private final CorrelationHeadersProvider correlationHeadersProvider;
    private final RequestContextManager requestContextManager;
    private final boolean requestContextHeadersEnabled;
    private final Set<String> blockedRequestContextHeaders;
    private final HttpExchangeAuditSink auditSink;
    private final Function<HttpClientDefinition, ClientHttpRequestFactory> requestFactoryBuilder;
    private final HttpErrorResponseMapper errorResponseMapper;
    private final HttpErrorResponseBodyReader errorResponseBodyReader;
    private final ResilienceStateRegistry resilienceStateRegistry;
    private final MeterRegistry meterRegistry;
    private final List<RestClientAuthenticationConfigurer> authenticationConfigurers;
    private final List<RestClientBuilderCustomizer> customizers;

    /**
     * Creates a configured REST client factory.
     *
     * @param restClientBuilder base REST client builder
     * @param correlationHeadersProvider correlation header provider
     * @param requestContextManager dynamic request context manager
     * @param requestContextHeadersEnabled whether dynamic headers are enabled
     * @param blockedRequestContextHeaders header names that cannot be propagated
     * @param auditSink HTTP exchange audit sink
     * @param requestFactoryBuilder HTTP transport request factory builder
     * @param errorResponseMapper error response mapper
     * @param errorResponseBodyReader error body reader
     * @param resilienceStateRegistry resilience state registry
     * @param meterRegistry optional meter registry
     * @param authenticationConfigurers authentication provider configurers
     * @param customizers REST client builder customizers
     */
    public ConfiguredRestClientFactory(
            RestClient.Builder restClientBuilder,
            CorrelationHeadersProvider correlationHeadersProvider,
            RequestContextManager requestContextManager,
            boolean requestContextHeadersEnabled,
            Set<String> blockedRequestContextHeaders,
            HttpExchangeAuditSink auditSink,
            Function<HttpClientDefinition, ClientHttpRequestFactory> requestFactoryBuilder,
            HttpErrorResponseMapper errorResponseMapper,
            HttpErrorResponseBodyReader errorResponseBodyReader,
            ResilienceStateRegistry resilienceStateRegistry,
            MeterRegistry meterRegistry,
            List<RestClientAuthenticationConfigurer> authenticationConfigurers,
            List<RestClientBuilderCustomizer> customizers) {
        this.restClientBuilder = Objects.requireNonNull(restClientBuilder);
        this.correlationHeadersProvider = Objects.requireNonNull(correlationHeadersProvider);
        this.requestContextManager = requestContextManager;
        this.requestContextHeadersEnabled = requestContextHeadersEnabled;
        this.blockedRequestContextHeaders =
                Set.copyOf(
                        Objects.requireNonNullElse(blockedRequestContextHeaders, Set.<String>of()));
        this.auditSink = Objects.requireNonNull(auditSink);
        this.requestFactoryBuilder = Objects.requireNonNull(requestFactoryBuilder);
        this.errorResponseMapper = Objects.requireNonNull(errorResponseMapper);
        this.errorResponseBodyReader = errorResponseBodyReader;
        this.resilienceStateRegistry = resilienceStateRegistry;
        this.meterRegistry = meterRegistry;
        this.authenticationConfigurers = List.copyOf(authenticationConfigurers);
        this.customizers = List.copyOf(customizers);
    }

    /**
     * Creates a REST client for the supplied definition.
     *
     * @param definition HTTP client definition
     * @return configured REST client
     */
    public RestClient create(HttpClientDefinition definition) {
        RestClient.Builder builder =
                restClientBuilder
                        .clone()
                        .baseUrl(definition.baseUrl())
                        .defaultStatusHandler(
                                HttpStatusCode::isError,
                                (request, response) -> {
                                    // Error handling is centralized in the response interceptor.
                                })
                        .requestFactory(requestFactory(definition))
                        .requestInterceptor(
                                new DefaultHeadersInterceptor(definition.defaultHeaders()))
                        .requestInterceptor(
                                new CorrelationHeadersInterceptor(correlationHeadersProvider));
        if (requestContextHeadersEnabled && requestContextManager != null) {
            builder.requestInterceptor(
                    new RequestContextHeadersInterceptor(
                            requestContextManager, blockedRequestContextHeaders));
        }

        configureAuthentication(builder, definition);

        if (definition.observability().enabled() && meterRegistry != null) {
            builder.requestInterceptor(
                    new MicrometerHttpClientObservationInterceptor(definition, meterRegistry));
        }
        if (definition.audit().enabled()) {
            builder.requestInterceptor(new AuditLogInterceptor(definition, auditSink));
        }
        if (definition.resilience().enabled() && resilienceStateRegistry != null) {
            builder.requestInterceptor(
                    new ResilienceInterceptor(definition, resilienceStateRegistry));
        }

        builder.requestInterceptor(
                new StandardErrorHandlingInterceptor(
                        definition,
                        errorResponseMapper,
                        new HttpErrorNotificationMapper(),
                        errorResponseBodyReader));
        customizers.forEach(customizer -> customizer.customize(definition, builder));
        return builder.build();
    }

    private void configureAuthentication(
            RestClient.Builder builder, HttpClientDefinition definition) {
        AuthenticationType authenticationType = definition.authenticationType();
        if (authenticationType == AuthenticationType.NO_AUTH) {
            return;
        }
        if (authenticationType == AuthenticationType.BASIC_AUTH) {
            builder.requestInterceptor(
                    new BasicAuthenticationInterceptor(definition.basicAuthentication()));
            return;
        }
        RestClientAuthenticationConfigurer configurer =
                authenticationConfigurers.stream()
                        .filter(candidate -> candidate.supports(authenticationType))
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new HttpClientAuthenticationException(
                                                "No authentication provider is configured for "
                                                        + authenticationType
                                                        + " HTTP client: "
                                                        + definition.name()));
        configurer.configure(definition, builder);
    }

    private ClientHttpRequestFactory requestFactory(HttpClientDefinition definition) {
        ClientHttpRequestFactory delegate = requestFactoryBuilder.apply(definition);
        if (definition.audit().enabled() && definition.audit().includeBody()) {
            return new BufferingClientHttpRequestFactory(delegate);
        }
        return delegate;
    }
}
