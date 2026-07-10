package com.smbtech.serviceframework.starter.restclient.autoconfigure;

import com.smbtech.serviceframework.httpclient.domain.AuditPolicy;
import com.smbtech.serviceframework.httpclient.domain.ApacheHttpClientPolicy;
import com.smbtech.serviceframework.httpclient.domain.BasicAuthentication;
import com.smbtech.serviceframework.httpclient.domain.CircuitBreakerPolicy;
import com.smbtech.serviceframework.httpclient.domain.ErrorHandlingPolicy;
import com.smbtech.serviceframework.httpclient.domain.HttpClientDefinition;
import com.smbtech.serviceframework.httpclient.domain.ObservabilityPolicy;
import com.smbtech.serviceframework.httpclient.domain.PoolingPolicy;
import com.smbtech.serviceframework.httpclient.domain.ResiliencePolicy;
import com.smbtech.serviceframework.httpclient.domain.RetryPolicy;
import com.smbtech.serviceframework.httpclient.domain.SslPolicy;
import com.smbtech.serviceframework.httpclient.domain.TimeoutPolicy;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class RestClientPropertiesMapper {

    private final CredentialResolver credentialResolver;

    public RestClientPropertiesMapper() {
        this.credentialResolver = null;
    }

    public RestClientPropertiesMapper(CredentialResolver credentialResolver) {
        this.credentialResolver = credentialResolver;
    }

    public Map<String, HttpClientDefinition> map(RestClientProperties properties) {
        Map<String, HttpClientDefinition> definitions = new LinkedHashMap<>();

        properties.getClients().forEach((name, client) -> {
            if (!client.isEnabled()) {
                return;
            }

            definitions.put(name, new HttpClientDefinition(
                    name,
                    client.getBeanName(),
                    toUri(client.getBaseUrl()),
                    client.getClientType(),
                    client.getAuthenticationType(),
                    new BasicAuthentication(
                            resolve(
                                    basicAuthentication(client).getUsername(),
                                    basicAuthentication(client).getUsernameRef(),
                                    "basic authentication username"
                            ),
                            resolve(
                                    basicAuthentication(client).getPassword(),
                                    basicAuthentication(client).getPasswordRef(),
                                    "basic authentication password"
                            )
                    ),
                    client.getCredentialTokenRequestorId(),
                    client.getScopes(),
                    new TimeoutPolicy(
                            client.getTimeout().getConnectTimeout(),
                            client.getTimeout().getConnectionRequestTimeout(),
                            client.getTimeout().getResponseTimeout()
                    ),
                    new PoolingPolicy(
                            client.getPooling().getConnectionReusePolicy(),
                            client.getPooling().getKeepAlive(),
                            client.getPooling().getMaxConnections(),
                            client.getPooling().getMaxConnectionsPerRoute(),
                            client.getPooling().isTcpKeepAlive()
                    ),
                    new ApacheHttpClientPolicy(
                            apache(client).isHostnameVerificationEnabled(),
                            apache(client).getValidateAfterInactivity(),
                            apache(client).getConnectionTimeToLive(),
                            new SslPolicy(
                                    ssl(client).isEnabled(),
                                    ssl(client).getTrustStoreId(),
                                    ssl(client).getKeyStoreId()
                            )
                    ),
                    new ErrorHandlingPolicy(
                            errorHandling(client).isEnabled(),
                            errorHandling(client).isIncludeBody(),
                            errorHandling(client).getMaxBodySize(),
                            errorHandling(client).isIncludeHeaders(),
                            errorHandling(client).isIncludeNotificationMetadata(),
                            errorHandling(client).getNotificationCodePrefix()
                    ),
                    new ObservabilityPolicy(
                            observability(client).isEnabled(),
                            observability(client).getMetricName(),
                            observability(client).isIncludeUri(),
                            observability(client).isIncludeStatus(),
                            observability(client).isIncludeException(),
                            observability(client).getTags()
                    ),
                    new ResiliencePolicy(
                            resilience(client).isEnabled(),
                            new RetryPolicy(
                                    retry(client).isEnabled(),
                                    retry(client).getMaxAttempts(),
                                    retry(client).getBackoff(),
                                    retry(client).isRetryOnServerErrors(),
                                    retry(client).isRetryOnExceptions(),
                                    retry(client).getRetryOnStatuses()
                            ),
                            new CircuitBreakerPolicy(
                                    circuitBreaker(client).isEnabled(),
                                    circuitBreaker(client).getFailureThreshold(),
                                    circuitBreaker(client).getOpenDuration()
                            )
                    ),
                    new AuditPolicy(
                            client.getAudit().isEnabled(),
                            client.getAudit().isIncludeRequest(),
                            client.getAudit().isIncludeResponse(),
                            client.getAudit().isIncludeHeaders(),
                            client.getAudit().isIncludeBody(),
                            client.getAudit().getMaxBodySize()
                    ),
                    client.getDefaultHeaders()
            ));
        });

        return definitions;
    }

    private URI toUri(String baseUrl) {
        return baseUrl == null || baseUrl.isBlank() ? null : URI.create(baseUrl);
    }

    private RestClientProperties.BasicAuthentication basicAuthentication(RestClientProperties.Client client) {
        return Objects.requireNonNullElseGet(
                client.getBasicAuthentication(),
                RestClientProperties.BasicAuthentication::new
        );
    }

    private String resolve(String directValue, String credentialRef, String fieldName) {
        if (credentialResolver == null) {
            return directValue;
        }
        return credentialResolver.resolve(directValue, credentialRef, fieldName);
    }

    private RestClientProperties.Apache apache(RestClientProperties.Client client) {
        return Objects.requireNonNullElseGet(client.getApache(), RestClientProperties.Apache::new);
    }

    private RestClientProperties.Ssl ssl(RestClientProperties.Client client) {
        return Objects.requireNonNullElseGet(apache(client).getSsl(), RestClientProperties.Ssl::new);
    }

    private RestClientProperties.ErrorHandling errorHandling(RestClientProperties.Client client) {
        return Objects.requireNonNullElseGet(client.getErrorHandling(), RestClientProperties.ErrorHandling::new);
    }

    private RestClientProperties.Observability observability(RestClientProperties.Client client) {
        return Objects.requireNonNullElseGet(client.getObservability(), RestClientProperties.Observability::new);
    }

    private RestClientProperties.Resilience resilience(RestClientProperties.Client client) {
        return Objects.requireNonNullElseGet(client.getResilience(), RestClientProperties.Resilience::new);
    }

    private RestClientProperties.Retry retry(RestClientProperties.Client client) {
        return Objects.requireNonNullElseGet(resilience(client).getRetry(), RestClientProperties.Retry::new);
    }

    private RestClientProperties.CircuitBreaker circuitBreaker(RestClientProperties.Client client) {
        return Objects.requireNonNullElseGet(
                resilience(client).getCircuitBreaker(),
                RestClientProperties.CircuitBreaker::new
        );
    }
}
