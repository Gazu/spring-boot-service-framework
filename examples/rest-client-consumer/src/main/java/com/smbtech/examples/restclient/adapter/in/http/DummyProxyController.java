package com.smbtech.examples.restclient.adapter.in.http;

import com.smbtech.examples.restclient.application.PaymentsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dummy")
public class DummyProxyController {

    private final PaymentsService paymentsService;

    public DummyProxyController(PaymentsService paymentsService) {
        this.paymentsService = paymentsService;
    }

    @GetMapping
    public String dummy() {
        return paymentsService.dummy();
    }
}
