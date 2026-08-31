package com.smbtech.serviceframework.starter.errorhandling.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.smbtech.serviceframework.commons.notification.Notification;
import com.smbtech.serviceframework.error.ErrorCategory;
import com.smbtech.serviceframework.error.ErrorExposure;
import com.smbtech.serviceframework.error.ResolvedError;
import com.smbtech.serviceframework.error.metadata.OAuth2ErrorMetadata;
import com.smbtech.serviceframework.error.metadata.SecurityErrorMetadata;
import com.smbtech.serviceframework.error.metadata.StandardErrorMetadata;
import com.smbtech.serviceframework.starter.errorhandling.api.security.OAuth2SecurityError;
import com.smbtech.serviceframework.starter.errorhandling.api.security.SecurityFailureContext;
import com.smbtech.serviceframework.starter.errorhandling.api.security.SecurityFailureReason;
import com.smbtech.serviceframework.starter.errorhandling.api.security.SecurityFailureResolution;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.TestingAuthenticationToken;

class DefaultOAuth2SecurityChallengeWriterTest {

    private final DefaultOAuth2SecurityChallengeWriter writer =
            new DefaultOAuth2SecurityChallengeWriter();

    @Test
    void writesPlainBearerChallengeForMissingCredentials() throws Exception {
        MockHttpServletResponse response = responseWithStatus(418);
        SecurityFailureContext context = authenticationContext(false, "bearer");

        writer.write(
                new MockHttpServletRequest("GET", "/secure"),
                response,
                context,
                resolution(
                        SecurityFailureReason.AUTHENTICATION_REQUIRED,
                        OAuth2SecurityError.none(),
                        true));

        assertChallenge(response, "Bearer", 418);
    }

    @Test
    void writesInvalidRequestChallengeWithControlledValues() throws Exception {
        MockHttpServletResponse response = responseWithStatus(401);

        writer.write(
                new MockHttpServletRequest("POST", "/secure"),
                response,
                authenticationContext(true, "bearer"),
                resolution(
                        SecurityFailureReason.BEARER_REQUEST_INVALID,
                        OAuth2SecurityError.invalidRequest(),
                        true));

        assertChallenge(
                response,
                "Bearer error=\"invalid_request\", error_description=\"The Bearer token request is invalid\", "
                        + "error_uri=\"https://www.rfc-editor.org/rfc/rfc6750#section-3.1\"",
                401);
    }

    @Test
    void writesInvalidTokenChallengeWithoutProviderDescriptionOrResourceMetadata()
            throws Exception {
        MockHttpServletResponse response = responseWithStatus(401);

        writer.write(
                new MockHttpServletRequest("GET", "/secure"),
                response,
                authenticationContext(true, "bearer"),
                resolution(
                        SecurityFailureReason.BEARER_TOKEN_INVALID,
                        OAuth2SecurityError.invalidToken(),
                        true));

        assertChallenge(
                response,
                "Bearer error=\"invalid_token\", error_description=\"The access token is invalid\", "
                        + "error_uri=\"https://www.rfc-editor.org/rfc/rfc6750#section-3.1\"",
                401);
        assertThat(response.getHeader(HttpHeaders.WWW_AUTHENTICATE))
                .doesNotContain("resource_metadata", "tools.ietf.org", "provider-secret");
    }

    @Test
    void preservesNonStandardStatusWithoutApplyingAnAuthenticationFallback() throws Exception {
        MockHttpServletResponse response = responseWithStatus(499);

        writer.write(
                new MockHttpServletRequest("GET", "/secure"),
                response,
                authenticationContext(true, "bearer"),
                resolution(
                        SecurityFailureReason.BEARER_TOKEN_INVALID,
                        OAuth2SecurityError.invalidToken(),
                        true));

        assertChallenge(
                response,
                "Bearer error=\"invalid_token\", error_description=\"The access token is invalid\", "
                        + "error_uri=\"https://www.rfc-editor.org/rfc/rfc6750#section-3.1\"",
                499);
    }

    @Test
    void writesInsufficientScopeChallengeWithRequiredScopes() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/payments/123");
        request.setUserPrincipal(
                new TestingAuthenticationToken("client", "credential", "SCOPE_payment.read"));
        MockHttpServletResponse response = responseWithStatus(403);
        SecurityFailureContext context =
                authorizationContext(Set.of("payment.write", "payment.read"), "bearer");

        writer.write(
                request,
                response,
                context,
                resolution(
                        SecurityFailureReason.INSUFFICIENT_SCOPE,
                        OAuth2SecurityError.insufficientScope(context.requiredScopes()),
                        true));

        assertChallenge(
                response,
                "Bearer error=\"insufficient_scope\", "
                        + "error_description=\"The access token does not grant the required scope\", "
                        + "error_uri=\"https://www.rfc-editor.org/rfc/rfc6750#section-3.1\", "
                        + "scope=\"payment.read payment.write\"",
                403);
    }

    @Test
    void writesPlainBearerChallengeForGenericBearerAccessDenied() throws Exception {
        MockHttpServletResponse response = responseWithStatus(403);

        writer.write(
                new MockHttpServletRequest("GET", "/secure"),
                response,
                authorizationContext(Set.of(), "bearer"),
                resolution(SecurityFailureReason.ACCESS_DENIED, OAuth2SecurityError.none(), true));

        assertChallenge(response, "Bearer", 403);
    }

    @Test
    void omitsChallengeForCsrfAndProviderFailures() throws Exception {
        MockHttpServletResponse csrfResponse = responseWithStatus(403);
        MockHttpServletResponse providerResponse = responseWithStatus(502);

        writer.write(
                new MockHttpServletRequest("POST", "/secure"),
                csrfResponse,
                authorizationContext(Set.of(), "session"),
                resolution(
                        SecurityFailureReason.CSRF_ACCESS_DENIED,
                        OAuth2SecurityError.none(),
                        false));
        writer.write(
                new MockHttpServletRequest("GET", "/secure"),
                providerResponse,
                authenticationContext(true, "bearer"),
                resolution(
                        SecurityFailureReason.AUTHENTICATION_PROVIDER_FAILURE,
                        OAuth2SecurityError.none(),
                        false));

        assertThat(csrfResponse.getHeader(HttpHeaders.WWW_AUTHENTICATE)).isNull();
        assertThat(providerResponse.getHeader(HttpHeaders.WWW_AUTHENTICATE)).isNull();
        assertThat(csrfResponse.getStatus()).isEqualTo(403);
        assertThat(providerResponse.getStatus()).isEqualTo(502);
    }

    @Test
    void doesNotWriteOrChangeCommittedResponse() throws Exception {
        MockHttpServletResponse response = responseWithStatus(401);
        response.setCommitted(true);

        writer.write(
                new MockHttpServletRequest("GET", "/secure"),
                response,
                authenticationContext(true, "bearer"),
                resolution(
                        SecurityFailureReason.BEARER_TOKEN_INVALID,
                        OAuth2SecurityError.invalidToken(),
                        true));

        assertThat(response.getHeader(HttpHeaders.WWW_AUTHENTICATE)).isNull();
        assertThat(response.getContentAsByteArray()).isEmpty();
    }

    @Test
    void rejectsScopeCharactersThatCouldBreakTheChallengeHeader() {
        assertThatThrownBy(
                        () ->
                                OAuth2SecurityError.insufficientScope(
                                        Set.of("payment.write\"injected")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(
                        () ->
                                OAuth2SecurityError.insufficientScope(
                                        Set.of("payment.write\\injected")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(
                        () ->
                                OAuth2SecurityError.insufficientScope(
                                        Set.of("payment.write\r\nInjected")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> OAuth2SecurityError.insufficientScope(Set.of("payment write")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsUnsafeCustomizedMetadataBeforeChangingTheResponse() {
        DefaultOAuth2SecurityChallengeWriter customWriter =
                new DefaultOAuth2SecurityChallengeWriter(
                        (context, resolution) ->
                                StandardErrorMetadata.builder(ErrorCategory.AUTHENTICATION)
                                        .security(
                                                new SecurityErrorMetadata(
                                                        "invalid_token", "bearer"))
                                        .oauth2(
                                                new OAuth2ErrorMetadata(
                                                        "invalid_token",
                                                        "Invalid token\r\nX-Injected: true",
                                                        DefaultOAuth2SecurityMetadataFactory
                                                                .RFC6750_ERROR_URI,
                                                        ""))
                                        .build());
        MockHttpServletResponse response = responseWithStatus(401);

        assertThatThrownBy(
                        () ->
                                customWriter.write(
                                        new MockHttpServletRequest("GET", "/secure"),
                                        response,
                                        authenticationContext(true, "bearer"),
                                        resolution(
                                                SecurityFailureReason.BEARER_TOKEN_INVALID,
                                                OAuth2SecurityError.invalidToken(),
                                                true)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(response.getHeader(HttpHeaders.WWW_AUTHENTICATE)).isNull();
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsByteArray()).isEmpty();
    }

    private static SecurityFailureContext authenticationContext(
            boolean bearerCredentialsPresent, String authenticationType) {
        return new SecurityFailureContext(
                new BadCredentialsException("provider-secret"),
                "GET",
                "/secure",
                "correlation-123",
                bearerCredentialsPresent,
                authenticationType,
                Set.of());
    }

    private static SecurityFailureContext authorizationContext(
            Set<String> requiredScopes, String authenticationType) {
        return new SecurityFailureContext(
                new AccessDeniedException("access denied"),
                "POST",
                "/payments/{paymentId}",
                "correlation-123",
                "bearer".equals(authenticationType),
                authenticationType,
                requiredScopes);
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

    private static MockHttpServletResponse responseWithStatus(int status) {
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(status);
        return response;
    }

    private static void assertChallenge(
            MockHttpServletResponse response, String expectedHeader, int expectedStatus) {
        assertThat(response.getHeader(HttpHeaders.WWW_AUTHENTICATE)).isEqualTo(expectedHeader);
        assertThat(response.getStatus()).isEqualTo(expectedStatus);
        assertThat(response.isCommitted()).isFalse();
        assertThat(response.getContentAsByteArray()).isEmpty();
    }
}
