package com.smbtech.serviceframework.starter.mock.adapter.in.openapi;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.request.NativeWebRequest;

/** Provides open api mock endpoint behavior. */
public final class OpenApiMockEndpoint {

    private static final byte[] INVALID_STATUS_BODY =
            "{\"error\":\"undeclared_mock_status\"}".getBytes(StandardCharsets.UTF_8);

    private final OpenApiMockOperation operation;
    private final String statusHeader;

    OpenApiMockEndpoint(OpenApiMockOperation operation, String statusHeader) {
        this.operation = operation;
        this.statusHeader = statusHeader;
    }

    /**
     * Performs the handle operation.
     *
     * @param request request value
     * @return handle result
     */
    public ResponseEntity<byte[]> handle(NativeWebRequest request) {
        Integer selectedStatus = selectedStatus(request.getHeader(statusHeader));
        if (selectedStatus == null) {
            return ResponseEntity.badRequest()
                    .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                    .body(INVALID_STATUS_BODY.clone());
        }
        OpenApiMockResponse response = operation.responses().get(selectedStatus);
        if (response == null) {
            return ResponseEntity.badRequest()
                    .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                    .body(INVALID_STATUS_BODY.clone());
        }
        delay();
        HttpHeaders headers = new HttpHeaders();
        response.headers().forEach((name, values) -> headers.put(name, List.copyOf(values)));
        if (!response.contentType().isBlank()) {
            headers.setContentType(MediaType.parseMediaType(response.contentType()));
        }
        headers.set("X-Mock-Operation-Id", operation.operationId());
        byte[] body = operation.method() == RequestMethod.HEAD ? new byte[0] : response.body();
        return new ResponseEntity<>(body, headers, HttpStatusCode.valueOf(response.status()));
    }

    private Integer selectedStatus(String value) {
        if (value == null || value.isBlank()) {
            return operation.defaultStatus();
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private void delay() {
        if (operation.delay().isZero() || operation.delay().isNegative()) {
            return;
        }
        try {
            Thread.sleep(operation.delay());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
