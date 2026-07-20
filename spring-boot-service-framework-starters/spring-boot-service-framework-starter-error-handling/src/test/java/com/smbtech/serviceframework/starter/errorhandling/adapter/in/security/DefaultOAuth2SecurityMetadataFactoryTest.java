package com.smbtech.serviceframework.starter.errorhandling.adapter.in.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.smbtech.serviceframework.commons.notification.Notification;
import com.smbtech.serviceframework.error.ErrorExposure;
import com.smbtech.serviceframework.error.ResolvedError;
import com.smbtech.serviceframework.error.metadata.StandardErrorMetadata;
import com.smbtech.serviceframework.starter.errorhandling.api.security.OAuth2SecurityError;
import com.smbtech.serviceframework.starter.errorhandling.api.security.SecurityFailureContext;
import com.smbtech.serviceframework.starter.errorhandling.api.security.SecurityFailureReason;
import com.smbtech.serviceframework.starter.errorhandling.api.security.SecurityFailureResolution;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

class DefaultOAuth2SecurityMetadataFactoryTest {

    private final DefaultOAuth2SecurityMetadataFactory factory =
            new DefaultOAuth2SecurityMetadataFactory();

    @Test
    void createsAuthenticationRequiredMetadataWithoutOAuth2Error() {
        SecurityFailureContext context =
                context(
                        new BadCredentialsException("internal-secret"),
                        "GET",
                        "/secure",
                        "correlation-123",
                        false,
                        "bearer");
        SecurityFailureResolution resolution =
                resolution(
                        SecurityFailureReason.AUTHENTICATION_REQUIRED,
                        OAuth2SecurityError.none(),
                        true);

        Map<String, Object> metadata = factory.create(context, resolution).toMap();

        assertThat(metadata).containsEntry("schemaVersion", "1");
        assertThat(metadata).containsEntry("category", "AUTHENTICATION");
        assertThat(metadata).containsEntry("correlationId", "correlation-123");
        assertThat(metadata).containsEntry("retryable", false);
        assertThat(metadata.get("request")).isEqualTo(Map.of("method", "GET", "route", "/secure"));
        assertThat(metadata.get("security"))
                .isEqualTo(
                        Map.of(
                                "reason", "authentication_required",
                                "authenticationScheme", "bearer"));
        assertThat(metadata).doesNotContainKey("oauth2");
        assertThat(metadata.toString()).doesNotContain("internal-secret");
    }

    @Test
    void createsInvalidRequestMetadataFromFrameworkConstants() {
        SecurityFailureResolution resolution =
                resolution(
                        SecurityFailureReason.BEARER_REQUEST_INVALID,
                        OAuth2SecurityError.invalidRequest(),
                        true);

        Map<String, Object> oauth2 =
                section(
                        factory.create(
                                context(
                                        new BadCredentialsException("provider description"),
                                        "POST",
                                        "/token",
                                        "",
                                        true,
                                        ""),
                                resolution),
                        "oauth2");

        assertThat(oauth2)
                .containsExactly(
                        Map.entry("error", "invalid_request"),
                        Map.entry(
                                "errorDescription",
                                DefaultOAuth2SecurityMetadataFactory.INVALID_REQUEST_DESCRIPTION),
                        Map.entry(
                                "errorUri",
                                DefaultOAuth2SecurityMetadataFactory.RFC6750_ERROR_URI));
    }

    @Test
    void createsInvalidTokenMetadataWithoutProviderDetails() {
        SecurityFailureResolution resolution =
                resolution(
                        SecurityFailureReason.BEARER_TOKEN_INVALID,
                        OAuth2SecurityError.invalidToken(),
                        true);

        StandardErrorMetadata metadata =
                factory.create(
                        context(
                                new BadCredentialsException("issuer.internal token=secret"),
                                "GET",
                                "/secure",
                                "",
                                true,
                                ""),
                        resolution);
        Map<String, Object> oauth2 = section(metadata, "oauth2");

        assertThat(oauth2).containsEntry("error", "invalid_token");
        assertThat(oauth2)
                .containsEntry(
                        "errorDescription",
                        DefaultOAuth2SecurityMetadataFactory.INVALID_TOKEN_DESCRIPTION);
        assertThat(metadata.toMap().toString()).doesNotContain("issuer.internal", "secret");
        assertThat(section(metadata, "security")).containsEntry("authenticationScheme", "bearer");
    }

    @Test
    void createsInsufficientScopeMetadataWithRequiredScopesOnly() {
        SecurityFailureContext context =
                new SecurityFailureContext(
                        new AccessDeniedException("granted scopes must stay internal"),
                        "POST",
                        "/payments/{paymentId}",
                        "correlation-456",
                        true,
                        "bearer",
                        Set.of("payment.write", "payment.read"));
        SecurityFailureResolution resolution =
                resolution(
                        SecurityFailureReason.INSUFFICIENT_SCOPE,
                        OAuth2SecurityError.insufficientScope(context.requiredScopes()),
                        true);

        StandardErrorMetadata metadata = factory.create(context, resolution);
        Map<String, Object> oauth2 = section(metadata, "oauth2");

        assertThat(section(metadata, "security")).containsEntry("reason", "insufficient_scope");
        assertThat(oauth2).containsEntry("error", "insufficient_scope");
        assertThat(oauth2)
                .containsEntry(
                        "errorDescription",
                        DefaultOAuth2SecurityMetadataFactory.INSUFFICIENT_SCOPE_DESCRIPTION);
        assertThat(oauth2).containsEntry("scope", "payment.read payment.write");
    }

    @Test
    void appliesSelectiveOAuth2MetadataExposure() {
        DefaultOAuth2SecurityMetadataFactory selectiveFactory =
                new DefaultOAuth2SecurityMetadataFactory(true, false, false, false);
        SecurityFailureResolution resolution =
                resolution(
                        SecurityFailureReason.INSUFFICIENT_SCOPE,
                        OAuth2SecurityError.insufficientScope(Set.of("payment.write")),
                        true);

        Map<String, Object> oauth2 =
                section(
                        selectiveFactory.create(
                                context(
                                        new AccessDeniedException("secret"),
                                        "POST",
                                        "/payments",
                                        "",
                                        true,
                                        "bearer"),
                                resolution),
                        "oauth2");

        assertThat(oauth2).containsExactly(Map.entry("error", "insufficient_scope"));
    }

    @Test
    void canDisableOnlyTheOAuth2Namespace() {
        DefaultOAuth2SecurityMetadataFactory disabledFactory =
                new DefaultOAuth2SecurityMetadataFactory(false, true, true, true);
        SecurityFailureResolution resolution =
                resolution(
                        SecurityFailureReason.BEARER_TOKEN_INVALID,
                        OAuth2SecurityError.invalidToken(),
                        true);

        Map<String, Object> metadata =
                disabledFactory
                        .create(
                                context(
                                        new BadCredentialsException("secret"),
                                        "GET",
                                        "/secure",
                                        "",
                                        true,
                                        "bearer"),
                                resolution)
                        .toMap();

        assertThat(metadata).doesNotContainKey("oauth2");
        assertThat(metadata.get("security"))
                .isEqualTo(
                        Map.of(
                                "reason", "invalid_token",
                                "authenticationScheme", "bearer"));
    }

    @Test
    void omitsOAuth2AndRetryPolicyForAuthenticationProviderFailure() {
        SecurityFailureResolution resolution =
                resolution(
                        SecurityFailureReason.AUTHENTICATION_PROVIDER_FAILURE,
                        OAuth2SecurityError.none(),
                        false);

        Map<String, Object> metadata =
                factory.create(
                                context(
                                        new BadCredentialsException("provider failure"),
                                        "",
                                        "",
                                        "",
                                        false,
                                        ""),
                                resolution)
                        .toMap();

        assertThat(metadata).containsEntry("category", "DOWNSTREAM");
        assertThat(metadata).doesNotContainKeys("oauth2", "retryable", "request");
        assertThat(metadata.get("security")).isEqualTo(Map.of("reason", "provider_failure"));
    }

    @Test
    void createsCsrfMetadataWithoutOAuth2OrBearerScheme() {
        SecurityFailureResolution resolution =
                resolution(
                        SecurityFailureReason.CSRF_ACCESS_DENIED,
                        OAuth2SecurityError.none(),
                        false);

        Map<String, Object> metadata =
                factory.create(
                                context(
                                        new AccessDeniedException("csrf"),
                                        "POST",
                                        "/payments",
                                        "",
                                        false,
                                        "session"),
                                resolution)
                        .toMap();

        assertThat(metadata.get("security"))
                .isEqualTo(
                        Map.of(
                                "reason", "csrf_rejected",
                                "authenticationScheme", "session"));
        assertThat(metadata).doesNotContainKey("oauth2");
    }

    @Test
    void rejectsMissingInputs() {
        SecurityFailureResolution resolution =
                resolution(SecurityFailureReason.ACCESS_DENIED, OAuth2SecurityError.none(), false);

        assertThatThrownBy(() -> factory.create(null, resolution))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(
                        () ->
                                factory.create(
                                        context(
                                                new AccessDeniedException("denied"),
                                                "GET",
                                                "/secure",
                                                "",
                                                false,
                                                ""),
                                        null))
                .isInstanceOf(NullPointerException.class);
    }

    private static SecurityFailureContext context(
            Throwable failure,
            String method,
            String route,
            String correlationId,
            boolean bearerCredentialsPresent,
            String authenticationType) {
        return new SecurityFailureContext(
                failure,
                method,
                route,
                correlationId,
                bearerCredentialsPresent,
                authenticationType,
                Set.of());
    }

    private static SecurityFailureResolution resolution(
            SecurityFailureReason reason,
            OAuth2SecurityError oauth2Error,
            boolean bearerChallenge) {
        var definition = reason.errorDefinition();
        ResolvedError resolvedError =
                new ResolvedError(
                        Notification.error(definition.code(), definition.publicMessage()),
                        definition.category(),
                        ErrorExposure.PUBLIC,
                        "internal diagnostic");
        return new SecurityFailureResolution(resolvedError, reason, oauth2Error, bearerChallenge);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> section(StandardErrorMetadata metadata, String name) {
        return (Map<String, Object>) metadata.toMap().get(name);
    }
}
