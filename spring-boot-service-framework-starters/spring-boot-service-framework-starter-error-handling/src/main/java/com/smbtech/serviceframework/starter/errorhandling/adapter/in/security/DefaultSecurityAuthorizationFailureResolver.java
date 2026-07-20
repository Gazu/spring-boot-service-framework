package com.smbtech.serviceframework.starter.errorhandling.adapter.in.security;

import com.smbtech.serviceframework.commons.notification.Notification;
import com.smbtech.serviceframework.error.ErrorExposure;
import com.smbtech.serviceframework.error.ResolvedError;
import com.smbtech.serviceframework.starter.errorhandling.api.security.OAuth2SecurityError;
import com.smbtech.serviceframework.starter.errorhandling.api.security.SecurityAuthorizationFailureResolver;
import com.smbtech.serviceframework.starter.errorhandling.api.security.SecurityErrorCatalog;
import com.smbtech.serviceframework.starter.errorhandling.api.security.SecurityFailureContext;
import com.smbtech.serviceframework.starter.errorhandling.api.security.SecurityFailureReason;
import com.smbtech.serviceframework.starter.errorhandling.api.security.SecurityFailureResolution;
import java.util.Objects;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.csrf.CsrfException;

/**
 * Classifies authorization failures using structured context and Spring Security exception types
 * without inspecting exception messages or granted authorities.
 */
public final class DefaultSecurityAuthorizationFailureResolver
        implements SecurityAuthorizationFailureResolver {

    /** Creates the default authorization failure resolver. */
    public DefaultSecurityAuthorizationFailureResolver() {}

    @Override
    public SecurityFailureResolution resolve(SecurityFailureContext context) {
        SecurityFailureContext source = Objects.requireNonNull(context, "context must not be null");
        if (!(source.failure() instanceof AccessDeniedException accessDeniedException)) {
            throw new IllegalArgumentException("context failure must be an AccessDeniedException");
        }

        Classification classification = classify(source, accessDeniedException);
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
                        diagnosticMessage(accessDeniedException));
        return new SecurityFailureResolution(
                resolvedError,
                classification.reason(),
                classification.oauth2Error(),
                classification.bearerChallenge());
    }

    private static Classification classify(
            SecurityFailureContext context, AccessDeniedException exception) {
        if (exception instanceof CsrfException) {
            return new Classification(
                    SecurityFailureReason.CSRF_ACCESS_DENIED, OAuth2SecurityError.none(), false);
        }

        boolean bearerAuthentication =
                context.bearerCredentialsPresent() || "bearer".equals(context.authenticationType());
        if (bearerAuthentication && context.hasRequiredScopes()) {
            return new Classification(
                    SecurityFailureReason.INSUFFICIENT_SCOPE,
                    OAuth2SecurityError.insufficientScope(context.requiredScopes()),
                    true);
        }
        return new Classification(
                SecurityFailureReason.ACCESS_DENIED,
                OAuth2SecurityError.none(),
                bearerAuthentication);
    }

    private static String diagnosticMessage(AccessDeniedException exception) {
        StringBuilder diagnostic = new StringBuilder(exception.getClass().getName());
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
