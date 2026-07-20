package com.smbtech.serviceframework.starter.restclient;

import static org.assertj.core.api.Assertions.assertThat;

import com.smbtech.serviceframework.starter.restclient.api.RestClientRegistry;
import com.smbtech.serviceframework.starter.restclient.api.customizer.ApacheHttpClientBuilderCustomizer;
import com.smbtech.serviceframework.starter.restclient.api.customizer.ClientHttpRequestFactoryCustomizer;
import com.smbtech.serviceframework.starter.restclient.api.customizer.RestClientBuilderCustomizer;
import com.smbtech.serviceframework.starter.restclient.autoconfigure.RestClientAutoConfiguration;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

class RestClientCustomizerTest {

    @Test
    void appliesCustomizersWhenRestClientIsCreated() {
        AtomicReference<String> restClientCustomized = new AtomicReference<>();
        AtomicReference<String> apacheCustomized = new AtomicReference<>();
        AtomicReference<Class<?>> requestFactoryType = new AtomicReference<>();

        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(RestClientAutoConfiguration.class))
                .withBean(
                        RestClientBuilderCustomizer.class,
                        () ->
                                (definition, builder) -> {
                                    restClientCustomized.set(definition.name());
                                    builder.defaultHeader(
                                            "X-Customized-By",
                                            "spring-boot-service-framework-test");
                                })
                .withBean(
                        ApacheHttpClientBuilderCustomizer.class,
                        () ->
                                (definition, builder) -> {
                                    apacheCustomized.set(definition.name());
                                    builder.setUserAgent("spring-boot-service-framework-test");
                                })
                .withBean(
                        ClientHttpRequestFactoryCustomizer.class,
                        () ->
                                (definition, requestFactory) ->
                                        requestFactoryType.set(requestFactory.getClass()))
                .withPropertyValues(
                        "smbtech.rest-clients.clients.projects.base-url=https://projects.example",
                        "smbtech.rest-clients.clients.projects.client-type=APACHE_HTTP")
                .run(
                        context -> {
                            context.getBean(RestClientRegistry.class).get("projects");

                            assertThat(restClientCustomized).hasValue("projects");
                            assertThat(apacheCustomized).hasValue("projects");
                            assertThat(requestFactoryType)
                                    .hasValue(HttpComponentsClientHttpRequestFactory.class);
                        });
    }
}
