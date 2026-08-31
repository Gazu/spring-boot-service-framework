package com.smbtech.serviceframework.starter.errorhandling.api.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.smbtech.serviceframework.commons.notification.NotificationSeverity;
import com.smbtech.serviceframework.error.ErrorCategory;
import com.smbtech.serviceframework.error.ErrorDefinition;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class SecurityErrorCatalogTest {

    @Test
    void definesTheStableSecurityCatalog() {
        assertEntry(
                SecurityErrorCatalog.AUTHENTICATION_REQUIRED,
                "E_SERVICE_FRAMEWORK_SECURITY_AUTHENTICATION_0001",
                ErrorCategory.AUTHENTICATION,
                "Authentication is required");
        assertEntry(
                SecurityErrorCatalog.BEARER_REQUEST_INVALID,
                "E_SERVICE_FRAMEWORK_SECURITY_AUTHENTICATION_0002",
                ErrorCategory.AUTHENTICATION,
                "Bearer token request is invalid");
        assertEntry(
                SecurityErrorCatalog.BEARER_TOKEN_INVALID,
                "E_SERVICE_FRAMEWORK_SECURITY_AUTHENTICATION_0003",
                ErrorCategory.AUTHENTICATION,
                "Bearer token is invalid");
        assertEntry(
                SecurityErrorCatalog.AUTHENTICATION_PROVIDER_FAILURE,
                "E_SERVICE_FRAMEWORK_SECURITY_AUTHENTICATION_0004",
                ErrorCategory.DOWNSTREAM,
                "Authentication provider is unavailable");
        assertEntry(
                SecurityErrorCatalog.ACCESS_DENIED,
                "E_SERVICE_FRAMEWORK_SECURITY_AUTHORIZATION_0001",
                ErrorCategory.AUTHORIZATION,
                "Access is denied");
        assertEntry(
                SecurityErrorCatalog.INSUFFICIENT_SCOPE,
                "E_SERVICE_FRAMEWORK_SECURITY_AUTHORIZATION_0002",
                ErrorCategory.AUTHORIZATION,
                "The access token does not grant the required scope");
        assertEntry(
                SecurityErrorCatalog.CSRF_ACCESS_DENIED,
                "E_SERVICE_FRAMEWORK_SECURITY_AUTHORIZATION_0003",
                ErrorCategory.AUTHORIZATION,
                "The request was rejected by CSRF protection");
    }

    @Test
    void keepsCatalogNamesAndCodesUnique() {
        assertThat(Arrays.stream(SecurityErrorCatalog.values()).map(Enum::name))
                .containsExactly(
                        "AUTHENTICATION_REQUIRED",
                        "BEARER_REQUEST_INVALID",
                        "BEARER_TOKEN_INVALID",
                        "AUTHENTICATION_PROVIDER_FAILURE",
                        "ACCESS_DENIED",
                        "INSUFFICIENT_SCOPE",
                        "CSRF_ACCESS_DENIED");
        assertThat(Arrays.stream(SecurityErrorCatalog.values()).map(SecurityErrorCatalog::code))
                .doesNotHaveDuplicates();
        assertThat(Arrays.stream(SecurityErrorCatalog.values()).map(SecurityErrorCatalog::severity))
                .containsOnly(NotificationSeverity.ERROR);
    }

    private static void assertEntry(
            ErrorDefinition definition, String code, ErrorCategory category, String publicMessage) {
        assertThat(definition.code()).isEqualTo(code);
        assertThat(definition.category()).isEqualTo(category);
        assertThat(definition.publicMessage()).isEqualTo(publicMessage);
        assertThat(definition.severity()).isEqualTo(NotificationSeverity.ERROR);
    }
}
