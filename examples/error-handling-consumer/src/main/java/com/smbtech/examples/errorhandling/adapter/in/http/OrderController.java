package com.smbtech.examples.errorhandling.adapter.in.http;

import com.smbtech.examples.errorhandling.api.CreateOrderRequest;
import com.smbtech.examples.errorhandling.api.OrderResponse;
import com.smbtech.examples.errorhandling.application.OrderService;
import com.smbtech.examples.errorhandling.infrastructure.PaymentsGateway;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class OrderController {

    private final OrderService orderService;
    private final PaymentsGateway paymentsGateway;

    public OrderController(OrderService orderService, PaymentsGateway paymentsGateway) {
        this.orderService = orderService;
        this.paymentsGateway = paymentsGateway;
    }

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse create(@Valid @RequestBody CreateOrderRequest request) {
        return orderService.create(request);
    }

    @GetMapping("/orders/{orderId}")
    public OrderResponse find(@PathVariable String orderId) {
        return orderService.find(orderId);
    }

    @GetMapping("/simulations/downstream")
    public void downstream() {
        paymentsGateway.authorize();
    }

    @GetMapping("/simulations/unexpected")
    public void unexpected() {
        orderService.failUnexpectedly();
    }

    @GetMapping("/secure/profile")
    public String profile() {
        return "authenticated";
    }

    @GetMapping("/secure/admin")
    public String admin() {
        return "admin";
    }
}
