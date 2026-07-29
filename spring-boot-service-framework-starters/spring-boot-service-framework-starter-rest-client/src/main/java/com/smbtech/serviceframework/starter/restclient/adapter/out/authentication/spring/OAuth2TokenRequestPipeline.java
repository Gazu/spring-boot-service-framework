package com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring;

import com.smbtech.serviceframework.starter.restclient.api.oauth2.OAuth2TokenRequestContext;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.client.endpoint.AbstractOAuth2AuthorizationGrantRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

final class OAuth2TokenRequestPipeline {

    private final OAuth2ExtensionRegistry extensionRegistry;

    OAuth2TokenRequestPipeline(OAuth2ExtensionRegistry extensionRegistry) {
        this.extensionRegistry =
                Objects.requireNonNullElseGet(extensionRegistry, OAuth2ExtensionRegistry::empty);
    }

    MultiValueMap<String, String> resolveParameters(
            AbstractOAuth2AuthorizationGrantRequest grantRequest,
            MultiValueMap<String, String> parameters) {
        OAuth2TokenRequestContext context = resolve(grantRequest, parameters, Map.of());
        return toMultiValueMap(context.parameters());
    }

    HttpHeaders resolveHeaders(
            AbstractOAuth2AuthorizationGrantRequest grantRequest, HttpHeaders headers) {
        OAuth2TokenRequestContext context =
                resolve(grantRequest, new LinkedMultiValueMap<>(), toHeadersMap(headers));
        HttpHeaders resolvedHeaders = new HttpHeaders();
        context.headers().forEach(resolvedHeaders::set);
        return resolvedHeaders;
    }

    private OAuth2TokenRequestContext resolve(
            AbstractOAuth2AuthorizationGrantRequest grantRequest,
            MultiValueMap<String, String> parameters,
            Map<String, String> headers) {
        ClientRegistration registration = grantRequest.getClientRegistration();
        OAuth2TokenRequestContext context =
                new OAuth2TokenRequestContext(
                        registration.getRegistrationId(),
                        OAuth2RegistrationValues.grantType(grantRequest),
                        OAuth2RegistrationValues.clientAuthenticationMethod(registration),
                        OAuth2RegistrationValues.tokenUri(registration),
                        registration.getScopes(),
                        toParametersMap(parameters),
                        headers);
        for (var customizer : extensionRegistry.tokenRequestCustomizers()) {
            context =
                    sanitize(
                            Objects.requireNonNull(
                                    customizer.customize(context),
                                    "OAuth2TokenRequestCustomizer must not return null"));
        }
        return context;
    }

    private OAuth2TokenRequestContext sanitize(OAuth2TokenRequestContext context) {
        return new OAuth2TokenRequestContext(
                context.registrationId(),
                context.grantType(),
                context.clientAuthenticationMethod(),
                context.tokenUri(),
                context.scopes(),
                sanitizeParameters(context.parameters()),
                sanitizeHeaders(context.headers()));
    }

    private Map<String, Object> sanitizeParameters(Map<String, Object> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<String, Object> sanitized = new LinkedHashMap<>();
        parameters.forEach(
                (name, value) -> {
                    if (name != null && !name.isBlank() && value != null) {
                        sanitized.put(name.trim(), value);
                    }
                });
        return Map.copyOf(sanitized);
    }

    private Map<String, String> sanitizeHeaders(Map<String, String> headers) {
        if (headers == null || headers.isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<String, String> sanitized = new LinkedHashMap<>();
        headers.forEach(
                (name, value) -> {
                    if (name != null && !name.isBlank() && value != null && !value.isBlank()) {
                        sanitized.put(name.trim(), value);
                    }
                });
        return Map.copyOf(sanitized);
    }

    private Map<String, Object> toParametersMap(MultiValueMap<String, String> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<String, Object> values = new LinkedHashMap<>();
        parameters.forEach(
                (name, parameterValues) -> {
                    if (name != null
                            && !name.isBlank()
                            && parameterValues != null
                            && !parameterValues.isEmpty()) {
                        values.put(
                                name.trim(),
                                parameterValues.size() == 1
                                        ? parameterValues.getFirst()
                                        : List.copyOf(parameterValues));
                    }
                });
        return Map.copyOf(values);
    }

    private Map<String, String> toHeadersMap(HttpHeaders headers) {
        if (headers == null || headers.isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        headers.headerSet()
                .forEach(
                        entry -> {
                            if (!entry.getValue().isEmpty()) {
                                values.put(entry.getKey(), entry.getValue().getFirst());
                            }
                        });
        return Map.copyOf(values);
    }

    private MultiValueMap<String, String> toMultiValueMap(Map<String, Object> parameters) {
        LinkedMultiValueMap<String, String> values = new LinkedMultiValueMap<>();
        parameters.forEach(
                (name, value) -> {
                    if (value instanceof Iterable<?> iterable) {
                        iterable.forEach(item -> addValue(values, name, item));
                    } else if (value instanceof Object[] array) {
                        for (Object item : array) {
                            addValue(values, name, item);
                        }
                    } else {
                        addValue(values, name, value);
                    }
                });
        return values;
    }

    private void addValue(MultiValueMap<String, String> values, String name, Object value) {
        if (value != null) {
            values.add(name, value.toString());
        }
    }
}
