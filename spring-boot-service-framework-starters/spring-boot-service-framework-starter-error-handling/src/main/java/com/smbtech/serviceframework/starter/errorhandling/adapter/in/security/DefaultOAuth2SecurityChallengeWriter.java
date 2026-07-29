package com.smbtech.serviceframework.starter.errorhandling.adapter.in.security;

import com.smbtech.serviceframework.error.metadata.OAuth2ErrorMetadata;
import com.smbtech.serviceframework.error.metadata.StandardErrorMetadata;
import com.smbtech.serviceframework.starter.errorhandling.api.security.OAuth2SecurityChallengeWriter;
import com.smbtech.serviceframework.starter.errorhandling.api.security.OAuth2SecurityMetadataFactory;
import com.smbtech.serviceframework.starter.errorhandling.api.security.SecurityFailureContext;
import com.smbtech.serviceframework.starter.errorhandling.api.security.SecurityFailureReason;
import com.smbtech.serviceframework.starter.errorhandling.api.security.SecurityFailureResolution;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.server.resource.BearerTokenError;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;

/**
 * Composes Spring Security Bearer handlers for challenge semantics and then replaces their header
 * with framework-controlled RFC 6750 values. Delegate status changes are restored and delegates are
 * not allowed to commit a body.
 */
public final class DefaultOAuth2SecurityChallengeWriter implements OAuth2SecurityChallengeWriter {

    private final OAuth2SecurityMetadataFactory metadataFactory;
    private final BearerTokenAuthenticationEntryPoint authenticationEntryPoint;
    private final BearerTokenAccessDeniedHandler accessDeniedHandler;

    /** Creates the writer with framework defaults and Spring Security delegates. */
    public DefaultOAuth2SecurityChallengeWriter() {
        this(new DefaultOAuth2SecurityMetadataFactory());
    }

    /**
     * Creates the writer with a replaceable metadata factory.
     *
     * @param metadataFactory OAuth2 response and challenge metadata factory
     */
    public DefaultOAuth2SecurityChallengeWriter(OAuth2SecurityMetadataFactory metadataFactory) {
        this(
                metadataFactory,
                new BearerTokenAuthenticationEntryPoint(),
                new BearerTokenAccessDeniedHandler());
    }

    /**
     * Creates the writer with replaceable metadata and Spring Security delegates.
     *
     * @param metadataFactory OAuth2 response and challenge metadata factory
     * @param authenticationEntryPoint authentication challenge delegate
     * @param accessDeniedHandler authorization challenge delegate
     */
    public DefaultOAuth2SecurityChallengeWriter(
            OAuth2SecurityMetadataFactory metadataFactory,
            BearerTokenAuthenticationEntryPoint authenticationEntryPoint,
            BearerTokenAccessDeniedHandler accessDeniedHandler) {
        this.metadataFactory =
                Objects.requireNonNull(metadataFactory, "metadataFactory must not be null");
        this.authenticationEntryPoint =
                Objects.requireNonNull(
                        authenticationEntryPoint, "authenticationEntryPoint must not be null");
        this.accessDeniedHandler =
                Objects.requireNonNull(accessDeniedHandler, "accessDeniedHandler must not be null");
    }

    @Override
    public void write(
            HttpServletRequest request,
            HttpServletResponse response,
            SecurityFailureContext context,
            SecurityFailureResolution resolution)
            throws IOException {
        HttpServletRequest httpRequest =
                Objects.requireNonNull(request, "request must not be null");
        HttpServletResponse httpResponse =
                Objects.requireNonNull(response, "response must not be null");
        SecurityFailureContext sourceContext =
                Objects.requireNonNull(context, "context must not be null");
        SecurityFailureResolution sourceResolution =
                Objects.requireNonNull(resolution, "resolution must not be null");
        if (!sourceResolution.bearerChallenge() || httpResponse.isCommitted()) {
            return;
        }

        StandardErrorMetadata metadata = metadataFactory.create(sourceContext, sourceResolution);
        String challenge = headerValue(metadata.oauth2());
        int originalStatus = httpResponse.getStatus();
        invokeSpringDelegate(httpRequest, httpResponse, sourceContext, sourceResolution, metadata);
        if (httpResponse.isCommitted()) {
            throw new IOException(
                    "Spring Security Bearer challenge delegate committed the response");
        }
        httpResponse.setStatus(originalStatus);
        httpResponse.setHeader(HttpHeaders.WWW_AUTHENTICATE, challenge);
    }

    private void invokeSpringDelegate(
            HttpServletRequest request,
            HttpServletResponse response,
            SecurityFailureContext context,
            SecurityFailureResolution resolution,
            StandardErrorMetadata metadata)
            throws IOException {
        if (isAuthorizationFailure(resolution.reason())) {
            AccessDeniedException exception =
                    context.failure() instanceof AccessDeniedException accessDenied
                            ? accessDenied
                            : new AccessDeniedException("Access is denied");
            accessDeniedHandler.handle(request, response, exception);
            return;
        }
        authenticationEntryPoint.commence(
                request,
                response,
                authenticationException(metadata.oauth2(), response.getStatus()));
    }

    private static AuthenticationException authenticationException(
            OAuth2ErrorMetadata oauth2, int resolvedStatus) {
        if (oauth2 == null) {
            return new BadCredentialsException("Authentication is required");
        }
        HttpStatus status = HttpStatus.resolve(resolvedStatus);
        if (status == null) {
            return new OAuth2AuthenticationException(
                    new OAuth2Error(oauth2.error(), oauth2.errorDescription(), oauth2.errorUri()));
        }
        BearerTokenError error =
                new BearerTokenError(
                        oauth2.error(),
                        status,
                        oauth2.errorDescription(),
                        oauth2.errorUri(),
                        oauth2.scope());
        return new OAuth2AuthenticationException(error);
    }

    private static boolean isAuthorizationFailure(SecurityFailureReason reason) {
        return switch (reason) {
            case ACCESS_DENIED, INSUFFICIENT_SCOPE, CSRF_ACCESS_DENIED -> true;
            default -> false;
        };
    }

    private static String headerValue(OAuth2ErrorMetadata oauth2) {
        if (oauth2 == null) {
            return "Bearer";
        }
        validateHeaderParameter(oauth2.error());
        StringBuilder header =
                new StringBuilder("Bearer error=\"").append(oauth2.error()).append('\"');
        appendParameter(header, "error_description", oauth2.errorDescription());
        appendParameter(header, "error_uri", oauth2.errorUri());
        appendParameter(header, "scope", oauth2.scope());
        return header.toString();
    }

    private static void appendParameter(StringBuilder header, String name, String value) {
        if (value != null && !value.isEmpty()) {
            validateHeaderParameter(value);
            header.append(", ").append(name).append("=\"").append(value).append('\"');
        }
    }

    private static void validateHeaderParameter(String value) {
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character < 0x20 || character > 0x7E || character == '"' || character == '\\') {
                throw new IllegalArgumentException(
                        "WWW-Authenticate parameter contains an invalid character");
            }
        }
    }
}
