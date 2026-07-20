package com.smbtech.serviceframework.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.smbtech.serviceframework.commons.notification.Notification;
import com.smbtech.serviceframework.commons.notification.NotificationSeverity;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ServiceExceptionTest {

    @Test
    void createsExceptionFromErrorDefinition() {
        ServiceException exception = ServiceException.from(CustomerErrors.CUSTOMER_NOT_FOUND);

        Notification notification = exception.primaryNotification().orElseThrow();
        assertEquals("E_CUSTOMER_0001", notification.code());
        assertEquals("The requested customer does not exist", notification.message());
        assertEquals(NotificationSeverity.ERROR, notification.severity());
        assertEquals(ErrorCategory.NOT_FOUND, exception.category());
        assertEquals(notification.message(), exception.diagnosticMessage());
    }

    @Test
    void keepsDiagnosticMessageAndCauseSeparateFromPublicNotification() {
        IllegalStateException cause = new IllegalStateException("database unavailable");

        ServiceException exception =
                ServiceException.from(
                        CustomerErrors.CUSTOMER_NOT_FOUND,
                        "Customer lookup failed for internal identifier 123",
                        cause);

        assertEquals(
                "The requested customer does not exist",
                exception.primaryNotification().orElseThrow().message());
        assertEquals(
                "Customer lookup failed for internal identifier 123",
                exception.diagnosticMessage());
        assertEquals(exception.diagnosticMessage(), exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    @Test
    void supportsMultipleDefinitionsUsingThePrimaryCategory() {
        ServiceException exception =
                ServiceException.from(
                        List.of(
                                CustomerErrors.INVALID_CUSTOMER_ID,
                                CustomerErrors.CUSTOMER_NOT_FOUND),
                        "Customer request validation failed");

        assertEquals(2, exception.notifications().size());
        assertEquals("E_CUSTOMER_0002", exception.notifications().get(0).code());
        assertEquals("E_CUSTOMER_0001", exception.notifications().get(1).code());
        assertEquals(ErrorCategory.VALIDATION, exception.category());
        assertEquals("Customer request validation failed", exception.diagnosticMessage());
    }

    @Test
    void supportsDirectNotificationsAndCopiesTheirList() {
        Notification first = Notification.error("E_CUSTOMER_0001", "Customer not found");
        Notification second =
                Notification.warning("W_CUSTOMER_0002", "Customer data is incomplete");
        List<Notification> notifications = new ArrayList<>(List.of(first, second));

        ServiceException exception =
                new ServiceException(notifications, "Customer operation failed");
        notifications.clear();

        assertEquals(List.of(first, second), exception.notifications());
        assertEquals(ErrorCategory.INTERNAL, exception.category());
        assertThrows(
                UnsupportedOperationException.class, () -> exception.notifications().add(first));
    }

    @Test
    void supportsEverySingleDefinitionFactoryVariant() {
        IllegalStateException cause = new IllegalStateException("lookup failed");

        ServiceException withCause =
                ServiceException.from(CustomerErrors.CUSTOMER_NOT_FOUND, cause);
        ServiceException withDiagnostic =
                ServiceException.from(
                        CustomerErrors.CUSTOMER_NOT_FOUND, "Customer lookup diagnostic");

        assertSame(cause, withCause.getCause());
        assertEquals("The requested customer does not exist", withCause.diagnosticMessage());
        assertEquals("Customer lookup diagnostic", withDiagnostic.diagnosticMessage());
        assertEquals(ErrorCategory.NOT_FOUND, withCause.category());
    }

    @Test
    void supportsEveryMultipleDefinitionFactoryVariantAndCopiesDefinitions() {
        IllegalStateException cause = new IllegalStateException("validation dependency failed");
        List<ErrorDefinition> definitions =
                new ArrayList<>(
                        List.of(
                                CustomerErrors.INVALID_CUSTOMER_ID,
                                CustomerErrors.CUSTOMER_NOT_FOUND));

        ServiceException withCause = ServiceException.from(definitions, cause);
        ServiceException withDiagnosticAndCause =
                ServiceException.from(definitions, "Customer validation diagnostic", cause);
        definitions.clear();

        assertEquals(2, withCause.notifications().size());
        assertSame(cause, withCause.getCause());
        assertEquals("The customer identifier is invalid", withCause.diagnosticMessage());
        assertEquals("Customer validation diagnostic", withDiagnosticAndCause.diagnosticMessage());
        assertSame(cause, withDiagnosticAndCause.getCause());
        assertEquals(ErrorCategory.VALIDATION, withDiagnosticAndCause.category());
    }

    @Test
    void supportsDirectNotificationConstructorVariants() {
        Notification notification =
                Notification.error("E_CUSTOMER_0003", "Customer operation failed");
        IllegalArgumentException cause = new IllegalArgumentException("invalid state");

        ServiceException withCause = new ServiceException(notification, cause);
        ServiceException withDiagnostic = new ServiceException(notification, "Internal diagnostic");
        ServiceException withDiagnosticAndCause =
                new ServiceException(
                        List.of(notification), "Internal diagnostic with cause", cause);

        assertSame(cause, withCause.getCause());
        assertEquals(notification.message(), withCause.diagnosticMessage());
        assertEquals("Internal diagnostic", withDiagnostic.diagnosticMessage());
        assertEquals("Internal diagnostic with cause", withDiagnosticAndCause.diagnosticMessage());
        assertSame(cause, withDiagnosticAndCause.getCause());
    }

    @Test
    void rejectsMissingNotificationsAndDefinitions() {
        assertThrows(NullPointerException.class, () -> new ServiceException((Notification) null));
        assertThrows(IllegalArgumentException.class, () -> new ServiceException(List.of()));
        assertThrows(
                NullPointerException.class, () -> ServiceException.from((ErrorDefinition) null));
        assertThrows(IllegalArgumentException.class, () -> ServiceException.from(List.of()));
        assertThrows(
                IllegalArgumentException.class,
                () -> ServiceException.from((List<ErrorDefinition>) null));
        assertThrows(
                NullPointerException.class,
                () ->
                        ServiceException.from(
                                java.util.Arrays.asList(CustomerErrors.CUSTOMER_NOT_FOUND, null)));
    }

    private enum CustomerErrors implements ErrorDefinition {
        CUSTOMER_NOT_FOUND(
                "E_CUSTOMER_0001",
                ErrorCategory.NOT_FOUND,
                "The requested customer does not exist",
                NotificationSeverity.ERROR),
        INVALID_CUSTOMER_ID(
                "E_CUSTOMER_0002",
                ErrorCategory.VALIDATION,
                "The customer identifier is invalid",
                NotificationSeverity.ERROR);

        private final String code;
        private final ErrorCategory category;
        private final String publicMessage;
        private final NotificationSeverity severity;

        CustomerErrors(
                String code,
                ErrorCategory category,
                String publicMessage,
                NotificationSeverity severity) {
            this.code = code;
            this.category = category;
            this.publicMessage = publicMessage;
            this.severity = severity;
        }

        @Override
        public String code() {
            return code;
        }

        @Override
        public ErrorCategory category() {
            return category;
        }

        @Override
        public String publicMessage() {
            return publicMessage;
        }

        @Override
        public NotificationSeverity severity() {
            return severity;
        }
    }
}
