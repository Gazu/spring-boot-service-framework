package com.smbtech.serviceframework.httpclient.service;

import com.smbtech.serviceframework.httpclient.domain.AuthenticationType;
import com.smbtech.serviceframework.httpclient.domain.ApacheHttpClientPolicy;
import com.smbtech.serviceframework.httpclient.domain.AuditPolicy;
import com.smbtech.serviceframework.httpclient.domain.BasicAuthentication;
import com.smbtech.serviceframework.httpclient.domain.ClientType;
import com.smbtech.serviceframework.httpclient.domain.ErrorHandlingPolicy;
import com.smbtech.serviceframework.httpclient.domain.HttpClientDefinition;
import com.smbtech.serviceframework.httpclient.domain.ObservabilityPolicy;
import com.smbtech.serviceframework.httpclient.domain.PoolingPolicy;
import com.smbtech.serviceframework.httpclient.domain.ResiliencePolicy;
import com.smbtech.serviceframework.httpclient.domain.TimeoutPolicy;
import com.smbtech.serviceframework.httpclient.exception.HttpClientConfigurationException;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultHttpClientCatalogTest {

    @Test
    void loadsAndValidatesConfiguredDefinitions() {
        HttpClientDefinition definition = definition("projects", URI.create("https://projects.example"));

        DefaultHttpClientCatalog catalog = new DefaultHttpClientCatalog(
                () -> Map.of("projects", definition),
                new DefaultHttpClientDefinitionValidator()
        );

        assertEquals("projectsRestClient", catalog.requireByName("projects").beanName());
        assertEquals("https://projects.example", catalog.requireByName("projects").baseUrl().toString());
    }

    @Test
    void failsWhenBaseUrlIsMissing() {
        HttpClientDefinition definition = definition("broken", null);

        assertThrows(HttpClientConfigurationException.class, () -> new DefaultHttpClientCatalog(
                () -> Map.of("broken", definition),
                new DefaultHttpClientDefinitionValidator()
        ));
    }

    private HttpClientDefinition definition(String name, URI baseUrl) {
        return new HttpClientDefinition(
                name,
                null,
                baseUrl,
                ClientType.DEFAULT,
                AuthenticationType.NO_AUTH,
                new BasicAuthentication("", ""),
                "",
                "",
                TimeoutPolicy.defaults(),
                PoolingPolicy.defaults(),
                ApacheHttpClientPolicy.defaults(),
                ErrorHandlingPolicy.defaults(),
                ObservabilityPolicy.defaults(),
                ResiliencePolicy.disabled(),
                AuditPolicy.disabled(),
                Map.of()
        );
    }
}
