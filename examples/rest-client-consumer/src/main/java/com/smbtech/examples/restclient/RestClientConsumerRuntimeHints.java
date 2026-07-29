package com.smbtech.examples.restclient;

import com.smbtech.examples.restclient.adapter.out.payments.PaymentsApi;
import com.smbtech.serviceframework.starter.restclient.api.HttpApiClientRuntimeHints;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

final class RestClientConsumerRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        HttpApiClientRuntimeHints.register(hints, PaymentsApi.class);
    }
}
