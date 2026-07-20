package com.smbtech.serviceframework.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.smbtech.serviceframework.commons.notification.NotificationSeverity;
import org.junit.jupiter.api.Test;

class ErrorDefinitionTest {

    @Test
    void supportsApplicationDefinedErrorCatalogs() {
        ErrorDefinition definition = CustomerErrors.CUSTOMER_NOT_FOUND;

        assertEquals("E_CUSTOMER_0001", definition.code());
        assertEquals(ErrorCategory.NOT_FOUND, definition.category());
        assertEquals("The requested customer does not exist", definition.publicMessage());
        assertEquals(NotificationSeverity.ERROR, definition.severity());
    }

    @Test
    void keepsCatalogEntriesStableAndIndependent() {
        ErrorDefinition notFound = CustomerErrors.CUSTOMER_NOT_FOUND;
        ErrorDefinition deprecatedProfile = CustomerErrors.CUSTOMER_PROFILE_DEPRECATED;

        assertNotEquals(notFound.code(), deprecatedProfile.code());
        assertEquals(ErrorCategory.VALIDATION, deprecatedProfile.category());
        assertEquals(
                "The customer profile format is deprecated", deprecatedProfile.publicMessage());
        assertEquals(NotificationSeverity.WARNING, deprecatedProfile.severity());
        assertEquals(2, CustomerErrors.values().length);
    }

    private enum CustomerErrors implements ErrorDefinition {
        CUSTOMER_NOT_FOUND(
                "E_CUSTOMER_0001",
                ErrorCategory.NOT_FOUND,
                "The requested customer does not exist",
                NotificationSeverity.ERROR),
        CUSTOMER_PROFILE_DEPRECATED(
                "W_CUSTOMER_0002",
                ErrorCategory.VALIDATION,
                "The customer profile format is deprecated",
                NotificationSeverity.WARNING);

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
