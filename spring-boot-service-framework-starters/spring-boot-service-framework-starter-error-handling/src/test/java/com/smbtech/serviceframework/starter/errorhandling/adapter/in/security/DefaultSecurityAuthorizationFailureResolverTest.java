package com.smbtech.serviceframework.starter.errorhandling.adapter.in.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.smbtech.serviceframework.error.ErrorCategory;
import com.smbtech.serviceframework.error.ErrorExposure;
import com.smbtech.serviceframework.starter.errorhandling.api.security.SecurityErrorCatalog;
import com.smbtech.serviceframework.starter.errorhandling.api.security.SecurityFailureContext;
import com.smbtech.serviceframework.starter.errorhandling.api.security.SecurityFailureReason;
import com.smbtech.serviceframework.starter.errorhandling.api.security.SecurityFailureResolution;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.csrf.CsrfException;

class DefaultSecurityAuthorizationFailureResolverTest {

    private final DefaultSecurityAuthorizationFailureResolver resolver =
            new DefaultSecurityAuthorizationFailureResolver();

    @Test
    void resolvesGenericAccessDeniedWithoutInspectingItsMessage() {
        AccessDeniedException failure =
                new AccessDeniedException(
                        "insufficient_scope payment.write text must not control classification");

        SecurityFailureResolution resolution =
                resolver.resolve(context(failure, false, "", Set.of()));

        assertResolution(
                resolution,
                SecurityFailureReason.ACCESS_DENIED,
                SecurityErrorCatalog.ACCESS_DENIED);
        assertThat(resolution.hasOAuth2Error()).isFalse();
        assertThat(resolution.bearerChallenge()).isFalse();
    }

    @Test
    void resolvesBearerAccessDeniedWithKnownRequiredScopes() {
        AccessDeniedException failure = new AccessDeniedException("authorization denied");

        SecurityFailureResolution resolution =
                resolver.resolve(
                        context(failure, true, "bearer", Set.of("payment.write", "payment.read")));

        assertResolution(
                resolution,
                SecurityFailureReason.INSUFFICIENT_SCOPE,
                SecurityErrorCatalog.INSUFFICIENT_SCOPE);
        assertThat(resolution.oauth2Error().error()).isEqualTo("insufficient_scope");
        assertThat(resolution.oauth2Error().scope()).isEqualTo("payment.read payment.write");
        assertThat(resolution.bearerChallenge()).isTrue();
        assertThat(resolution.resolvedError().notification().metadata()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"anonymous", "basic", "session"})
    void doesNotTreatRequiredScopesAsInsufficientScopeForNonBearerAuthentication(
            String authenticationType) {
        SecurityFailureResolution resolution =
                resolver.resolve(
                        context(
                                new AccessDeniedException("denied"),
                                false,
                                authenticationType,
                                Set.of("payment.write")));

        assertThat(resolution.reason()).isEqualTo(SecurityFailureReason.ACCESS_DENIED);
        assertThat(resolution.hasOAuth2Error()).isFalse();
        assertThat(resolution.bearerChallenge()).isFalse();
    }

    @Test
    void keepsBearerAccessDeniedGenericWhenRequiredScopesAreUnknown() {
        SecurityFailureResolution resolution =
                resolver.resolve(
                        context(new AccessDeniedException("denied"), false, "bearer", Set.of()));

        assertThat(resolution.reason()).isEqualTo(SecurityFailureReason.ACCESS_DENIED);
        assertThat(resolution.hasOAuth2Error()).isFalse();
        assertThat(resolution.bearerChallenge()).isTrue();
    }

    @Test
    void csrfTakesPrecedenceOverBearerAndScopeClassification() {
        CsrfException failure = new CsrfException("csrf-token=secret");

        SecurityFailureResolution resolution =
                resolver.resolve(context(failure, true, "bearer", Set.of("payment.write")));

        assertResolution(
                resolution,
                SecurityFailureReason.CSRF_ACCESS_DENIED,
                SecurityErrorCatalog.CSRF_ACCESS_DENIED);
        assertThat(resolution.hasOAuth2Error()).isFalse();
        assertThat(resolution.bearerChallenge()).isFalse();
        assertThat(resolution.resolvedError().notification().message()).doesNotContain("secret");
        assertThat(resolution.resolvedError().diagnosticMessage()).contains("csrf-token=secret");
    }

    @Test
    void rejectsMissingContextAndNonAuthorizationFailures() {
        assertThatThrownBy(() -> resolver.resolve(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(
                        () ->
                                resolver.resolve(
                                        new SecurityFailureContext(new IllegalStateException())))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static SecurityFailureContext context(
            Throwable failure,
            boolean bearerCredentialsPresent,
            String authenticationType,
            Set<String> requiredScopes) {
        return new SecurityFailureContext(
                failure,
                "POST",
                "/payments/{paymentId}",
                "correlation-123",
                bearerCredentialsPresent,
                authenticationType,
                requiredScopes);
    }

    private static void assertResolution(
            SecurityFailureResolution resolution,
            SecurityFailureReason reason,
            SecurityErrorCatalog definition) {
        assertThat(resolution.reason()).isEqualTo(reason);
        assertThat(resolution.resolvedError().notification().code()).isEqualTo(definition.code());
        assertThat(resolution.resolvedError().notification().message())
                .isEqualTo(definition.publicMessage());
        assertThat(resolution.resolvedError().category()).isEqualTo(ErrorCategory.AUTHORIZATION);
        assertThat(resolution.resolvedError().exposure()).isEqualTo(ErrorExposure.PUBLIC);
    }
}
