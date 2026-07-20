package com.smbtech.examples.errorhandling.domain;

import com.smbtech.serviceframework.commons.notification.NotificationSeverity;
import com.smbtech.serviceframework.error.ErrorCategory;
import com.smbtech.serviceframework.error.ErrorDefinition;

public enum OrderErrors implements ErrorDefinition {
    ORDER_NOT_FOUND(
            "E_ORDER_0001",
            ErrorCategory.NOT_FOUND,
            "The requested order does not exist",
            NotificationSeverity.ERROR),
    ORDER_ALREADY_COMPLETED(
            "E_ORDER_0002",
            ErrorCategory.CONFLICT,
            "The order is already completed",
            NotificationSeverity.ERROR);

    private final String code;
    private final ErrorCategory category;
    private final String publicMessage;
    private final NotificationSeverity severity;

    OrderErrors(
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
