package com.smbtech.examples.restclient.adapter.out.payments;

import com.smbtech.serviceframework.starter.restclient.api.HttpApiClient;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpApiClient("payments")
@HttpExchange
public interface PaymentsApi {

    @GetExchange("/dummy")
    String dummy();
}
