package com.smbtech.serviceframework.starter.restclient.adapter.out.spring;

import com.smbtech.serviceframework.httpclient.domain.AuthenticationType;
import com.smbtech.serviceframework.httpclient.domain.HttpClientDefinition;
import com.smbtech.serviceframework.httpclient.domain.HttpErrorNotificationMapper;
import com.smbtech.serviceframework.httpclient.exception.AuthenticationException;
import com.smbtech.serviceframework.httpclient.port.out.CorrelationHeadersProvider;
import com.smbtech.serviceframework.httpclient.port.out.HttpErrorResponseBodyReader;
import com.smbtech.serviceframework.httpclient.port.out.HttpExchangeAuditSink;
import com.smbtech.serviceframework.starter.restclient.adapter.out.apache.HttpClientConfigurator;
import com.smbtech.serviceframework.starter.restclient.adapter.out.interceptor.AuditLogInterceptor;
import com.smbtech.serviceframework.starter.restclient.adapter.out.interceptor.BasicAuthenticationInterceptor;
import com.smbtech.serviceframework.starter.restclient.adapter.out.interceptor.CorrelationHeadersInterceptor;
import com.smbtech.serviceframework.starter.restclient.adapter.out.interceptor.DefaultHeadersInterceptor;
import com.smbtech.serviceframework.starter.restclient.adapter.out.error.HttpErrorResponseMapper;
import com.smbtech.serviceframework.starter.restclient.adapter.out.interceptor.MicrometerHttpClientObservationInterceptor;
import com.smbtech.serviceframework.starter.restclient.adapter.out.interceptor.ResilienceInterceptor;
import com.smbtech.serviceframework.starter.restclient.adapter.out.interceptor.StandardErrorHandlingInterceptor;
import com.smbtech.serviceframework.starter.restclient.adapter.out.resilience.ResilienceStateRegistry;
import com.smbtech.serviceframework.starter.restclient.api.customizer.RestClientBuilderCustomizer;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;
import org.springframework.security.oauth2.client.web.client.RequestAttributeClientRegistrationIdResolver;
import org.springframework.security.oauth2.client.web.client.RequestAttributePrincipalResolver;
import org.springframework.web.client.RestClient;

import java.util.List;

public final class ConfiguredRestClientFactory {

    private static final String PRINCIPAL_NAME = "spring-boot-service-framework";

    private final RestClient.Builder restClientBuilder;
    private final OAuth2AuthorizedClientManager authorizedClientManager;
    private final OAuth2AuthorizedClientService authorizedClientService;
    private final CorrelationHeadersProvider correlationHeadersProvider;
    private final HttpExchangeAuditSink auditSink;
    private final HttpClientConfigurator httpClientConfigurator;
    private final HttpErrorResponseMapper errorResponseMapper;
    private final HttpErrorResponseBodyReader errorResponseBodyReader;
    private final ResilienceStateRegistry resilienceStateRegistry;
    private final MeterRegistry meterRegistry;
    private final List<RestClientBuilderCustomizer> customizers;
    private final ClientHttpRequestInterceptor oauth2Interceptor;

    public ConfiguredRestClientFactory(
            RestClient.Builder restClientBuilder,
            OAuth2AuthorizedClientManager authorizedClientManager,
            CorrelationHeadersProvider correlationHeadersProvider,
            HttpExchangeAuditSink auditSink,
            HttpClientConfigurator httpClientConfigurator
    ) {
        this(
                restClientBuilder,
                authorizedClientManager,
                null,
                correlationHeadersProvider,
                auditSink,
                httpClientConfigurator,
                new HttpErrorResponseMapper(),
                null,
                null,
                null,
                List.of()
        );
    }

    public ConfiguredRestClientFactory(
            RestClient.Builder restClientBuilder,
            OAuth2AuthorizedClientManager authorizedClientManager,
            CorrelationHeadersProvider correlationHeadersProvider,
            HttpExchangeAuditSink auditSink,
            HttpClientConfigurator httpClientConfigurator,
            HttpErrorResponseMapper errorResponseMapper,
            List<RestClientBuilderCustomizer> customizers
    ) {
        this(
                restClientBuilder,
                authorizedClientManager,
                null,
                correlationHeadersProvider,
                auditSink,
                httpClientConfigurator,
                errorResponseMapper,
                null,
                null,
                null,
                customizers
        );
    }

    public ConfiguredRestClientFactory(
            RestClient.Builder restClientBuilder,
            OAuth2AuthorizedClientManager authorizedClientManager,
            OAuth2AuthorizedClientService authorizedClientService,
            CorrelationHeadersProvider correlationHeadersProvider,
            HttpExchangeAuditSink auditSink,
            HttpClientConfigurator httpClientConfigurator,
            HttpErrorResponseMapper errorResponseMapper,
            ResilienceStateRegistry resilienceStateRegistry,
            MeterRegistry meterRegistry,
            List<RestClientBuilderCustomizer> customizers
    ) {
        this(
                restClientBuilder,
                authorizedClientManager,
                authorizedClientService,
                correlationHeadersProvider,
                auditSink,
                httpClientConfigurator,
                errorResponseMapper,
                null,
                resilienceStateRegistry,
                meterRegistry,
                customizers
        );
    }

    public ConfiguredRestClientFactory(
            RestClient.Builder restClientBuilder,
            OAuth2AuthorizedClientManager authorizedClientManager,
            OAuth2AuthorizedClientService authorizedClientService,
            CorrelationHeadersProvider correlationHeadersProvider,
            HttpExchangeAuditSink auditSink,
            HttpClientConfigurator httpClientConfigurator,
            HttpErrorResponseMapper errorResponseMapper,
            HttpErrorResponseBodyReader errorResponseBodyReader,
            ResilienceStateRegistry resilienceStateRegistry,
            MeterRegistry meterRegistry,
            List<RestClientBuilderCustomizer> customizers
    ) {
        this.restClientBuilder = restClientBuilder;
        this.authorizedClientManager = authorizedClientManager;
        this.authorizedClientService = authorizedClientService;
        this.correlationHeadersProvider = correlationHeadersProvider;
        this.auditSink = auditSink;
        this.httpClientConfigurator = httpClientConfigurator;
        this.errorResponseMapper = errorResponseMapper;
        this.errorResponseBodyReader = errorResponseBodyReader;
        this.resilienceStateRegistry = resilienceStateRegistry;
        this.meterRegistry = meterRegistry;
        this.customizers = List.copyOf(customizers);
        this.oauth2Interceptor = oauth2Interceptor();
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

        builder.requestInterceptor(new StandardErrorHandlingInterceptor(
                definition,
                errorResponseMapper,
                new HttpErrorNotificationMapper(),
                errorResponseBodyReader
        ));

        customizers.forEach(customizer -> customizer.customize(definition, builder));

        return builder.build();
    }

    private void configureAuthentication(RestClient.Builder builder, HttpClientDefinition definition) {
        if (definition.authenticationType() == AuthenticationType.BASIC_AUTH) {
            builder.requestInterceptor(new BasicAuthenticationInterceptor(definition.basicAuthentication()));
        }
        if (definition.authenticationType() == AuthenticationType.CLIENT_CREDENTIALS
                || definition.authenticationType() == AuthenticationType.JWT_BEARER) {
            builder.defaultRequest(request -> request.attributes(attributes -> {
                RequestAttributeClientRegistrationIdResolver
                        .clientRegistrationId(definition.credentialTokenRequestorId())
                        .accept(attributes);
                RequestAttributePrincipalResolver.principal(PRINCIPAL_NAME).accept(attributes);
            }));
            builder.requestInterceptor(requiredOauth2Interceptor(definition));
        }
    }

    private ClientHttpRequestInterceptor requiredOauth2Interceptor(HttpClientDefinition definition) {
        if (oauth2Interceptor == null) {
            throw new AuthenticationException(
                    "OAuth2 authorized client manager is not configured for HTTP client: " + definition.name()
            );
        }
        return oauth2Interceptor;
    }

    private ClientHttpRequestInterceptor oauth2Interceptor() {
        if (authorizedClientManager == null) {
            return null;
        }
        OAuth2ClientHttpRequestInterceptor interceptor =
                new OAuth2ClientHttpRequestInterceptor(authorizedClientManager);
        RequestAttributeClientRegistrationIdResolver clientRegistrationIdResolver =
                new RequestAttributeClientRegistrationIdResolver();
        interceptor.setClientRegistrationIdResolver(request -> {
            if (request.getHeaders().containsHeader(HttpHeaders.AUTHORIZATION)) {
                return null;
            }
            return clientRegistrationIdResolver.resolve(request);
        });
        interceptor.setPrincipalResolver(new RequestAttributePrincipalResolver());
        if (authorizedClientService != null) {
            interceptor.setAuthorizationFailureHandler(
                    OAuth2ClientHttpRequestInterceptor.authorizationFailureHandler(authorizedClientService)
            );
        }
        return interceptor;
    }

    private ClientHttpRequestFactory requestFactory(HttpClientDefinition definition) {
        ClientHttpRequestFactory delegate = httpClientConfigurator.build(definition);

        if (definition.audit().enabled() && definition.audit().includeBody()) {
            return new BufferingClientHttpRequestFactory(delegate);
        }
        return delegate;
    }
}
