package com.smbtech.examples.errorhandling.application;

import com.smbtech.examples.errorhandling.api.CreateOrderRequest;
import com.smbtech.examples.errorhandling.api.OrderResponse;
import com.smbtech.examples.errorhandling.domain.OrderErrors;
import com.smbtech.serviceframework.error.ServiceException;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    public OrderResponse create(CreateOrderRequest request) {
        return new OrderResponse("order-100", request.customerId(), request.amount());
    }

    public OrderResponse find(String orderId) {
        if ("missing".equals(orderId)) {
            throw ServiceException.from(
                    OrderErrors.ORDER_NOT_FOUND, "Order lookup failed for internal id " + orderId);
        }
        if ("completed".equals(orderId)) {
            throw ServiceException.from(OrderErrors.ORDER_ALREADY_COMPLETED);
        }
        return new OrderResponse(orderId, "customer-100", new BigDecimal("29.90"));
    }

    public void failUnexpectedly() {
        throw new IllegalStateException(
                "Database password=internal-secret failed in OrderRepository");
    }
}
