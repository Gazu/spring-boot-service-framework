package com.smbtech.serviceframework.starter.errorhandling.internal;

import com.smbtech.serviceframework.commons.notification.Notification;
import com.smbtech.serviceframework.error.ErrorExposure;
import com.smbtech.serviceframework.error.ResolvedError;
import com.smbtech.serviceframework.starter.errorhandling.api.security.OAuth2SecurityError;
import com.smbtech.serviceframework.starter.errorhandling.api.security.SecurityAuthenticationFailureResolver;
import com.smbtech.serviceframework.starter.errorhandling.api.security.SecurityErrorCatalog;
import com.smbtech.serviceframework.starter.errorhandling.api.security.SecurityFailureContext;
import com.smbtech.serviceframework.starter.errorhandling.api.security.SecurityFailureReason;
import com.smbtech.serviceframework.starter.errorhandling.api.security.SecurityFailureResolution;
import java.util.Objects;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.server.resource.BearerTokenError;

/**
 * Classifies authentication failures using Spring Security exception types and OAuth2 error codes
 * without inspecting exception messages or provider classes.
 */
final class DefaultSecurityAuthenticationFailureResolver
        implements SecurityAuthenticationFailureResolver {

    /** Creates the default authentication failure resolver. */
    public DefaultSecurityAuthenticationFailureResolver() {}

    @Override
    public SecurityFailureResolution resolve(SecurityFailureContext context) {
        SecurityFailureContext source = Objects.requireNonNull(context, "context must not be null");
        if (!(source.failure() instanceof AuthenticationException authenticationException)) {
            throw new IllegalArgumentException(
                    "context failure must be an AuthenticationException");
        }

        Classification classification = classify(source, authenticationException);
        SecurityErrorCatalog definition = classification.reason().errorDefinition();
        ResolvedError resolvedError =
                new ResolvedError(
                        Notification.builder()
                                .code(definition.code())
                                .message(definition.publicMessage())
                                .severity(definition.severity())
                                .build(),
                        definition.category(),
                        ErrorExposure.PUBLIC,
                        diagnosticMessage(authenticationException));
        return new SecurityFailureResolution(
                resolvedError,
                classification.reason(),
                classification.oauth2Error(),
                classification.bearerChallenge());
    }

    private static Classification classify(
            SecurityFailureContext context, AuthenticationException exception) {
        if (exception instanceof OAuth2AuthenticationException oauth2Exception) {
            OAuth2Error error = oauth2Exception.getError();
            String errorCode = error.getErrorCode();
            boolean bearerError = error instanceof BearerTokenError;
            if (OAuth2ErrorCodes.INVALID_REQUEST.equals(errorCode)) {
                return new Classification(
                        SecurityFailureReason.BEARER_REQUEST_INVALID,
                        OAuth2SecurityError.invalidRequest(),
                        true);
            }
            if (OAuth2ErrorCodes.INVALID_TOKEN.equals(errorCode)) {
                return new Classification(
                        SecurityFailureReason.BEARER_TOKEN_INVALID,
                        OAuth2SecurityError.invalidToken(),
                        true);
            }
            if (OAuth2ErrorCodes.SERVER_ERROR.equals(errorCode)
                    || OAuth2ErrorCodes.TEMPORARILY_UNAVAILABLE.equals(errorCode)) {
                return new Classification(
                        SecurityFailureReason.AUTHENTICATION_PROVIDER_FAILURE,
                        OAuth2SecurityError.none(),
                        false);
            }
            if (bearerError || context.bearerCredentialsPresent()) {
                return new Classification(
                        SecurityFailureReason.BEARER_TOKEN_INVALID,
                        OAuth2SecurityError.invalidToken(),
                        true);
            }
        }

        if (context.bearerCredentialsPresent()) {
            return new Classification(
                    SecurityFailureReason.BEARER_TOKEN_INVALID,
                    OAuth2SecurityError.invalidToken(),
                    true);
        }
        return new Classification(
                SecurityFailureReason.AUTHENTICATION_REQUIRED,
                OAuth2SecurityError.none(),
                "bearer".equals(context.authenticationType()));
    }

    private static String diagnosticMessage(AuthenticationException exception) {
        StringBuilder diagnostic = new StringBuilder(exception.getClass().getName());
        if (exception instanceof OAuth2AuthenticationException oauth2Exception) {
            diagnostic.append(" oauth2Error=").append(oauth2Exception.getError().getErrorCode());
        }
        if (exception.getMessage() != null && !exception.getMessage().isBlank()) {
            diagnostic.append(": ").append(exception.getMessage());
        }
        Throwable cause = exception.getCause();
        if (cause != null && cause != exception) {
            diagnostic.append(" cause=").append(cause.getClass().getName());
            if (cause.getMessage() != null && !cause.getMessage().isBlank()) {
                diagnostic.append(": ").append(cause.getMessage());
            }
        }
        return diagnostic.toString();
    }

    private record Classification(
            SecurityFailureReason reason,
            OAuth2SecurityError oauth2Error,
            boolean bearerChallenge) {}
}
