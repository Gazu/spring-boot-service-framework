package com.smbtech.serviceframework.httpclient.domain;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record HttpClientDefinition(
        String name,
        String beanName,
        URI baseUrl,
        ClientType clientType,
        AuthenticationType authenticationType,
        BasicAuthentication basicAuthentication,
        String credentialTokenRequestorId,
        String scopes,
        TimeoutPolicy timeout,
        PoolingPolicy pooling,
        ApacheHttpClientPolicy apache,
        ErrorHandlingPolicy errorHandling,
        ObservabilityPolicy observability,
        ResiliencePolicy resilience,
        AuditPolicy audit,
        //String mockKey,
        Map<String, String> defaultHeaders
) {
    public static final String REST_CLIENT_BEAN_SUFFIX = "RestClient";

    public HttpClientDefinition {
        name = Objects.requireNonNullElse(name, "").trim();
        beanName = normalizeBeanName(name, beanName);
        clientType = Objects.requireNonNullElse(clientType, ClientType.DEFAULT);
        authenticationType = Objects.requireNonNullElse(authenticationType, AuthenticationType.NO_AUTH);
        basicAuthentication = Objects.requireNonNullElseGet(basicAuthentication, () -> new BasicAuthentication("", ""));
        credentialTokenRequestorId = Objects.requireNonNullElse(credentialTokenRequestorId, "");
        scopes = Objects.requireNonNullElse(scopes, "");
        timeout = Objects.requireNonNullElseGet(timeout, TimeoutPolicy::defaults);
        pooling = Objects.requireNonNullElseGet(pooling, PoolingPolicy::defaults);
        apache = Objects.requireNonNullElseGet(apache, ApacheHttpClientPolicy::defaults);
        errorHandling = Objects.requireNonNullElseGet(errorHandling, ErrorHandlingPolicy::defaults);
        observability = Objects.requireNonNullElseGet(observability, ObservabilityPolicy::defaults);
        resilience = Objects.requireNonNullElseGet(resilience, ResiliencePolicy::disabled);
        audit = Objects.requireNonNullElseGet(audit, AuditPolicy::disabled);
        //mockKey = Objects.requireNonNullElse(mockKey, "");
        defaultHeaders = Map.copyOf(new LinkedHashMap<>(Objects.requireNonNullElse(defaultHeaders, Map.of())));
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
