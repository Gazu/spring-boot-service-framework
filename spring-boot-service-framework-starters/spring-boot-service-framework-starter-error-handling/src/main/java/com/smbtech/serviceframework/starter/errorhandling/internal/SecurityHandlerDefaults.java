package com.smbtech.serviceframework.starter.errorhandling.internal;

import com.smbtech.serviceframework.commons.notification.Notification;
import com.smbtech.serviceframework.error.ErrorExposure;
import com.smbtech.serviceframework.error.ResolvedError;
import com.smbtech.serviceframework.starter.errorhandling.api.security.OAuth2SecurityChallengeWriter;
import com.smbtech.serviceframework.starter.errorhandling.api.security.SecurityAuthenticationFailureResolver;
import com.smbtech.serviceframework.starter.errorhandling.api.security.SecurityErrorCatalog;
import com.smbtech.serviceframework.starter.errorhandling.api.security.SecurityFailureReason;
import com.smbtech.serviceframework.starter.errorhandling.api.security.SecurityFailureResolution;
import org.springframework.security.core.AuthenticationException;
import org.springframework.util.ClassUtils;

final class SecurityHandlerDefaults {

    private static final String OAUTH2_AUTHENTICATION_EXCEPTION =
            "org.springframework.security.oauth2.core.OAuth2AuthenticationException";
    private static final String BEARER_TOKEN_ERROR =
            "org.springframework.security.oauth2.server.resource.BearerTokenError";
    private static final String BEARER_TOKEN_ENTRY_POINT =
            "org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint";

    private SecurityHandlerDefaults() {}

    static SecurityAuthenticationFailureResolver authenticationFailureResolver() {
        if (isPresent(OAUTH2_AUTHENTICATION_EXCEPTION) && isPresent(BEARER_TOKEN_ERROR)) {
            return new DefaultSecurityAuthenticationFailureResolver();
        }
        return context -> {
            if (!(context.failure() instanceof AuthenticationException exception)) {
                throw new IllegalArgumentException(
                        "context failure must be an AuthenticationException");
            }
            SecurityErrorCatalog definition = SecurityErrorCatalog.AUTHENTICATION_REQUIRED;
            return new SecurityFailureResolution(
                    new ResolvedError(
                            Notification.builder()
                                    .code(definition.code())
                                    .message(definition.publicMessage())
                                    .severity(definition.severity())
                                    .build(),
                            definition.category(),
                            ErrorExposure.PUBLIC,
                            diagnosticMessage(exception)),
                    SecurityFailureReason.AUTHENTICATION_REQUIRED);
        };
    }

    static OAuth2SecurityChallengeWriter challengeWriter() {
        if (isPresent(BEARER_TOKEN_ENTRY_POINT)) {
            return new DefaultOAuth2SecurityChallengeWriter();
        }
        return (request, response, context, resolution) -> {};
    }

    private static boolean isPresent(String className) {
        return ClassUtils.isPresent(className, ClassUtils.getDefaultClassLoader());
    }

    private static String diagnosticMessage(AuthenticationException exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank()
                ? exception.getClass().getName()
                : exception.getClass().getName() + ": " + message;
    }
}
