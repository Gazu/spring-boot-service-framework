package com.smbtech.serviceframework.starter.mock.adapter.in.openapi;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import org.springframework.web.bind.annotation.RequestMethod;

record OpenApiMockOperation(
        String operationId,
        RequestMethod method,
        String path,
        int defaultStatus,
        Map<Integer, OpenApiMockResponse> responses,
        Duration delay) {

    OpenApiMockOperation {
        operationId = Objects.requireNonNullElse(operationId, "");
        method = Objects.requireNonNull(method, "method");
        path = Objects.requireNonNull(path, "path");
        responses = Map.copyOf(Objects.requireNonNull(responses, "responses"));
        delay = Objects.requireNonNullElse(delay, Duration.ZERO);
    }
}
