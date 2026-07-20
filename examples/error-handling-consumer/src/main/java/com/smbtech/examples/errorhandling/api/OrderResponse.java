package com.smbtech.examples.errorhandling.api;

import java.math.BigDecimal;

public record OrderResponse(String orderId, String customerId, BigDecimal amount) {}
