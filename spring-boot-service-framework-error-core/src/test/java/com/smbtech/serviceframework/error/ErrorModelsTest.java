package com.smbtech.serviceframework.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.smbtech.serviceframework.commons.notification.Notification;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ErrorModelsTest {

    @Test
    void normalizesFieldViolationValues() {
        FieldViolation violation = new FieldViolation(" customerId ", " required ", null);

        assertEquals("customerId", violation.fieldName());
        assertEquals("required", violation.code());
        assertEquals("", violation.message());
    }

    @Test
    void supportsObjectLevelViolationsAndRejectsMissingCodes() {
        FieldViolation violation =
                new FieldViolation(null, "invalid_request", "Request is invalid");

        assertEquals("", violation.fieldName());
        assertThrows(
                IllegalArgumentException.class,
                () -> new FieldViolation("customerId", " ", "Invalid"));
    }

    @Test
    void createsImmutableResolvedError() {
        Notification notification =
                Notification.error("E_REQUEST_0001", "Request validation failed");
        FieldViolation violation =
                new FieldViolation("customerId", "required", "customerId is required");
        List<FieldViolation> violations = new ArrayList<>(List.of(violation));

        ResolvedError resolvedError =
                new ResolvedError(
                        notification,
                        ErrorCategory.VALIDATION,
                        ErrorExposure.PUBLIC,
                        "Bean validation rejected the request",
                        violations);
        violations.clear();

        assertEquals(notification, resolvedError.notification());
        assertEquals(ErrorCategory.VALIDATION, resolvedError.category());
        assertEquals(ErrorExposure.PUBLIC, resolvedError.exposure());
        assertEquals("Bean validation rejected the request", resolvedError.diagnosticMessage());
        assertEquals(List.of(violation), resolvedError.fieldViolations());
        assertTrue(resolvedError.hasFieldViolations());
        assertThrows(
                UnsupportedOperationException.class,
                () -> resolvedError.fieldViolations().add(violation));
    }

    @Test
    void copyMethodsPreserveUnchangedErrorData() {
        Notification notification = Notification.error("E_REQUEST_0001", "Invalid request");
        ResolvedError source =
                new ResolvedError(
                        notification,
                        ErrorCategory.VALIDATION,
                        ErrorExposure.INTERNAL,
                        "diagnostic");
        Notification replacement = notification.withMetadata(java.util.Map.of("field", "name"));
        FieldViolation violation = new FieldViolation("name", "required", "Name is required");

        ResolvedError updated =
                source.withNotification(replacement)
                        .withExposure(ErrorExposure.PUBLIC)
                        .withFieldViolations(List.of(violation));

        assertEquals(notification, source.notification());
        assertEquals(ErrorExposure.INTERNAL, source.exposure());
        assertEquals(replacement, updated.notification());
        assertEquals(ErrorCategory.VALIDATION, updated.category());
        assertEquals(ErrorExposure.PUBLIC, updated.exposure());
        assertEquals("diagnostic", updated.diagnosticMessage());
        assertEquals(List.of(violation), updated.fieldViolations());
    }

    @Test
    void appliesSafeDefaults() {
        ResolvedError resolvedError =
                new ResolvedError(
                        Notification.error("E_INTERNAL_0001", "Request could not be completed"),
                        ErrorCategory.INTERNAL,
                        null,
                        null,
                        null);

        assertEquals(ErrorExposure.INTERNAL, resolvedError.exposure());
        assertEquals("", resolvedError.diagnosticMessage());
        assertTrue(resolvedError.fieldViolations().isEmpty());
        assertFalse(resolvedError.hasFieldViolations());
    }

    @Test
    void requiresNotificationAndCategory() {
        Notification notification = Notification.error("E_INTERNAL_0001", "Failure");

        assertThrows(
                NullPointerException.class,
                () -> new ResolvedError(null, ErrorCategory.INTERNAL, ErrorExposure.INTERNAL, ""));
        assertThrows(
                NullPointerException.class,
                () -> new ResolvedError(notification, null, ErrorExposure.INTERNAL, ""));
    }
}
