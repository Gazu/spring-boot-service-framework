package com.smbtech.serviceframework.starter.restclient.adapter.out.spring;

import com.smbtech.serviceframework.httpclient.domain.AuthenticationType;
import com.smbtech.serviceframework.httpclient.domain.GrantType;
import com.smbtech.serviceframework.httpclient.domain.HttpClientDefinition;
import com.smbtech.serviceframework.httpclient.domain.HttpErrorNotificationMapper;
import com.smbtech.serviceframework.httpclient.exception.HttpClientAuthenticationException;
import com.smbtech.serviceframework.httpclient.port.out.CorrelationHeadersProvider;
import com.smbtech.serviceframework.httpclient.port.out.HttpErrorResponseBodyReader;
import com.smbtech.serviceframework.httpclient.port.out.HttpExchangeAuditSink;
import com.smbtech.serviceframework.starter.restclient.adapter.out.apache.HttpClientConfigurator;
import com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring.AccessTokenCacheKeyPipeline;
import com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring.JwtBearerAuthorizationAttributes;
import com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring.JwtBearerClaimsPipeline;
import com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring.OAuth2ExtensionRegistry;
import com.smbtech.serviceframework.starter.restclient.adapter.out.error.HttpErrorResponseMapper;
import com.smbtech.serviceframework.starter.restclient.adapter.out.interceptor.AuditLogInterceptor;
import com.smbtech.serviceframework.starter.restclient.adapter.out.interceptor.BasicAuthenticationInterceptor;
import com.smbtech.serviceframework.starter.restclient.adapter.out.interceptor.CorrelationHeadersInterceptor;
import com.smbtech.serviceframework.starter.restclient.adapter.out.interceptor.DefaultHeadersInterceptor;
import com.smbtech.serviceframework.starter.restclient.adapter.out.interceptor.MicrometerHttpClientObservationInterceptor;
import com.smbtech.serviceframework.starter.restclient.adapter.out.interceptor.RequestContextHeadersInterceptor;
import com.smbtech.serviceframework.starter.restclient.adapter.out.interceptor.ResilienceInterceptor;
import com.smbtech.serviceframework.starter.restclient.adapter.out.interceptor.StandardErrorHandlingInterceptor;
import com.smbtech.serviceframework.starter.restclient.adapter.out.resilience.ResilienceStateRegistry;
import com.smbtech.serviceframework.starter.restclient.api.RequestContextManager;
import com.smbtech.serviceframework.starter.restclient.api.customizer.RestClientBuilderCustomizer;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;
import org.springframework.security.oauth2.client.web.client.RequestAttributeClientRegistrationIdResolver;
import org.springframework.security.oauth2.client.web.client.RequestAttributePrincipalResolver;
import org.springframework.web.client.RestClient;

/** Provides configured rest client factory behavior. */
public final class ConfiguredRestClientFactory {

    private static final String PRINCIPAL_NAME = "spring-boot-service-framework";

    private final RestClient.Builder restClientBuilder;
    private final OAuth2AuthorizedClientManager authorizedClientManager;
    private final OAuth2AuthorizedClientService authorizedClientService;
    private final CorrelationHeadersProvider correlationHeadersProvider;
    private final RequestContextManager requestContextManager;
    private final boolean requestContextHeadersEnabled;
    private final boolean requestContextJwtBearerClaimsEnabled;
    private final Set<String> blockedRequestContextHeaders;
    private final Set<String> blockedJwtBearerClaims;
    private final OAuth2ExtensionRegistry oAuth2ExtensionRegistry;
    private final JwtBearerClaimsPipeline jwtBearerClaimsPipeline;
    private final AccessTokenCacheKeyPipeline accessTokenCacheKeyPipeline;
    private final HttpExchangeAuditSink auditSink;
    private final HttpClientConfigurator httpClientConfigurator;
    private final HttpErrorResponseMapper errorResponseMapper;
    private final HttpErrorResponseBodyReader errorResponseBodyReader;
    private final ResilienceStateRegistry resilienceStateRegistry;
    private final MeterRegistry meterRegistry;
    private final List<RestClientBuilderCustomizer> customizers;
    private final ClientHttpRequestInterceptor oauth2Interceptor;

    /**
     * Creates a configured rest client factory instance.
     *
     * @param restClientBuilder rest client builder value
     * @param authorizedClientManager authorized client manager value
     * @param correlationHeadersProvider correlation headers provider value
     * @param auditSink audit sink value
     * @param httpClientConfigurator HTTP client configurator value
     */
    public ConfiguredRestClientFactory(
            RestClient.Builder restClientBuilder,
            OAuth2AuthorizedClientManager authorizedClientManager,
            CorrelationHeadersProvider correlationHeadersProvider,
            HttpExchangeAuditSink auditSink,
            HttpClientConfigurator httpClientConfigurator) {
        this(
                restClientBuilder,
                authorizedClientManager,
                null,
                correlationHeadersProvider,
                null,
                auditSink,
                httpClientConfigurator,
                new HttpErrorResponseMapper(),
                null,
                null,
                null,
                List.of());
    }

    /**
     * Creates a configured rest client factory instance.
     *
     * @param restClientBuilder rest client builder value
     * @param authorizedClientManager authorized client manager value
     * @param correlationHeadersProvider correlation headers provider value
     * @param auditSink audit sink value
     * @param httpClientConfigurator HTTP client configurator value
     * @param errorResponseMapper error response mapper value
     * @param customizers customizers value
     */
    public ConfiguredRestClientFactory(
            RestClient.Builder restClientBuilder,
            OAuth2AuthorizedClientManager authorizedClientManager,
            CorrelationHeadersProvider correlationHeadersProvider,
            HttpExchangeAuditSink auditSink,
            HttpClientConfigurator httpClientConfigurator,
            HttpErrorResponseMapper errorResponseMapper,
            List<RestClientBuilderCustomizer> customizers) {
        this(
                restClientBuilder,
                authorizedClientManager,
                null,
                correlationHeadersProvider,
                null,
                auditSink,
                httpClientConfigurator,
                errorResponseMapper,
                null,
                null,
                null,
                customizers);
    }

    /**
     * Creates a configured rest client factory instance.
     *
     * @param restClientBuilder rest client builder value
     * @param authorizedClientManager authorized client manager value
     * @param authorizedClientService authorized client service value
     * @param correlationHeadersProvider correlation headers provider value
     * @param auditSink audit sink value
     * @param httpClientConfigurator HTTP client configurator value
     * @param errorResponseMapper error response mapper value
     * @param resilienceStateRegistry resilience state registry value
     * @param meterRegistry meter registry value
     * @param customizers customizers value
     */
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
            List<RestClientBuilderCustomizer> customizers) {
        this(
                restClientBuilder,
                authorizedClientManager,
                authorizedClientService,
                correlationHeadersProvider,
                null,
                auditSink,
                httpClientConfigurator,
                errorResponseMapper,
                null,
                resilienceStateRegistry,
                meterRegistry,
                customizers);
    }

    /**
     * Creates a configured rest client factory instance.
     *
     * @param restClientBuilder rest client builder value
     * @param authorizedClientManager authorized client manager value
     * @param authorizedClientService authorized client service value
     * @param correlationHeadersProvider correlation headers provider value
     * @param auditSink audit sink value
     * @param httpClientConfigurator HTTP client configurator value
     * @param errorResponseMapper error response mapper value
     * @param errorResponseBodyReader error response body reader value
     * @param resilienceStateRegistry resilience state registry value
     * @param meterRegistry meter registry value
     * @param customizers customizers value
     */
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
            List<RestClientBuilderCustomizer> customizers) {
        this(
                restClientBuilder,
                authorizedClientManager,
                authorizedClientService,
                correlationHeadersProvider,
                null,
                auditSink,
                httpClientConfigurator,
                errorResponseMapper,
                errorResponseBodyReader,
                resilienceStateRegistry,
                meterRegistry,
                customizers);
    }

    /**
     * Creates a configured rest client factory instance.
     *
     * @param restClientBuilder rest client builder value
     * @param authorizedClientManager authorized client manager value
     * @param authorizedClientService authorized client service value
     * @param correlationHeadersProvider correlation headers provider value
     * @param requestContextManager request context manager value
     * @param auditSink audit sink value
     * @param httpClientConfigurator HTTP client configurator value
     * @param errorResponseMapper error response mapper value
     * @param errorResponseBodyReader error response body reader value
     * @param resilienceStateRegistry resilience state registry value
     * @param meterRegistry meter registry value
     * @param customizers customizers value
     */
    public ConfiguredRestClientFactory(
            RestClient.Builder restClientBuilder,
            OAuth2AuthorizedClientManager authorizedClientManager,
            OAuth2AuthorizedClientService authorizedClientService,
            CorrelationHeadersProvider correlationHeadersProvider,
            RequestContextManager requestContextManager,
            HttpExchangeAuditSink auditSink,
            HttpClientConfigurator httpClientConfigurator,
            HttpErrorResponseMapper errorResponseMapper,
            HttpErrorResponseBodyReader errorResponseBodyReader,
            ResilienceStateRegistry resilienceStateRegistry,
            MeterRegistry meterRegistry,
            List<RestClientBuilderCustomizer> customizers) {
        this(
                restClientBuilder,
                authorizedClientManager,
                authorizedClientService,
                correlationHeadersProvider,
                requestContextManager,
                true,
                true,
                Set.of(),
                Set.of(),
                OAuth2ExtensionRegistry.empty(),
                auditSink,
                httpClientConfigurator,
                errorResponseMapper,
                errorResponseBodyReader,
                resilienceStateRegistry,
                meterRegistry,
                customizers);
    }

    /**
     * Creates a configured rest client factory instance.
     *
     * @param restClientBuilder rest client builder value
     * @param authorizedClientManager authorized client manager value
     * @param authorizedClientService authorized client service value
     * @param correlationHeadersProvider correlation headers provider value
     * @param requestContextManager request context manager value
     * @param requestContextHeadersEnabled request context headers enabled value
     * @param requestContextJwtBearerClaimsEnabled request context JWT bearer claims enabled value
     * @param blockedRequestContextHeaders blocked request context headers value
     * @param blockedJwtBearerClaims blocked JWT bearer claims value
     * @param oAuth2ExtensionRegistry o auth2 extension registry value
     * @param auditSink audit sink value
     * @param httpClientConfigurator HTTP client configurator value
     * @param errorResponseMapper error response mapper value
     * @param errorResponseBodyReader error response body reader value
     * @param resilienceStateRegistry resilience state registry value
     * @param meterRegistry meter registry value
     * @param customizers customizers value
     */
    public ConfiguredRestClientFactory(
            RestClient.Builder restClientBuilder,
            OAuth2AuthorizedClientManager authorizedClientManager,
            OAuth2AuthorizedClientService authorizedClientService,
            CorrelationHeadersProvider correlationHeadersProvider,
            RequestContextManager requestContextManager,
            boolean requestContextHeadersEnabled,
            boolean requestContextJwtBearerClaimsEnabled,
            Set<String> blockedRequestContextHeaders,
            Set<String> blockedJwtBearerClaims,
            OAuth2ExtensionRegistry oAuth2ExtensionRegistry,
            HttpExchangeAuditSink auditSink,
            HttpClientConfigurator httpClientConfigurator,
            HttpErrorResponseMapper errorResponseMapper,
            HttpErrorResponseBodyReader errorResponseBodyReader,
            ResilienceStateRegistry resilienceStateRegistry,
            MeterRegistry meterRegistry,
            List<RestClientBuilderCustomizer> customizers) {
        this.restClientBuilder = restClientBuilder;
        this.authorizedClientManager = authorizedClientManager;
        this.authorizedClientService = authorizedClientService;
        this.correlationHeadersProvider = correlationHeadersProvider;
        this.requestContextManager = requestContextManager;
        this.requestContextHeadersEnabled = requestContextHeadersEnabled;
        this.requestContextJwtBearerClaimsEnabled = requestContextJwtBearerClaimsEnabled;
        this.blockedRequestContextHeaders =
                Set.copyOf(
                        java.util.Objects.requireNonNullElse(
                                blockedRequestContextHeaders, Set.<String>of()));
        this.blockedJwtBearerClaims =
                Set.copyOf(
                        java.util.Objects.requireNonNullElse(
                                blockedJwtBearerClaims, Set.<String>of()));
        this.oAuth2ExtensionRegistry =
                java.util.Objects.requireNonNullElseGet(
                        oAuth2ExtensionRegistry, OAuth2ExtensionRegistry::empty);
        this.jwtBearerClaimsPipeline =
                new JwtBearerClaimsPipeline(
                        this.oAuth2ExtensionRegistry, this.blockedJwtBearerClaims);
        this.accessTokenCacheKeyPipeline =
                new AccessTokenCacheKeyPipeline(this.oAuth2ExtensionRegistry);
        this.auditSink = auditSink;
        this.httpClientConfigurator = httpClientConfigurator;
        this.errorResponseMapper = errorResponseMapper;
        this.errorResponseBodyReader = errorResponseBodyReader;
        this.resilienceStateRegistry = resilienceStateRegistry;
        this.meterRegistry = meterRegistry;
        this.customizers = List.copyOf(customizers);
        this.oauth2Interceptor = oauth2Interceptor();
    }

    /**
     * Creates the result.
     *
     * @param definition definition value
     * @return create result
     */
    public RestClient create(HttpClientDefinition definition) {
        RestClient.Builder builder =
                restClientBuilder
                        .clone()
                        .baseUrl(definition.baseUrl())
                        .defaultStatusHandler(
                                HttpStatusCode::isError,
                                (request, response) -> {
                                    // Error handling is centralized in
                                    // StandardErrorHandlingInterceptor.
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
        if (definition.authenticationType() == AuthenticationType.BASIC_AUTH) {
            builder.requestInterceptor(
                    new BasicAuthenticationInterceptor(definition.basicAuthentication()));
        }
        if (definition.authenticationType() == AuthenticationType.CLIENT_CREDENTIALS
                || definition.authenticationType() == AuthenticationType.JWT_BEARER) {
            builder.defaultRequest(
                    request ->
                            request.attributes(
                                    attributes -> {
                                        RequestAttributeClientRegistrationIdResolver
                                                .clientRegistrationId(definition.tokenRequestId())
                                                .accept(attributes);
                                        if (requestContextJwtBearerClaimsEnabled
                                                && definition.authenticationType()
                                                        == AuthenticationType.JWT_BEARER) {
                                            addJwtBearerRequestContextAttributes(
                                                    definition, attributes);
                                        } else {
                                            RequestAttributePrincipalResolver.principal(
                                                            accessTokenCacheKeyPipeline.resolve(
                                                                    definition.tokenRequestId(),
                                                                    grantType(definition),
                                                                    PRINCIPAL_NAME,
                                                                    scopes(definition),
                                                                    Map.of()))
                                                    .accept(attributes);
                                        }
                                    }));
            builder.requestInterceptor(requiredOauth2Interceptor(definition));
        }
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
        String defaultPrincipalName =
                JwtBearerAuthorizationAttributes.cachePrincipalName(
                        PRINCIPAL_NAME, customClaims, blockedJwtBearerClaims);
        RequestAttributePrincipalResolver.principal(
                        accessTokenCacheKeyPipeline.resolve(
                                definition.tokenRequestId(),
                                GrantType.JWT_BEARER,
                                defaultPrincipalName,
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

    private ClientHttpRequestInterceptor requiredOauth2Interceptor(
            HttpClientDefinition definition) {
        if (oauth2Interceptor == null) {
            throw new HttpClientAuthenticationException(
                    "OAuth2 authorized client manager is not configured for HTTP client: "
                            + definition.name());
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
        interceptor.setClientRegistrationIdResolver(
                request -> {
                    if (request.getHeaders().containsHeader(HttpHeaders.AUTHORIZATION)) {
                        return null;
                    }
                    return clientRegistrationIdResolver.resolve(request);
                });
        interceptor.setPrincipalResolver(new RequestAttributePrincipalResolver());
        if (authorizedClientService != null) {
            interceptor.setAuthorizationFailureHandler(
                    OAuth2ClientHttpRequestInterceptor.authorizationFailureHandler(
                            authorizedClientService));
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
