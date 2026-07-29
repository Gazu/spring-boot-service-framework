package com.smbtech.serviceframework.starter.restclient.autoconfigure;

import com.smbtech.serviceframework.starter.restclient.adapter.out.spring.ConfiguredRestClientFactoryBean;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/** Native-image hints required by REST client infrastructure created programmatically. */
final class RestClientRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        hints.reflection()
                .registerType(
                        ConfiguredRestClientFactoryBean.class,
                        MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS);
    }
}
