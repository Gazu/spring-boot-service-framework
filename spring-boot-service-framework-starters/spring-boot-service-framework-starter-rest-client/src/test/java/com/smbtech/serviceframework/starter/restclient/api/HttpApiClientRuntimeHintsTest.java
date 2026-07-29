package com.smbtech.serviceframework.starter.restclient.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.aop.SpringProxy;
import org.springframework.aop.framework.Advised;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.core.DecoratingProxy;

class HttpApiClientRuntimeHintsTest {

    @Test
    void registersProxyMethodsAndBindingTypes() throws Exception {
        RuntimeHints hints = new RuntimeHints();

        HttpApiClientRuntimeHints.register(hints, OrdersApi.class);

        assertThat(
                        RuntimeHintsPredicates.proxies()
                                .forInterfaces(
                                        OrdersApi.class,
                                        SpringProxy.class,
                                        Advised.class,
                                        DecoratingProxy.class))
                .accepts(hints);
        assertThat(
                        RuntimeHintsPredicates.reflection()
                                .onMethodInvocation(
                                        OrdersApi.class.getMethod("create", CreateOrder.class)))
                .accepts(hints);
        assertThat(RuntimeHintsPredicates.reflection().onType(CreateOrder.class)).accepts(hints);
        assertThat(RuntimeHintsPredicates.reflection().onType(Order.class)).accepts(hints);
    }

    @Test
    void rejectsNonInterfaceTypes() {
        assertThatThrownBy(
                        () ->
                                HttpApiClientRuntimeHints.register(
                                        new RuntimeHints(), CreateOrder.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be an interface");
    }

    interface OrdersApi {
        Order create(CreateOrder request);
    }

    record CreateOrder(String customerId) {}

    record Order(String id) {}
}
