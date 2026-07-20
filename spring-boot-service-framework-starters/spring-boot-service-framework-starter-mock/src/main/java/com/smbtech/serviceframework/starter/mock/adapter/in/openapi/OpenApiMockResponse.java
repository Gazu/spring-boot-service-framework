package com.smbtech.serviceframework.starter.mock.adapter.in.openapi;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

record OpenApiMockResponse(
        int status, String contentType, Map<String, List<String>> headers, byte[] body) {

    OpenApiMockResponse {
        contentType = Objects.requireNonNullElse(contentType, "");
        Map<String, List<String>> safeHeaders = new LinkedHashMap<>();
        Objects.requireNonNullElse(headers, Map.<String, List<String>>of())
                .forEach((name, values) -> safeHeaders.put(name, List.copyOf(values)));
        headers = Map.copyOf(safeHeaders);
        body = body == null ? new byte[0] : body.clone();
    }

    @Override
    public byte[] body() {
        return body.clone();
    }
}
