package com.smbtech.serviceframework.starter.restclient.autoconfigure;

import com.smbtech.serviceframework.starter.restclient.api.RestClientRegistry;
import org.springframework.web.client.RestClient;

final class DynamicRestClientFactory {

    private DynamicRestClientFactory() {}

    static RestClient create(RestClientRegistry registry, String clientName) {
        return registry.get(clientName);
    }
}
