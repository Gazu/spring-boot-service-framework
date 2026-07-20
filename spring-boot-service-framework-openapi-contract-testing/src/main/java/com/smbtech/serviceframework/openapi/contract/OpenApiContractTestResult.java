package com.smbtech.serviceframework.openapi.contract;

import java.util.List;
import java.util.Objects;

/**
 * Carries immutable open api contract test result data.
 *
 * @param violations violations value
 */
public record OpenApiContractTestResult(List<OpenApiContractViolation> violations) {

    /** Creates and validates the record components. */
    public OpenApiContractTestResult {
        violations = List.copyOf(Objects.requireNonNull(violations, "violations"));
    }

    /**
     * Reports whether valid.
     *
     * @return is valid result
     */
    public boolean isValid() {
        return violations.isEmpty();
    }

    /** Performs the throw if invalid operation. */
    public void throwIfInvalid() {
        if (!isValid()) {
            throw new AssertionError(formatMessage());
        }
    }

    /**
     * Performs the format message operation.
     *
     * @return format message result
     */
    public String formatMessage() {
        if (isValid()) {
            return "OpenAPI contract is valid";
        }
        StringBuilder message = new StringBuilder("OpenAPI contract violations:");
        violations.forEach(
                violation ->
                        message.append(System.lineSeparator())
                                .append("- [")
                                .append(violation.code())
                                .append("] ")
                                .append(violation.operationId())
                                .append(" ")
                                .append(violation.location())
                                .append(": ")
                                .append(violation.message()));
        return message.toString();
    }
}
