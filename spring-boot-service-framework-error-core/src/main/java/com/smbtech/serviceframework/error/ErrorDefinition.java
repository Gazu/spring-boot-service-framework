package com.smbtech.serviceframework.error;

import com.smbtech.serviceframework.commons.notification.NotificationSeverity;

/** Defines a stable application error that can be declared in a domain catalog. */
public interface ErrorDefinition {

    /**
     * Returns the stable machine-readable error code.
     *
     * @return error code
     */
    String code();

    /**
     * Returns the category used by error handling policies.
     *
     * @return error category
     */
    ErrorCategory category();

    /**
     * Returns the message that may be exposed to consumers.
     *
     * @return public error message
     */
    String publicMessage();

    /**
     * Returns the notification severity.
     *
     * @return notification severity
     */
    NotificationSeverity severity();
}
