package com.smbtech.serviceframework.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.smbtech.serviceframework.commons.notification.Notification;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class NotificationAggregationPolicyTest {

    @Test
    void selectsFirstNotificationAndAggregatesFieldViolationsInOrder() {
        Notification primary = Notification.error("E_REQUEST_0001", "Request validation failed");
        Notification customerId =
                fieldNotification("E_REQUEST_0002", "customerId", "customerId is required");
        Notification email = fieldNotification("E_REQUEST_0003", "email", "email is invalid");
        List<Notification> notifications = new ArrayList<>(List.of(primary, customerId, email));

        ResolvedError resolvedError =
                new DefaultNotificationAggregationPolicy()
                        .aggregate(
                                notifications,
                                ErrorCategory.VALIDATION,
                                ErrorExposure.PUBLIC,
                                "Bean validation rejected the request");
        notifications.clear();

        assertSame(primary, resolvedError.notification());
        assertEquals(
                List.of(
                        new FieldViolation(
                                "customerId", "E_REQUEST_0002", "customerId is required"),
                        new FieldViolation("email", "E_REQUEST_0003", "email is invalid")),
                resolvedError.fieldViolations());
        assertTrue(resolvedError.hasFieldViolations());
    }

    @Test
    void includesThePrimaryNotificationWhenItRepresentsAFieldViolation() {
        Notification primary =
                fieldNotification("E_REQUEST_0002", "customerId", "customerId is required");

        ResolvedError resolvedError =
                new DefaultNotificationAggregationPolicy()
                        .aggregate(
                                List.of(primary),
                                ErrorCategory.VALIDATION,
                                ErrorExposure.PUBLIC,
                                "Validation failed");

        assertEquals(
                List.of(
                        new FieldViolation(
                                "customerId", "E_REQUEST_0002", "customerId is required")),
                resolvedError.fieldViolations());
    }

    @Test
    void ignoresRelatedNotificationsWithoutAFieldName() {
        ResolvedError resolvedError =
                new DefaultNotificationAggregationPolicy()
                        .aggregate(
                                List.of(
                                        Notification.error("E_PRIMARY", "Primary"),
                                        Notification.warning("W_RELATED", "Related warning")),
                                ErrorCategory.INTERNAL,
                                ErrorExposure.INTERNAL,
                                "Diagnostic");

        assertFalse(resolvedError.hasFieldViolations());
    }

    @Test
    void preservesDuplicateViolationsAndReturnsAnImmutableSnapshot() {
        Notification primary = Notification.error("E_REQUEST", "Validation failed");
        Notification first = fieldNotification("E_REQUIRED", "customerId", "Required");
        Notification second = fieldNotification("E_REQUIRED", "customerId", "Required again");
        List<Notification> notifications = new ArrayList<>(List.of(primary, first, second));

        ResolvedError resolvedError =
                new DefaultNotificationAggregationPolicy()
                        .aggregate(
                                notifications,
                                ErrorCategory.VALIDATION,
                                ErrorExposure.PUBLIC,
                                "Validation diagnostic");
        notifications.remove(second);

        assertEquals(2, resolvedError.fieldViolations().size());
        assertEquals("Required", resolvedError.fieldViolations().get(0).message());
        assertEquals("Required again", resolvedError.fieldViolations().get(1).message());
        assertThrows(
                UnsupportedOperationException.class,
                () ->
                        resolvedError
                                .fieldViolations()
                                .add(new FieldViolation("email", "E_INVALID", "Invalid")));
    }

    @Test
    void rejectsInvalidAggregationInput() {
        DefaultNotificationAggregationPolicy policy = new DefaultNotificationAggregationPolicy();

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        policy.aggregate(
                                List.of(), ErrorCategory.INTERNAL, ErrorExposure.INTERNAL, ""));
        assertThrows(
                IllegalArgumentException.class,
                () -> policy.aggregate(null, ErrorCategory.INTERNAL, ErrorExposure.INTERNAL, ""));
        assertThrows(
                NullPointerException.class,
                () ->
                        policy.aggregate(
                                List.of(Notification.error("E_INTERNAL", "Failure")),
                                null,
                                ErrorExposure.INTERNAL,
                                ""));
        ResolvedError defaultExposure =
                policy.aggregate(
                        List.of(Notification.error("E_INTERNAL", "Failure")),
                        ErrorCategory.INTERNAL,
                        null,
                        "");
        assertEquals(ErrorExposure.INTERNAL, defaultExposure.exposure());
    }

    @Test
    void serviceExceptionResolverUsesReplaceableAggregationPolicy() {
        AtomicBoolean invoked = new AtomicBoolean();
        Notification customNotification = Notification.warning("W_CUSTOM", "Custom aggregation");
        NotificationAggregationPolicy customPolicy =
                (notifications, category, exposure, diagnosticMessage) -> {
                    invoked.set(true);
                    return new ResolvedError(
                            customNotification,
                            ErrorCategory.CONFLICT,
                            ErrorExposure.PUBLIC,
                            diagnosticMessage);
                };
        ServiceExceptionThrowableErrorResolver resolver =
                new ServiceExceptionThrowableErrorResolver(customPolicy);
        ServiceException exception =
                new ServiceException(
                        Notification.error("E_ORIGINAL", "Original"), "Internal diagnostic");

        ResolvedError resolvedError = resolver.resolve(exception);

        assertTrue(invoked.get());
        assertSame(customPolicy, resolver.aggregationPolicy());
        assertSame(customNotification, resolvedError.notification());
        assertEquals("Internal diagnostic", resolvedError.diagnosticMessage());
    }

    @Test
    void integratesServiceExceptionAggregationWithResolutionPipeline() {
        Notification primary = Notification.error("E_REQUEST_0001", "Request validation failed");
        Notification field = fieldNotification("E_REQUEST_0002", "customerId", "Required");
        ServiceException exception =
                new ServiceException(List.of(primary, field), "Validation failed");
        ThrowableErrorResolutionPipeline pipeline =
                new ThrowableErrorResolutionPipeline(
                        List.of(new ServiceExceptionThrowableErrorResolver()));

        ResolvedError resolvedError = pipeline.resolve(exception);

        assertSame(primary, resolvedError.notification());
        assertEquals(ErrorExposure.PUBLIC, resolvedError.exposure());
        assertEquals(1, resolvedError.fieldViolations().size());
    }

    @Test
    void serviceExceptionResolverRejectsUnsupportedFailures() {
        ServiceExceptionThrowableErrorResolver resolver =
                new ServiceExceptionThrowableErrorResolver();

        assertFalse(resolver.supports(new RuntimeException()));
        assertThrows(
                IllegalArgumentException.class, () -> resolver.resolve(new RuntimeException()));
        assertThrows(
                NullPointerException.class, () -> new ServiceExceptionThrowableErrorResolver(null));
    }

    private static Notification fieldNotification(String code, String fieldName, String message) {
        return Notification.builder().code(code).message(message).fieldName(fieldName).build();
    }
}
