package com.smbtech.examples.restclient.application;

import com.smbtech.examples.restclient.adapter.out.payments.PaymentsApi;
import com.smbtech.serviceframework.starter.restclient.api.ApiClientFactory;
import org.springframework.stereotype.Service;

@Service
public class PaymentsService {

    private final PaymentsApi paymentsApi;

    public PaymentsService(ApiClientFactory apiClientFactory) {
        this.paymentsApi = apiClientFactory.create(PaymentsApi.class);
    }

    public String dummy() {
        return paymentsApi.dummy();
    }
}
