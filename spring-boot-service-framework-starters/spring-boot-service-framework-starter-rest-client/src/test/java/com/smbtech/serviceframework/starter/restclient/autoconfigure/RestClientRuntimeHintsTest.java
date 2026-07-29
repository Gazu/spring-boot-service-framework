package com.smbtech.serviceframework.starter.restclient.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import com.smbtech.serviceframework.starter.restclient.adapter.out.spring.ConfiguredRestClientFactoryBean;
import org.junit.jupiter.api.Test;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;

class RestClientRuntimeHintsTest {

    @Test
    void registersProgrammaticFactoryBeanConstructor() throws Exception {
        RuntimeHints hints = new RuntimeHints();

        new RestClientRuntimeHints().registerHints(hints, getClass().getClassLoader());

        assertThat(
                        RuntimeHintsPredicates.reflection()
                                .onConstructorInvocation(
                                        ConfiguredRestClientFactoryBean.class.getConstructor(
                                                String.class)))
                .accepts(hints);
    }
}
