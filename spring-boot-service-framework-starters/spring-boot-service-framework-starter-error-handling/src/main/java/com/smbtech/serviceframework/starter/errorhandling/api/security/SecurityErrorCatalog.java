package com.smbtech.serviceframework.starter.errorhandling.api.security;

import com.smbtech.serviceframework.commons.notification.NotificationSeverity;
import com.smbtech.serviceframework.error.ErrorCategory;
import com.smbtech.serviceframework.error.ErrorDefinition;
import java.util.Objects;

/**
 * Stable public errors produced by the Spring Security error handling adapters. Detailed token or
 * provider failure reasons remain internal diagnostics.
 */
public enum SecurityErrorCatalog implements ErrorDefinition {

    /** Represents authentication required. */
    AUTHENTICATION_REQUIRED(
            "E_SERVICE_FRAMEWORK_SECURITY_AUTHENTICATION_0001",
            ErrorCategory.AUTHENTICATION,
            "Authentication is required"),
    /** Represents bearer request invalid. */
    BEARER_REQUEST_INVALID(
            "E_SERVICE_FRAMEWORK_SECURITY_AUTHENTICATION_0002",
            ErrorCategory.AUTHENTICATION,
            "Bearer token request is invalid"),
    /** Represents bearer token invalid. */
    BEARER_TOKEN_INVALID(
            "E_SERVICE_FRAMEWORK_SECURITY_AUTHENTICATION_0003",
            ErrorCategory.AUTHENTICATION,
            "Bearer token is invalid"),
    /** Represents authentication provider failure. */
    AUTHENTICATION_PROVIDER_FAILURE(
            "E_SERVICE_FRAMEWORK_SECURITY_AUTHENTICATION_0004",
            ErrorCategory.DOWNSTREAM,
            "Authentication provider is unavailable"),
    /** Represents access denied. */
    ACCESS_DENIED(
            "E_SERVICE_FRAMEWORK_SECURITY_AUTHORIZATION_0001",
            ErrorCategory.AUTHORIZATION,
            "Access is denied"),
    /** Represents insufficient scope. */
    INSUFFICIENT_SCOPE(
            "E_SERVICE_FRAMEWORK_SECURITY_AUTHORIZATION_0002",
            ErrorCategory.AUTHORIZATION,
            "The access token does not grant the required scope"),
    /** Represents csrf access denied. */
    CSRF_ACCESS_DENIED(
            "E_SERVICE_FRAMEWORK_SECURITY_AUTHORIZATION_0003",
            ErrorCategory.AUTHORIZATION,
            "The request was rejected by CSRF protection");

    private final String code;
    private final ErrorCategory category;
    private final String publicMessage;

    SecurityErrorCatalog(String code, ErrorCategory category, String publicMessage) {
        this.code = requireText(code, "code");
        this.category = Objects.requireNonNull(category, "category must not be null");
        this.publicMessage = requireText(publicMessage, "publicMessage");
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
        return NotificationSeverity.ERROR;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Security error " + field + " must not be blank");
        }
        return value;
    }
}
