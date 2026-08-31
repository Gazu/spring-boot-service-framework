package com.smbtech.serviceframework.starter.mock.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.web.context.request.NativeWebRequest;

class MockRuntimeHintsTest {

    @Test
    void registersDynamicHandlerAndConventionalResources() throws Exception {
        RuntimeHints hints = new RuntimeHints();

        new MockRuntimeHints().registerHints(hints, getClass().getClassLoader());

        Method handler =
                Class.forName(
                                "com.smbtech.serviceframework.starter.mock.adapter.in.openapi.OpenApiMockEndpoint")
                        .getMethod("handle", NativeWebRequest.class);

        assertThat(RuntimeHintsPredicates.reflection().onMethodInvocation(handler)).accepts(hints);
        assertThat(RuntimeHintsPredicates.resource().forResource("mocks/orders.json"))
                .accepts(hints);
        assertThat(RuntimeHintsPredicates.resource().forResource("openapi/orders.yaml"))
                .accepts(hints);
    }
}
