package com.smbtech.serviceframework.openapi.contract;

import java.util.Objects;

/**
 * Carries immutable open api contract violation data.
 *
 * @param code code value
 * @param operationId operation id value
 * @param location location value
 * @param message message value
 */
public record OpenApiContractViolation(
        OpenApiContractViolationCode code, String operationId, String location, String message) {

    /** Creates and validates the record components. */
    public OpenApiContractViolation {
        code = Objects.requireNonNull(code, "code");
        operationId = Objects.requireNonNull(operationId, "operationId");
        location = Objects.requireNonNull(location, "location");
        message = Objects.requireNonNull(message, "message");
    }
}
