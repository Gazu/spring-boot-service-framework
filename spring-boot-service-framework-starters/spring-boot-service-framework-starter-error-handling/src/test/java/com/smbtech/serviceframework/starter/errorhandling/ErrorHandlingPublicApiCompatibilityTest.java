package com.smbtech.serviceframework.starter.errorhandling;

import static org.assertj.core.api.Assertions.assertThat;

import com.smbtech.serviceframework.commons.notification.Notification;
import com.smbtech.serviceframework.commons.notification.NotificationSeverity;
import com.smbtech.serviceframework.error.ErrorCategory;
import com.smbtech.serviceframework.error.ErrorDefinition;
import com.smbtech.serviceframework.error.ErrorExposure;
import com.smbtech.serviceframework.error.FieldViolation;
import com.smbtech.serviceframework.error.NotificationAggregationPolicy;
import com.smbtech.serviceframework.error.NotificationSanitizer;
import com.smbtech.serviceframework.error.ResolvedError;
import com.smbtech.serviceframework.error.ServiceException;
import com.smbtech.serviceframework.error.ThrowableErrorResolver;
import com.smbtech.serviceframework.starter.errorhandling.api.ErrorExposurePolicy;
import com.smbtech.serviceframework.starter.errorhandling.api.ErrorMetricsRecorder;
import com.smbtech.serviceframework.starter.errorhandling.api.ErrorReporter;
import com.smbtech.serviceframework.starter.errorhandling.api.NotificationHttpStatusResolver;
import com.smbtech.serviceframework.starter.errorhandling.api.NotificationResponseCustomizer;
import com.smbtech.serviceframework.starter.errorhandling.api.NotificationResponseFactory;
import com.smbtech.serviceframework.starter.errorhandling.api.NotificationResponseWriter;
import com.smbtech.serviceframework.starter.errorhandling.api.NotificationSerializer;
import com.smbtech.serviceframework.starter.errorhandling.api.ResolvedErrorCustomizer;
import com.smbtech.serviceframework.starter.errorhandling.api.security.OAuth2SecurityChallengeWriter;
import com.smbtech.serviceframework.starter.errorhandling.api.security.OAuth2SecurityError;
import com.smbtech.serviceframework.starter.errorhandling.api.security.OAuth2SecurityMetadataFactory;
import com.smbtech.serviceframework.starter.errorhandling.api.security.RequiredScopeResolver;
import com.smbtech.serviceframework.starter.errorhandling.api.security.SecurityAuthenticationFailureResolver;
import com.smbtech.serviceframework.starter.errorhandling.api.security.SecurityAuthorizationFailureResolver;
import com.smbtech.serviceframework.starter.errorhandling.api.security.SecurityErrorCatalog;
import com.smbtech.serviceframework.starter.errorhandling.api.security.SecurityFailureContext;
import com.smbtech.serviceframework.starter.errorhandling.api.security.SecurityFailureReason;
import com.smbtech.serviceframework.starter.errorhandling.api.security.SecurityFailureResolution;
import com.smbtech.serviceframework.starter.errorhandling.autoconfigure.ErrorHandlingProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;

class ErrorHandlingPublicApiCompatibilityTest {

    @Test
    void keepsDocumentedCoreAndExtensionTypesPublic() {
        List<Class<?>> publicTypes =
                List.of(
                        ErrorCategory.class,
                        ErrorDefinition.class,
                        ErrorExposure.class,
                        FieldViolation.class,
                        NotificationAggregationPolicy.class,
                        NotificationSanitizer.class,
                        ResolvedError.class,
                        ServiceException.class,
                        ThrowableErrorResolver.class,
                        OAuth2SecurityError.class,
                        OAuth2SecurityChallengeWriter.class,
                        OAuth2SecurityMetadataFactory.class,
                        RequiredScopeResolver.class,
                        SecurityErrorCatalog.class,
                        SecurityAuthenticationFailureResolver.class,
                        SecurityAuthorizationFailureResolver.class,
                        SecurityFailureContext.class,
                        SecurityFailureReason.class,
                        SecurityFailureResolution.class,
                        ErrorHandlingProperties.class,
                        ErrorHandlingProperties.Response.class,
                        ErrorHandlingProperties.Security.class,
                        ErrorHandlingProperties.OAuth2Metadata.class,
                        ErrorExposurePolicy.class,
                        ErrorMetricsRecorder.class,
                        ErrorReporter.class,
                        NotificationHttpStatusResolver.class,
                        NotificationResponseCustomizer.class,
                        NotificationResponseFactory.class,
                        NotificationResponseWriter.class,
                        NotificationSerializer.class,
                        ResolvedErrorCustomizer.class);

        assertThat(publicTypes)
                .allSatisfy(
                        type ->
                                assertThat(Modifier.isPublic(type.getModifiers()))
                                        .as("%s must remain public", type.getName())
                                        .isTrue());
    }

    @Test
    void keepsDefaultAdaptersOutsideThePublicApi() {
        List<String> internalTypes =
                List.of(
                        "com.smbtech.serviceframework.starter.errorhandling.api.CompositeErrorReporter",
                        "com.smbtech.serviceframework.starter.errorhandling.internal.DefaultOAuth2SecurityChallengeWriter",
                        "com.smbtech.serviceframework.starter.errorhandling.internal.DefaultOAuth2SecurityMetadataFactory",
                        "com.smbtech.serviceframework.starter.errorhandling.internal.DefaultRequiredScopeResolver",
                        "com.smbtech.serviceframework.starter.errorhandling.internal.DefaultSecurityAuthenticationFailureResolver",
                        "com.smbtech.serviceframework.starter.errorhandling.internal.DefaultSecurityAuthorizationFailureResolver",
                        "com.smbtech.serviceframework.starter.errorhandling.internal.SecurityAccessDeniedHandler",
                        "com.smbtech.serviceframework.starter.errorhandling.internal.SecurityAuthenticationEntryPoint",
                        "com.smbtech.serviceframework.starter.errorhandling.internal.DefaultNotificationHttpStatusResolver",
                        "com.smbtech.serviceframework.starter.errorhandling.internal.HttpClientExceptionResolver",
                        "com.smbtech.serviceframework.starter.errorhandling.internal.NotificationHttpMessageConverter",
                        "com.smbtech.serviceframework.starter.errorhandling.internal.NotificationWebMvcConfigurer",
                        "com.smbtech.serviceframework.starter.errorhandling.internal.ServiceFrameworkExceptionHandler",
                        "com.smbtech.serviceframework.starter.errorhandling.internal.SpringMvcExceptionResolver",
                        "com.smbtech.serviceframework.starter.errorhandling.internal.ValidationExceptionResolver",
                        "com.smbtech.serviceframework.starter.errorhandling.adapter.out.logging.StructuredErrorReporter",
                        "com.smbtech.serviceframework.starter.errorhandling.adapter.out.metrics.MicrometerErrorMetricsRecorder",
                        "com.smbtech.serviceframework.starter.errorhandling.serialization.NotificationMetadataKeyNormalizer");

        assertThat(internalTypes)
                .allSatisfy(
                        typeName ->
                                assertThat(Modifier.isPublic(loadClass(typeName).getModifiers()))
                                        .as("%s must remain internal", typeName)
                                        .isFalse());
    }

    @Test
    void keepsCoreModelAndPolicySignaturesCompatible() {
        assertThat(Arrays.stream(ErrorCategory.values()).map(Enum::name))
                .containsExactly(
                        "VALIDATION",
                        "AUTHENTICATION",
                        "AUTHORIZATION",
                        "NOT_FOUND",
                        "CONFLICT",
                        "DOWNSTREAM",
                        "RATE_LIMIT",
                        "METHOD_NOT_ALLOWED",
                        "UNSUPPORTED_MEDIA_TYPE",
                        "NOT_ACCEPTABLE",
                        "INTERNAL");
        assertThat(Arrays.stream(ErrorExposure.values()).map(Enum::name))
                .containsExactly("PUBLIC", "INTERNAL");
        assertThat(
                        Arrays.stream(FieldViolation.class.getRecordComponents())
                                .map(component -> component.getName()))
                .containsExactly("fieldName", "code", "message");
        assertThat(
                        Arrays.stream(ResolvedError.class.getRecordComponents())
                                .map(component -> component.getName()))
                .containsExactly(
                        "notification",
                        "category",
                        "exposure",
                        "diagnosticMessage",
                        "fieldViolations");

        assertPublicMethod(ErrorDefinition.class, "code", String.class);
        assertPublicMethod(ErrorDefinition.class, "category", ErrorCategory.class);
        assertPublicMethod(ErrorDefinition.class, "publicMessage", String.class);
        assertPublicMethod(
                ErrorDefinition.class,
                "severity",
                com.smbtech.serviceframework.commons.notification.NotificationSeverity.class);
        assertPublicMethod(
                ThrowableErrorResolver.class, "supports", boolean.class, Throwable.class);
        assertPublicMethod(
                ThrowableErrorResolver.class, "resolve", ResolvedError.class, Throwable.class);
        assertPublicMethod(ThrowableErrorResolver.class, "order", int.class);
        assertPublicMethod(
                NotificationAggregationPolicy.class,
                "aggregate",
                ResolvedError.class,
                List.class,
                ErrorCategory.class,
                ErrorExposure.class,
                String.class);
        assertPublicMethod(
                NotificationSanitizer.class, "sanitize", Notification.class, Notification.class);
        assertPublicMethod(
                NotificationSanitizer.class, "sanitize", ResolvedError.class, ResolvedError.class);
    }

    @Test
    void keepsGlobalResponseExposureConfigurationCompatible() {
        ConfigurationProperties configurationProperties =
                ErrorHandlingProperties.class.getAnnotation(ConfigurationProperties.class);

        assertThat(configurationProperties).isNotNull();
        assertThat(configurationProperties.value()).isEqualTo("smbtech.error-handling");
        assertPublicMethod(
                ErrorHandlingProperties.class,
                "getResponse",
                ErrorHandlingProperties.Response.class);
        assertPublicConstructor(ErrorHandlingProperties.Response.class);
        assertPublicMethod(
                ErrorHandlingProperties.Response.class, "getExposure", ErrorExposure.class);
        assertPublicMethod(
                ErrorHandlingProperties.Response.class,
                "setExposure",
                void.class,
                ErrorExposure.class);

        ErrorHandlingProperties properties = new ErrorHandlingProperties();
        assertThat(properties.getResponse().getExposure()).isEqualTo(ErrorExposure.PUBLIC);
        assertThat(Arrays.stream(ErrorExposure.values()).map(Enum::name))
                .containsExactly("PUBLIC", "INTERNAL");

        for (ErrorExposure exposure : ErrorExposure.values()) {
            properties.getResponse().setExposure(exposure);
            assertThat(properties.getResponse().getExposure()).isEqualTo(exposure);
        }

        assertThat(ErrorExposurePolicy.class.isAnnotationPresent(FunctionalInterface.class))
                .isTrue();
        assertPublicMethod(
                ErrorExposurePolicy.class, "resolve", ErrorExposure.class, ResolvedError.class);
    }

    @Test
    void keepsServiceExceptionConstructorsAndFactoriesCompatible() {
        assertPublicConstructor(ServiceException.class, Notification.class);
        assertPublicConstructor(ServiceException.class, Notification.class, Throwable.class);
        assertPublicConstructor(ServiceException.class, Notification.class, String.class);
        assertPublicConstructor(
                ServiceException.class, Notification.class, String.class, Throwable.class);
        assertPublicConstructor(ServiceException.class, List.class);
        assertPublicConstructor(ServiceException.class, List.class, Throwable.class);
        assertPublicConstructor(ServiceException.class, List.class, String.class);
        assertPublicConstructor(ServiceException.class, List.class, String.class, Throwable.class);

        assertPublicStaticMethod(ServiceException.class, "from", ErrorDefinition.class);
        assertPublicStaticMethod(
                ServiceException.class, "from", ErrorDefinition.class, Throwable.class);
        assertPublicStaticMethod(
                ServiceException.class, "from", ErrorDefinition.class, String.class);
        assertPublicStaticMethod(
                ServiceException.class,
                "from",
                ErrorDefinition.class,
                String.class,
                Throwable.class);
        assertPublicStaticMethod(ServiceException.class, "from", List.class);
        assertPublicStaticMethod(ServiceException.class, "from", List.class, Throwable.class);
        assertPublicStaticMethod(ServiceException.class, "from", List.class, String.class);
        assertPublicStaticMethod(
                ServiceException.class, "from", List.class, String.class, Throwable.class);
        assertPublicMethod(ServiceException.class, "category", ErrorCategory.class);
        assertPublicMethod(ServiceException.class, "diagnosticMessage", String.class);
    }

    @Test
    void keepsStarterExtensionMethodSignaturesCompatible() {
        assertPublicMethod(
                ErrorExposurePolicy.class, "resolve", ErrorExposure.class, ResolvedError.class);
        assertPublicMethod(
                ErrorReporter.class,
                "report",
                void.class,
                Throwable.class,
                ResolvedError.class,
                HttpServletRequest.class);
        assertPublicMethod(
                ErrorReporter.class,
                "report",
                void.class,
                Throwable.class,
                ResolvedError.class,
                HttpServletRequest.class,
                int.class);
        assertPublicMethod(ErrorReporter.class, "order", int.class);
        assertPublicMethod(ErrorReporter.class, "composite", ErrorReporter.class, List.class);
        assertPublicMethod(
                ErrorMetricsRecorder.class, "record", void.class, ResolvedError.class, int.class);
        assertPublicMethod(
                NotificationHttpStatusResolver.class,
                "resolve",
                HttpStatusCode.class,
                ResolvedError.class);
        assertPublicMethod(
                NotificationResponseFactory.class,
                "create",
                ResponseEntity.class,
                ResolvedError.class);
        assertPublicMethod(
                ResolvedErrorCustomizer.class,
                "customize",
                ResolvedError.class,
                Throwable.class,
                ResolvedError.class,
                HttpServletRequest.class);
        assertPublicMethod(ResolvedErrorCustomizer.class, "order", int.class);
        assertPublicMethod(
                NotificationResponseCustomizer.class,
                "customize",
                ResponseEntity.class,
                ResponseEntity.class,
                ResolvedError.class,
                HttpServletRequest.class);
        assertPublicMethod(NotificationResponseCustomizer.class, "order", int.class);
        assertPublicMethod(
                NotificationResponseWriter.class,
                "write",
                void.class,
                ResponseEntity.class,
                HttpServletResponse.class);
        assertPublicMethod(
                NotificationSerializer.class,
                "serialize",
                void.class,
                Notification.class,
                JsonGenerator.class,
                SerializationContext.class);
        assertPublicMethod(
                RequiredScopeResolver.class,
                "resolve",
                java.util.Set.class,
                HttpServletRequest.class,
                org.springframework.security.core.Authentication.class);
        assertPublicMethod(
                OAuth2SecurityMetadataFactory.class,
                "create",
                com.smbtech.serviceframework.error.metadata.StandardErrorMetadata.class,
                SecurityFailureContext.class,
                SecurityFailureResolution.class);
        assertPublicMethod(
                OAuth2SecurityChallengeWriter.class,
                "write",
                void.class,
                HttpServletRequest.class,
                HttpServletResponse.class,
                SecurityFailureContext.class,
                SecurityFailureResolution.class);
        assertPublicMethod(
                SecurityAuthenticationFailureResolver.class,
                "resolve",
                SecurityFailureResolution.class,
                SecurityFailureContext.class);
        assertPublicMethod(
                SecurityAuthorizationFailureResolver.class,
                "resolve",
                SecurityFailureResolution.class,
                SecurityFailureContext.class);
    }

    @Test
    void keepsSecurityCatalogAndResolutionContractCompatible() {
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
                .containsExactly(
                        "E_SERVICE_FRAMEWORK_SECURITY_AUTHENTICATION_0001",
                        "E_SERVICE_FRAMEWORK_SECURITY_AUTHENTICATION_0002",
                        "E_SERVICE_FRAMEWORK_SECURITY_AUTHENTICATION_0003",
                        "E_SERVICE_FRAMEWORK_SECURITY_AUTHENTICATION_0004",
                        "E_SERVICE_FRAMEWORK_SECURITY_AUTHORIZATION_0001",
                        "E_SERVICE_FRAMEWORK_SECURITY_AUTHORIZATION_0002",
                        "E_SERVICE_FRAMEWORK_SECURITY_AUTHORIZATION_0003");
        assertThat(Arrays.stream(SecurityErrorCatalog.values()).map(SecurityErrorCatalog::category))
                .containsExactly(
                        ErrorCategory.AUTHENTICATION,
                        ErrorCategory.AUTHENTICATION,
                        ErrorCategory.AUTHENTICATION,
                        ErrorCategory.DOWNSTREAM,
                        ErrorCategory.AUTHORIZATION,
                        ErrorCategory.AUTHORIZATION,
                        ErrorCategory.AUTHORIZATION);
        assertThat(
                        Arrays.stream(SecurityErrorCatalog.values())
                                .map(SecurityErrorCatalog::publicMessage))
                .containsExactly(
                        "Authentication is required",
                        "Bearer token request is invalid",
                        "Bearer token is invalid",
                        "Authentication provider is unavailable",
                        "Access is denied",
                        "The access token does not grant the required scope",
                        "The request was rejected by CSRF protection");
        assertThat(Arrays.stream(SecurityErrorCatalog.values()).map(SecurityErrorCatalog::severity))
                .containsOnly(NotificationSeverity.ERROR);
        assertThat(Arrays.stream(SecurityFailureReason.values()).map(Enum::name))
                .containsExactly(
                        "AUTHENTICATION_REQUIRED",
                        "BEARER_REQUEST_INVALID",
                        "BEARER_TOKEN_INVALID",
                        "AUTHENTICATION_PROVIDER_FAILURE",
                        "ACCESS_DENIED",
                        "INSUFFICIENT_SCOPE",
                        "CSRF_ACCESS_DENIED");
        assertThat(
                        Arrays.stream(SecurityFailureReason.values())
                                .map(SecurityFailureReason::metadataValue))
                .containsExactly(
                        "authentication_required",
                        "invalid_request",
                        "invalid_token",
                        "provider_failure",
                        "access_denied",
                        "insufficient_scope",
                        "csrf_rejected");
        assertThat(
                        Arrays.stream(SecurityFailureReason.values())
                                .map(SecurityFailureReason::errorDefinition))
                .containsExactly(SecurityErrorCatalog.values());

        assertThat(
                        Arrays.stream(OAuth2SecurityError.class.getRecordComponents())
                                .map(component -> component.getName()))
                .containsExactly("error", "scope");
        assertThat(
                        Arrays.stream(SecurityFailureContext.class.getRecordComponents())
                                .map(component -> component.getName()))
                .containsExactly(
                        "failure",
                        "method",
                        "route",
                        "correlationId",
                        "bearerCredentialsPresent",
                        "authenticationType",
                        "requiredScopes");
        assertThat(
                        Arrays.stream(SecurityFailureResolution.class.getRecordComponents())
                                .map(component -> component.getName()))
                .containsExactly("resolvedError", "reason", "oauth2Error", "bearerChallenge");

        assertPublicConstructor(OAuth2SecurityError.class, String.class, String.class);
        assertPublicConstructor(
                SecurityFailureContext.class,
                Throwable.class,
                String.class,
                String.class,
                String.class,
                boolean.class,
                String.class,
                java.util.Set.class);
        assertPublicConstructor(SecurityFailureContext.class, Throwable.class);
        assertPublicConstructor(
                SecurityFailureResolution.class,
                ResolvedError.class,
                SecurityFailureReason.class,
                OAuth2SecurityError.class,
                boolean.class);
        assertPublicConstructor(
                SecurityFailureResolution.class, ResolvedError.class, SecurityFailureReason.class);

        assertPublicStaticMethodReturning(
                OAuth2SecurityError.class, "none", OAuth2SecurityError.class);
        assertPublicStaticMethodReturning(
                OAuth2SecurityError.class, "invalidRequest", OAuth2SecurityError.class);
        assertPublicStaticMethodReturning(
                OAuth2SecurityError.class, "invalidToken", OAuth2SecurityError.class);
        assertPublicStaticMethodReturning(
                OAuth2SecurityError.class, "insufficientScope", OAuth2SecurityError.class);
        assertPublicStaticMethodReturning(
                OAuth2SecurityError.class,
                "insufficientScope",
                OAuth2SecurityError.class,
                java.util.Collection.class);
        assertPublicMethod(OAuth2SecurityError.class, "isPresent", boolean.class);
        assertPublicMethod(SecurityFailureContext.class, "hasRequiredScopes", boolean.class);
        assertPublicMethod(SecurityFailureResolution.class, "hasOAuth2Error", boolean.class);
    }

    @Test
    void keepsSecurityConfigurationAccessorsCompatible() {
        assertPublicMethod(
                ErrorHandlingProperties.class,
                "getSecurity",
                ErrorHandlingProperties.Security.class);
        assertPublicConstructor(ErrorHandlingProperties.Security.class);
        assertPublicMethod(ErrorHandlingProperties.Security.class, "isEnabled", boolean.class);
        assertPublicMethod(
                ErrorHandlingProperties.Security.class, "setEnabled", void.class, boolean.class);
        assertPublicMethod(
                ErrorHandlingProperties.Security.class,
                "getOauth2Metadata",
                ErrorHandlingProperties.OAuth2Metadata.class);

        assertPublicConstructor(ErrorHandlingProperties.OAuth2Metadata.class);
        assertPublicMethod(
                ErrorHandlingProperties.OAuth2Metadata.class, "isEnabled", boolean.class);
        assertPublicMethod(
                ErrorHandlingProperties.OAuth2Metadata.class,
                "setEnabled",
                void.class,
                boolean.class);
        assertPublicMethod(
                ErrorHandlingProperties.OAuth2Metadata.class,
                "isIncludeErrorDescription",
                boolean.class);
        assertPublicMethod(
                ErrorHandlingProperties.OAuth2Metadata.class,
                "setIncludeErrorDescription",
                void.class,
                boolean.class);
        assertPublicMethod(
                ErrorHandlingProperties.OAuth2Metadata.class, "isIncludeErrorUri", boolean.class);
        assertPublicMethod(
                ErrorHandlingProperties.OAuth2Metadata.class,
                "setIncludeErrorUri",
                void.class,
                boolean.class);
        assertPublicMethod(
                ErrorHandlingProperties.OAuth2Metadata.class,
                "isIncludeRequiredScope",
                boolean.class);
        assertPublicMethod(
                ErrorHandlingProperties.OAuth2Metadata.class,
                "setIncludeRequiredScope",
                void.class,
                boolean.class);
    }

    private static void assertPublicConstructor(Class<?> type, Class<?>... parameterTypes) {
        Constructor<?> constructor = findConstructor(type, parameterTypes);
        assertThat(Modifier.isPublic(constructor.getModifiers()))
                .as("%s constructor must remain public", type.getName())
                .isTrue();
    }

    private static Constructor<?> findConstructor(Class<?> type, Class<?>... parameterTypes) {
        try {
            return type.getDeclaredConstructor(parameterTypes);
        } catch (NoSuchMethodException exception) {
            throw new AssertionError("Missing constructor on " + type.getName(), exception);
        }
    }

    private static Class<?> loadClass(String typeName) {
        try {
            return Class.forName(typeName);
        } catch (ClassNotFoundException exception) {
            throw new AssertionError("Missing internal type " + typeName, exception);
        }
    }

    private static void assertPublicStaticMethod(
            Class<?> type, String name, Class<?>... parameterTypes) {
        Method method = findMethod(type, name, parameterTypes);
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
        assertThat(Modifier.isStatic(method.getModifiers())).isTrue();
        assertThat(method.getReturnType()).isEqualTo(ServiceException.class);
    }

    private static void assertPublicStaticMethodReturning(
            Class<?> type, String name, Class<?> returnType, Class<?>... parameterTypes) {
        Method method = findMethod(type, name, parameterTypes);
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
        assertThat(Modifier.isStatic(method.getModifiers())).isTrue();
        assertThat(method.getReturnType()).isEqualTo(returnType);
    }

    private static void assertPublicMethod(
            Class<?> type, String name, Class<?> returnType, Class<?>... parameterTypes) {
        Method method = findMethod(type, name, parameterTypes);
        assertThat(Modifier.isPublic(method.getModifiers()))
                .as("%s.%s must remain public", type.getName(), name)
                .isTrue();
        assertThat(method.getReturnType())
                .as("%s.%s return type", type.getName(), name)
                .isEqualTo(returnType);
    }

    private static Method findMethod(Class<?> type, String name, Class<?>... parameterTypes) {
        try {
            return type.getMethod(name, parameterTypes);
        } catch (NoSuchMethodException exception) {
            throw new AssertionError("Missing method " + type.getName() + "." + name, exception);
        }
    }
}
