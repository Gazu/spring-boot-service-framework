package com.smbtech.serviceframework.openapi.contract;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Carries immutable open api operation data.
 *
 * @param operationId operation id value
 * @param method method value
 * @param pathTemplate path template value
 * @param requiredPathParameters required path parameters value
 * @param responses responses value
 */
public record OpenApiOperation(
        String operationId,
        String method,
        String pathTemplate,
        List<String> requiredPathParameters,
        Map<Integer, OpenApiResponse> responses) {

    /** Creates and validates the record components. */
    public OpenApiOperation {
        operationId = requireText(operationId, "operationId");
        method = requireText(method, "method").toUpperCase(java.util.Locale.ROOT);
        pathTemplate = requireText(pathTemplate, "pathTemplate");
        requiredPathParameters =
                List.copyOf(
                        Objects.requireNonNull(requiredPathParameters, "requiredPathParameters"));
        responses = Map.copyOf(Objects.requireNonNull(responses, "responses"));
    }

    /**
     * Performs the successful response operation.
     *
     * @return successful response result
     */
    public OpenApiResponse successfulResponse() {
        return responses.entrySet().stream()
                .filter(entry -> entry.getKey() >= 200 && entry.getKey() < 300)
                .sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue)
                .findFirst()
                .orElseThrow(
                        () ->
                                new IllegalStateException(
                                        "operation "
                                                + operationId
                                                + " does not declare a successful response"));
    }

    private static String requireText(String value, String name) {
        String safeValue = Objects.requireNonNull(value, name).trim();
        if (safeValue.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return safeValue;
    }
}
