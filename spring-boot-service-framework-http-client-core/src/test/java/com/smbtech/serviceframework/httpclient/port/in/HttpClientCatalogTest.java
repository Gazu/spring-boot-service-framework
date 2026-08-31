package com.smbtech.serviceframework.httpclient.port.in;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.smbtech.serviceframework.httpclient.domain.ApacheHttpClientPolicy;
import com.smbtech.serviceframework.httpclient.domain.AuditPolicy;
import com.smbtech.serviceframework.httpclient.domain.AuthenticationType;
import com.smbtech.serviceframework.httpclient.domain.BasicAuthentication;
import com.smbtech.serviceframework.httpclient.domain.ClientType;
import com.smbtech.serviceframework.httpclient.domain.ErrorHandlingPolicy;
import com.smbtech.serviceframework.httpclient.domain.HttpClientDefinition;
import com.smbtech.serviceframework.httpclient.domain.ObservabilityPolicy;
import com.smbtech.serviceframework.httpclient.domain.PoolingPolicy;
import com.smbtech.serviceframework.httpclient.domain.ResiliencePolicy;
import com.smbtech.serviceframework.httpclient.domain.TimeoutPolicy;
import com.smbtech.serviceframework.httpclient.exception.HttpClientConfigurationException;
import java.net.URI;
import java.util.Map;
import org.junit.jupiter.api.Test;

class HttpClientCatalogTest {

    @Test
    void loadsAndValidatesConfiguredDefinitions() {
        HttpClientDefinition definition =
                definition("projects", URI.create("https://projects.example"));

        HttpClientCatalog catalog =
                HttpClientCatalog.from(
                        () -> Map.of("projects", definition),
                        HttpClientDefinitionValidator.defaultValidator());

        assertEquals("projectsRestClient", catalog.requireByName("projects").beanName());
        assertEquals(
                "https://projects.example", catalog.requireByName("projects").baseUrl().toString());
    }

    @Test
    void failsWhenBaseUrlIsMissing() {
        HttpClientDefinition definition = definition("broken", null);

        assertThrows(
                HttpClientConfigurationException.class,
                () ->
                        HttpClientCatalog.from(
                                () -> Map.of("broken", definition),
                                HttpClientDefinitionValidator.defaultValidator()));
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
                Map.of());
    }
}
