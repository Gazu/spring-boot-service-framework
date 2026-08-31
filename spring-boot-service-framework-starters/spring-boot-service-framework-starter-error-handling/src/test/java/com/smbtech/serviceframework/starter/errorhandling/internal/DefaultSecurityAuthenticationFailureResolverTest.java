package com.smbtech.serviceframework.starter.errorhandling.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.smbtech.serviceframework.error.ErrorCategory;
import com.smbtech.serviceframework.error.ErrorExposure;
import com.smbtech.serviceframework.starter.errorhandling.api.security.SecurityErrorCatalog;
import com.smbtech.serviceframework.starter.errorhandling.api.security.SecurityFailureContext;
import com.smbtech.serviceframework.starter.errorhandling.api.security.SecurityFailureReason;
import com.smbtech.serviceframework.starter.errorhandling.api.security.SecurityFailureResolution;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.server.resource.BearerTokenErrors;
import org.springframework.security.oauth2.server.resource.introspection.OAuth2IntrospectionException;

class DefaultSecurityAuthenticationFailureResolverTest {

    private final DefaultSecurityAuthenticationFailureResolver resolver =
            new DefaultSecurityAuthenticationFailureResolver();

    @ParameterizedTest
    @ValueSource(
            strings = {
                "invalid_request",
                "invalid_token",
                "insufficient_scope",
                "JWT expired",
                "Opaque token inactive"
            })
    void resolvesMissingCredentialsWithoutClassifyingByMessage(String misleadingMessage) {
        BadCredentialsException failure =
                new BadCredentialsException(
                        misleadingMessage + " text must not control classification");

        SecurityFailureResolution resolution =
                resolver.resolve(new SecurityFailureContext(failure));

        assertResolution(
                resolution,
                SecurityFailureReason.AUTHENTICATION_REQUIRED,
                SecurityErrorCatalog.AUTHENTICATION_REQUIRED,
                ErrorCategory.AUTHENTICATION);
        assertThat(resolution.hasOAuth2Error()).isFalse();
        assertThat(resolution.bearerChallenge()).isFalse();
    }

    @Test
    void createsBearerChallengeForMissingCredentialsWhenBearerIsTheConfiguredScheme() {
        SecurityFailureContext context =
                context(new BadCredentialsException("credentials missing"), false, "bearer");

        SecurityFailureResolution resolution = resolver.resolve(context);

        assertThat(resolution.reason()).isEqualTo(SecurityFailureReason.AUTHENTICATION_REQUIRED);
        assertThat(resolution.hasOAuth2Error()).isFalse();
        assertThat(resolution.bearerChallenge()).isTrue();
    }

    @Test
    void resolvesStructuredInvalidRequestError() {
        OAuth2AuthenticationException failure =
                new OAuth2AuthenticationException(
                        BearerTokenErrors.invalidRequest(
                                "provider description must remain internal"));

        SecurityFailureResolution resolution = resolver.resolve(context(failure, true, "bearer"));

        assertResolution(
                resolution,
                SecurityFailureReason.BEARER_REQUEST_INVALID,
                SecurityErrorCatalog.BEARER_REQUEST_INVALID,
                ErrorCategory.AUTHENTICATION);
        assertThat(resolution.oauth2Error().error()).isEqualTo("invalid_request");
        assertThat(resolution.bearerChallenge()).isTrue();
        assertThat(resolution.resolvedError().notification().metadata()).isEmpty();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tokenValidationFailures")
    void collapsesTokenValidationDetailsIntoBearerTokenInvalid(
            String scenario, RuntimeException cause) {
        OAuth2AuthenticationException failure =
                new OAuth2AuthenticationException(
                        BearerTokenErrors.invalidToken("provider detail"), cause);

        SecurityFailureResolution resolution = resolver.resolve(context(failure, true, "bearer"));

        assertResolution(
                resolution,
                SecurityFailureReason.BEARER_TOKEN_INVALID,
                SecurityErrorCatalog.BEARER_TOKEN_INVALID,
                ErrorCategory.AUTHENTICATION);
        assertThat(resolution.oauth2Error().error()).isEqualTo("invalid_token");
        assertThat(resolution.resolvedError().notification().message())
                .isEqualTo("Bearer token is invalid");
        assertThat(resolution.resolvedError().notification().message())
                .doesNotContain(cause.getMessage());
        assertThat(resolution.resolvedError().diagnosticMessage())
                .contains(
                        "oauth2Error=invalid_token",
                        cause.getClass().getName(),
                        cause.getMessage());
    }

    @ParameterizedTest
    @ValueSource(
            strings = {OAuth2ErrorCodes.SERVER_ERROR, OAuth2ErrorCodes.TEMPORARILY_UNAVAILABLE})
    void resolvesProviderFailuresAsDownstreamErrors(String errorCode) {
        OAuth2AuthenticationException failure =
                new OAuth2AuthenticationException(
                        new OAuth2Error(
                                errorCode, "provider-secret", "https://provider.internal/errors"));

        SecurityFailureResolution resolution = resolver.resolve(context(failure, true, "bearer"));

        assertResolution(
                resolution,
                SecurityFailureReason.AUTHENTICATION_PROVIDER_FAILURE,
                SecurityErrorCatalog.AUTHENTICATION_PROVIDER_FAILURE,
                ErrorCategory.DOWNSTREAM);
        assertThat(resolution.hasOAuth2Error()).isFalse();
        assertThat(resolution.bearerChallenge()).isFalse();
        assertThat(resolution.resolvedError().notification().message())
                .doesNotContain("provider-secret", "provider.internal");
    }

    @Test
    void treatsUnknownOAuth2ErrorAsInvalidTokenOnlyWhenBearerCredentialsWerePresent() {
        OAuth2AuthenticationException failure =
                new OAuth2AuthenticationException(new OAuth2Error("custom_provider_error"));

        SecurityFailureResolution bearerResolution =
                resolver.resolve(context(failure, true, "bearer"));
        SecurityFailureResolution genericResolution = resolver.resolve(context(failure, false, ""));

        assertThat(bearerResolution.reason()).isEqualTo(SecurityFailureReason.BEARER_TOKEN_INVALID);
        assertThat(bearerResolution.oauth2Error().error()).isEqualTo("invalid_token");
        assertThat(genericResolution.reason())
                .isEqualTo(SecurityFailureReason.AUTHENTICATION_REQUIRED);
        assertThat(genericResolution.hasOAuth2Error()).isFalse();
    }

    @Test
    void treatsGenericFailureWithBearerCredentialsAsInvalidToken() {
        SecurityFailureResolution resolution =
                resolver.resolve(
                        context(new BadCredentialsException("provider message"), true, "bearer"));

        assertThat(resolution.reason()).isEqualTo(SecurityFailureReason.BEARER_TOKEN_INVALID);
        assertThat(resolution.oauth2Error().error()).isEqualTo("invalid_token");
        assertThat(resolution.bearerChallenge()).isTrue();
    }

    @Test
    void rejectsMissingContextAndNonAuthenticationFailures() {
        assertThatThrownBy(() -> resolver.resolve(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(
                        () ->
                                resolver.resolve(
                                        new SecurityFailureContext(new IllegalStateException())))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static SecurityFailureContext context(
            Throwable failure, boolean bearerCredentialsPresent, String authenticationType) {
        return new SecurityFailureContext(
                failure,
                "GET",
                "/secure",
                "correlation-123",
                bearerCredentialsPresent,
                authenticationType,
                Set.of());
    }

    private static Stream<Arguments> tokenValidationFailures() {
        return Stream.of(
                Arguments.of(
                        "expired JWT",
                        new JwtValidationException(
                                "JWT expired",
                                List.of(new OAuth2Error("invalid_token", "expired", null)))),
                Arguments.of(
                        "invalid JWT issuer",
                        new JwtValidationException(
                                "JWT issuer rejected",
                                List.of(new OAuth2Error("invalid_token", "issuer", null)))),
                Arguments.of(
                        "invalid JWT signature", new BadJwtException("JWT signature rejected")),
                Arguments.of(
                        "inactive opaque token",
                        new OAuth2IntrospectionException("Opaque token inactive")));
    }

    private static void assertResolution(
            SecurityFailureResolution resolution,
            SecurityFailureReason reason,
            SecurityErrorCatalog definition,
            ErrorCategory category) {
        assertThat(resolution.reason()).isEqualTo(reason);
        assertThat(resolution.resolvedError().notification().code()).isEqualTo(definition.code());
        assertThat(resolution.resolvedError().notification().message())
                .isEqualTo(definition.publicMessage());
        assertThat(resolution.resolvedError().category()).isEqualTo(category);
        assertThat(resolution.resolvedError().exposure()).isEqualTo(ErrorExposure.PUBLIC);
    }
}
