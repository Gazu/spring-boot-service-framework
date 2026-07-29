package com.smbtech.serviceframework.starter.errorhandling.api.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.smbtech.serviceframework.commons.notification.Notification;
import com.smbtech.serviceframework.error.ErrorCategory;
import com.smbtech.serviceframework.error.ErrorExposure;
import com.smbtech.serviceframework.error.ResolvedError;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SecurityFailureModelsTest {

    @Test
    void normalizesSafeSecurityFailureContext() {
        IllegalStateException failure = new IllegalStateException("internal diagnostic");
        Set<String> scopes = new LinkedHashSet<>(Arrays.asList("payment.write", "payment.read"));

        SecurityFailureContext context =
                new SecurityFailureContext(
                        failure,
                        " post ",
                        " /payments/{paymentId} ",
                        " correlation-123 ",
                        true,
                        " Bearer ",
                        scopes);
        scopes.add("payment.delete");

        assertThat(context.failure()).isSameAs(failure);
        assertThat(context.method()).isEqualTo("POST");
        assertThat(context.route()).isEqualTo("/payments/{paymentId}");
        assertThat(context.correlationId()).isEqualTo("correlation-123");
        assertThat(context.bearerCredentialsPresent()).isTrue();
        assertThat(context.authenticationType()).isEqualTo("bearer");
        assertThat(context.requiredScopes()).containsExactly("payment.read", "payment.write");
        assertThat(context.hasRequiredScopes()).isTrue();
        assertThatThrownBy(() -> context.requiredScopes().add("payment.delete"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void createsMinimalContextWithoutOptionalRequestData() {
        RuntimeException failure = new RuntimeException("failure");

        SecurityFailureContext context = new SecurityFailureContext(failure);

        assertThat(context.failure()).isSameAs(failure);
        assertThat(context.method()).isEmpty();
        assertThat(context.route()).isEmpty();
        assertThat(context.correlationId()).isEmpty();
        assertThat(context.authenticationType()).isEmpty();
        assertThat(context.bearerCredentialsPresent()).isFalse();
        assertThat(context.hasRequiredScopes()).isFalse();
    }

    @Test
    void exposesOnlySupportedOAuth2Errors() {
        assertThat(OAuth2SecurityError.none().isPresent()).isFalse();
        assertThat(OAuth2SecurityError.invalidRequest().error()).isEqualTo("invalid_request");
        assertThat(OAuth2SecurityError.invalidToken().error()).isEqualTo("invalid_token");

        OAuth2SecurityError insufficientScope =
                OAuth2SecurityError.insufficientScope(Set.of("payment.write", "payment.read"));
        assertThat(insufficientScope.error()).isEqualTo("insufficient_scope");
        assertThat(insufficientScope.scope()).isEqualTo("payment.read payment.write");

        assertThatThrownBy(() -> new OAuth2SecurityError("server_error", ""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new OAuth2SecurityError("invalid_token", "payment.read"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new OAuth2SecurityError("", "payment.read"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(
                        () ->
                                OAuth2SecurityError.insufficientScope(
                                        Arrays.asList("payment.read", " ")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void mapsEveryFailureReasonToItsCatalogDefinition() {
        assertThat(
                        Arrays.stream(SecurityFailureReason.values())
                                .map(SecurityFailureReason::errorDefinition))
                .containsExactly(SecurityErrorCatalog.values());
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
    }

    @Test
    void carriesResolvedErrorAndChallengeData() {
        ResolvedError resolvedError =
                new ResolvedError(
                        Notification.error(
                                SecurityErrorCatalog.INSUFFICIENT_SCOPE.code(),
                                SecurityErrorCatalog.INSUFFICIENT_SCOPE.publicMessage()),
                        ErrorCategory.AUTHORIZATION,
                        ErrorExposure.PUBLIC,
                        "AccessDeniedException: internal diagnostic");

        SecurityFailureResolution resolution =
                new SecurityFailureResolution(
                        resolvedError,
                        SecurityFailureReason.INSUFFICIENT_SCOPE,
                        OAuth2SecurityError.insufficientScope(Set.of("payment.write")),
                        true);

        assertThat(resolution.resolvedError()).isSameAs(resolvedError);
        assertThat(resolution.reason()).isEqualTo(SecurityFailureReason.INSUFFICIENT_SCOPE);
        assertThat(resolution.oauth2Error().scope()).isEqualTo("payment.write");
        assertThat(resolution.hasOAuth2Error()).isTrue();
        assertThat(resolution.bearerChallenge()).isTrue();

        ResolvedError replacement = resolvedError.withExposure(ErrorExposure.INTERNAL);
        SecurityFailureResolution updated = resolution.withResolvedError(replacement);
        assertThat(updated.resolvedError()).isSameAs(replacement);
        assertThat(updated.reason()).isEqualTo(resolution.reason());
        assertThat(updated.oauth2Error()).isEqualTo(resolution.oauth2Error());
        assertThat(updated.bearerChallenge()).isEqualTo(resolution.bearerChallenge());
        assertThat(resolution.resolvedError()).isSameAs(resolvedError);

        SecurityFailureResolution generic =
                new SecurityFailureResolution(resolvedError, SecurityFailureReason.ACCESS_DENIED);
        assertThat(generic.hasOAuth2Error()).isFalse();
        assertThat(generic.bearerChallenge()).isFalse();
    }

    @Test
    void rejectsMissingRequiredModelValues() {
        ResolvedError resolvedError =
                new ResolvedError(
                        Notification.error("E_SECURITY_TEST", "Security failure"),
                        ErrorCategory.AUTHENTICATION,
                        ErrorExposure.PUBLIC,
                        "diagnostic");

        assertThatThrownBy(() -> new SecurityFailureContext(null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(
                        () ->
                                new SecurityFailureContext(
                                        new RuntimeException(),
                                        "GET",
                                        "/secure",
                                        "",
                                        true,
                                        "bearer",
                                        Set.of(" ")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(
                        () ->
                                new SecurityFailureResolution(
                                        null, SecurityFailureReason.AUTHENTICATION_REQUIRED))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new SecurityFailureResolution(resolvedError, null))
                .isInstanceOf(NullPointerException.class);
    }
}
