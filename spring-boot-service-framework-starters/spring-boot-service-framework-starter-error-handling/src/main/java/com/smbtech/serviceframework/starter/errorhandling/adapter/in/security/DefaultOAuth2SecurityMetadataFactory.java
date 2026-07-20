package com.smbtech.serviceframework.starter.errorhandling.adapter.in.security;

import com.smbtech.serviceframework.error.metadata.OAuth2ErrorMetadata;
import com.smbtech.serviceframework.error.metadata.RequestErrorMetadata;
import com.smbtech.serviceframework.error.metadata.SecurityErrorMetadata;
import com.smbtech.serviceframework.error.metadata.StandardErrorMetadata;
import com.smbtech.serviceframework.error.metadata.StandardErrorMetadataBuilder;
import com.smbtech.serviceframework.starter.errorhandling.api.security.OAuth2SecurityError;
import com.smbtech.serviceframework.starter.errorhandling.api.security.OAuth2SecurityMetadataFactory;
import com.smbtech.serviceframework.starter.errorhandling.api.security.SecurityFailureContext;
import com.smbtech.serviceframework.starter.errorhandling.api.security.SecurityFailureReason;
import com.smbtech.serviceframework.starter.errorhandling.api.security.SecurityFailureResolution;
import java.util.Objects;

/**
 * Creates safe, versioned metadata using static framework-controlled OAuth2 descriptions and the
 * RFC 6750 error reference.
 */
public final class DefaultOAuth2SecurityMetadataFactory implements OAuth2SecurityMetadataFactory {

    private final boolean oauth2MetadataEnabled;
    private final boolean includeErrorDescription;
    private final boolean includeErrorUri;
    private final boolean includeRequiredScope;

    /** RFC 6750 Bearer error reference used in public metadata. */
    public static final String RFC6750_ERROR_URI =
            "https://www.rfc-editor.org/rfc/rfc6750#section-3.1";

    /** RFC 6750 description exposed without echoing malformed request content. */
    public static final String INVALID_REQUEST_DESCRIPTION = "The Bearer token request is invalid";

    /** RFC 6750 description exposed without revealing token validation details. */
    public static final String INVALID_TOKEN_DESCRIPTION = "The access token is invalid";

    /** Public insufficient scope description. */
    public static final String INSUFFICIENT_SCOPE_DESCRIPTION =
            "The access token does not grant the required scope";

    /** Creates the default OAuth2 security metadata factory. */
    public DefaultOAuth2SecurityMetadataFactory() {
        this(true, true, true, true);
    }

    /**
     * Creates a metadata factory with explicit public OAuth2 exposure settings.
     *
     * @param oauth2MetadataEnabled whether OAuth2 metadata is exposed
     * @param includeErrorDescription whether the safe description is exposed
     * @param includeErrorUri whether the RFC reference is exposed
     * @param includeRequiredScope whether required scopes are exposed
     */
    public DefaultOAuth2SecurityMetadataFactory(
            boolean oauth2MetadataEnabled,
            boolean includeErrorDescription,
            boolean includeErrorUri,
            boolean includeRequiredScope) {
        this.oauth2MetadataEnabled = oauth2MetadataEnabled;
        this.includeErrorDescription = includeErrorDescription;
        this.includeErrorUri = includeErrorUri;
        this.includeRequiredScope = includeRequiredScope;
    }

    @Override
    public StandardErrorMetadata create(
            SecurityFailureContext context, SecurityFailureResolution resolution) {
        SecurityFailureContext sourceContext =
                Objects.requireNonNull(context, "context must not be null");
        SecurityFailureResolution sourceResolution =
                Objects.requireNonNull(resolution, "resolution must not be null");
        String authenticationScheme = authenticationScheme(sourceContext, sourceResolution);
        StandardErrorMetadataBuilder metadata =
                StandardErrorMetadata.builder(sourceResolution.resolvedError().category())
                        .correlationId(sourceContext.correlationId())
                        .security(
                                new SecurityErrorMetadata(
                                        sourceResolution.reason().metadataValue(),
                                        authenticationScheme));
        if (sourceResolution.reason() != SecurityFailureReason.AUTHENTICATION_PROVIDER_FAILURE) {
            metadata.retryable(false);
        }
        if (!sourceContext.method().isEmpty() || !sourceContext.route().isEmpty()) {
            metadata.request(
                    new RequestErrorMetadata(sourceContext.method(), sourceContext.route(), ""));
        }
        if (oauth2MetadataEnabled && sourceResolution.hasOAuth2Error()) {
            metadata.oauth2(oauth2Metadata(sourceResolution.oauth2Error()));
        }
        return metadata.build();
    }

    private static String authenticationScheme(
            SecurityFailureContext context, SecurityFailureResolution resolution) {
        if (!context.authenticationType().isEmpty()) {
            return context.authenticationType();
        }
        return resolution.bearerChallenge() || resolution.hasOAuth2Error() ? "bearer" : "";
    }

    private OAuth2ErrorMetadata oauth2Metadata(OAuth2SecurityError error) {
        return new OAuth2ErrorMetadata(
                error.error(),
                includeErrorDescription ? description(error.error()) : "",
                includeErrorUri ? RFC6750_ERROR_URI : "",
                includeRequiredScope ? error.scope() : "");
    }

    private static String description(String error) {
        return switch (error) {
            case "invalid_request" -> INVALID_REQUEST_DESCRIPTION;
            case "invalid_token" -> INVALID_TOKEN_DESCRIPTION;
            case "insufficient_scope" -> INSUFFICIENT_SCOPE_DESCRIPTION;
            default ->
                    throw new IllegalArgumentException("Unsupported public OAuth2 error: " + error);
        };
    }
}
