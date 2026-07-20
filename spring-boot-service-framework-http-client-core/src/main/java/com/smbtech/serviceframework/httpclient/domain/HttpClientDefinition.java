package com.smbtech.serviceframework.httpclient.domain;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Carries immutable HTTP client definition data.
 *
 * @param name name value
 * @param beanName bean name value
 * @param baseUrl base url value
 * @param clientType client type value
 * @param authenticationType authentication type value
 * @param basicAuthentication basic authentication value
 * @param tokenRequestId token request id value
 * @param scopes scopes value
 * @param timeout timeout value
 * @param pooling pooling value
 * @param apache apache value
 * @param errorHandling error handling value
 * @param observability observability value
 * @param resilience resilience value
 * @param audit audit value
 * @param defaultHeaders default headers value
 */
public record HttpClientDefinition(
        String name,
        String beanName,
        URI baseUrl,
        ClientType clientType,
        AuthenticationType authenticationType,
        BasicAuthentication basicAuthentication,
        String tokenRequestId,
        String scopes,
        TimeoutPolicy timeout,
        PoolingPolicy pooling,
        ApacheHttpClientPolicy apache,
        ErrorHandlingPolicy errorHandling,
        ObservabilityPolicy observability,
        ResiliencePolicy resilience,
        AuditPolicy audit,
        Map<String, String> defaultHeaders) {
    /** Suffix applied when deriving a Spring {@code RestClient} bean name from the client name. */
    public static final String REST_CLIENT_BEAN_SUFFIX = "RestClient";

    /** Creates and validates the record components. */
    public HttpClientDefinition {
        name = Objects.requireNonNullElse(name, "").trim();
        beanName = normalizeBeanName(name, beanName);
        clientType = Objects.requireNonNullElse(clientType, ClientType.DEFAULT);
        authenticationType =
                Objects.requireNonNullElse(authenticationType, AuthenticationType.NO_AUTH);
        basicAuthentication =
                Objects.requireNonNullElseGet(
                        basicAuthentication, () -> new BasicAuthentication("", ""));
        tokenRequestId = Objects.requireNonNullElse(tokenRequestId, "");
        scopes = Objects.requireNonNullElse(scopes, "");
        timeout = Objects.requireNonNullElseGet(timeout, TimeoutPolicy::defaults);
        pooling = Objects.requireNonNullElseGet(pooling, PoolingPolicy::defaults);
        apache = Objects.requireNonNullElseGet(apache, ApacheHttpClientPolicy::defaults);
        errorHandling = Objects.requireNonNullElseGet(errorHandling, ErrorHandlingPolicy::defaults);
        observability = Objects.requireNonNullElseGet(observability, ObservabilityPolicy::defaults);
        resilience = Objects.requireNonNullElseGet(resilience, ResiliencePolicy::disabled);
        audit = Objects.requireNonNullElseGet(audit, AuditPolicy::disabled);
        defaultHeaders =
                Map.copyOf(
                        new LinkedHashMap<>(Objects.requireNonNullElse(defaultHeaders, Map.of())));
    }

    private static String normalizeBeanName(String name, String beanName) {
        if (beanName != null && !beanName.isBlank()) {
            return beanName.trim();
        }
        if (name == null || name.isBlank()) {
            return "";
        }
        return name.trim() + REST_CLIENT_BEAN_SUFFIX;
    }
}
