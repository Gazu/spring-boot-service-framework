package com.smbtech.serviceframework.starter.restclient.adapter.out.spring;

import com.smbtech.serviceframework.starter.restclient.api.ApiClientFactory;
import com.smbtech.serviceframework.starter.restclient.api.HttpApiClient;
import com.smbtech.serviceframework.starter.restclient.api.RestClientRegistry;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/** Provides default api client factory behavior. */
final class DefaultApiClientFactory implements ApiClientFactory {

    private final RestClientRegistry restClientRegistry;
    private final Map<ApiClientKey, Object> cache = new ConcurrentHashMap<>();

    /**
     * Creates a default api client factory instance.
     *
     * @param restClientRegistry rest client registry value
     */
    public DefaultApiClientFactory(RestClientRegistry restClientRegistry) {
        this.restClientRegistry =
                Objects.requireNonNull(restClientRegistry, "restClientRegistry must not be null");
    }

    @Override
    public <T> T create(String clientName, Class<T> apiType) {
        String normalizedClientName = normalizeClientName(clientName);
        validateApiType(apiType);

        ApiClientKey key = new ApiClientKey(normalizedClientName, apiType);
        Object proxy =
                cache.computeIfAbsent(
                        key, current -> createProxy(current.clientName(), current.apiType()));
        return apiType.cast(proxy);
    }

    @Override
    public <T> T create(Class<T> apiType) {
        validateApiType(apiType);
        HttpApiClient annotation = apiType.getAnnotation(HttpApiClient.class);
        if (annotation == null || annotation.value().isBlank()) {
            throw new IllegalArgumentException(
                    "API type "
                            + apiType.getName()
                            + " must declare @HttpApiClient(\"clientName\")");
        }
        return create(annotation.value(), apiType);
    }

    private Object createProxy(String clientName, Class<?> apiType) {
        RestClientAdapter adapter = RestClientAdapter.create(restClientRegistry.get(clientName));
        return HttpServiceProxyFactory.builderFor(adapter).build().createClient(apiType);
    }

    private String normalizeClientName(String clientName) {
        if (clientName == null || clientName.isBlank()) {
            throw new IllegalArgumentException("clientName must not be blank");
        }
        return clientName.trim();
    }

    private void validateApiType(Class<?> apiType) {
        Objects.requireNonNull(apiType, "apiType must not be null");
        if (!apiType.isInterface()) {
            throw new IllegalArgumentException(
                    "API type must be an interface: " + apiType.getName());
        }
    }

    private record ApiClientKey(String clientName, Class<?> apiType) {}
}
