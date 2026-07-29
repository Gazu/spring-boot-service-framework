package com.smbtech.serviceframework.starter.errorhandling.adapter.in.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.smbtech.serviceframework.starter.errorhandling.adapter.in.web.DefaultNotificationResponseFactory;
import com.smbtech.serviceframework.starter.errorhandling.api.ErrorMetricsRecorder;
import com.smbtech.serviceframework.starter.errorhandling.api.ErrorReporter;
import com.smbtech.serviceframework.starter.errorhandling.api.security.SecurityErrorCatalog;
import com.smbtech.serviceframework.starter.errorhandling.api.security.SecurityFailureContext;
import com.smbtech.serviceframework.starter.errorhandling.customizer.ErrorCustomizationPipeline;
import com.smbtech.serviceframework.starter.errorhandling.serialization.NotificationJsonResponseWriter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.server.resource.BearerTokenErrors;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.web.servlet.HandlerMapping;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class SecurityHandlersResolutionTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void resolvesInvalidBearerTokenAndWritesStandardChallenge() throws Exception {
        SecurityAuthenticationEntryPoint entryPoint =
                new SecurityAuthenticationEntryPoint(
                        new DefaultNotificationResponseFactory(),
                        new NotificationJsonResponseWriter(objectMapper));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/payments/42");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer bearer-token-secret");
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(
                request,
                response,
                new OAuth2AuthenticationException(
                        BearerTokenErrors.invalidToken("provider-token-detail")));

        JsonNode notification = objectMapper.readTree(response.getContentAsByteArray());
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(notification.path("code").asText())
                .isEqualTo(SecurityErrorCatalog.BEARER_TOKEN_INVALID.code());
        assertThat(notification.at("/metadata/category").asText()).isEqualTo("AUTHENTICATION");
        assertThat(notification.path("metadata").size()).isEqualTo(1);
        assertThat(response.getHeader(HttpHeaders.WWW_AUTHENTICATE))
                .isEqualTo(
                        "Bearer error=\"invalid_token\", "
                                + "error_description=\"The access token is invalid\", "
                                + "error_uri=\"https://www.rfc-editor.org/rfc/rfc6750#section-3.1\"");
        assertThat(response.getContentAsString())
                .doesNotContain("bearer-token-secret", "provider-token-detail");
    }

    @Test
    void usesTheConfiguredStatusResolverForBearerResponsesAndChallenges() throws Exception {
        DefaultNotificationResponseFactory responseFactory =
                new DefaultNotificationResponseFactory(
                        resolvedError -> HttpStatusCode.valueOf(499), notification -> notification);
        SecurityAuthenticationEntryPoint entryPoint =
                new SecurityAuthenticationEntryPoint(
                        responseFactory, new NotificationJsonResponseWriter(objectMapper));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/payments");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer bearer-token-secret");
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(
                request,
                response,
                new OAuth2AuthenticationException(
                        BearerTokenErrors.invalidToken("provider detail")));

        assertThat(response.getStatus()).isEqualTo(499);
        assertThat(response.getHeader(HttpHeaders.WWW_AUTHENTICATE))
                .startsWith("Bearer error=\"invalid_token\"");
    }

    @Test
    void resolvesAuthenticationProviderFailuresWithTheDownstreamStatus() throws Exception {
        SecurityAuthenticationEntryPoint entryPoint =
                new SecurityAuthenticationEntryPoint(
                        new DefaultNotificationResponseFactory(),
                        new NotificationJsonResponseWriter(objectMapper));
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(
                new MockHttpServletRequest("GET", "/payments"),
                response,
                new OAuth2AuthenticationException(
                        new OAuth2Error(
                                OAuth2ErrorCodes.TEMPORARILY_UNAVAILABLE,
                                "provider detail",
                                "https://provider.internal/error")));

        JsonNode notification = objectMapper.readTree(response.getContentAsByteArray());
        assertThat(response.getStatus()).isEqualTo(502);
        assertThat(notification.path("code").asText())
                .isEqualTo(SecurityErrorCatalog.AUTHENTICATION_PROVIDER_FAILURE.code());
        assertThat(response.getHeader(HttpHeaders.WWW_AUTHENTICATE)).isNull();
        assertThat(response.getContentAsString())
                .doesNotContain("provider detail", "provider.internal");
    }

    @Test
    void resolvesRequiredScopesInsteadOfReadingGrantedTokenScopes() throws Exception {
        SecurityAccessDeniedHandler handler =
                new SecurityAccessDeniedHandler(
                        new DefaultNotificationResponseFactory(),
                        new NotificationJsonResponseWriter(objectMapper),
                        ErrorReporter.noop(),
                        ErrorMetricsRecorder.noop(),
                        emptyCustomizationPipeline(),
                        new DefaultSecurityAuthorizationFailureResolver(),
                        (request, authentication) -> Set.of("payment.write", "payment.read"),
                        new DefaultOAuth2SecurityChallengeWriter());
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/payments/42");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer bearer-token-secret");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handle(request, response, new AccessDeniedException("granted scope detail"));

        JsonNode notification = objectMapper.readTree(response.getContentAsByteArray());
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(notification.path("code").asText())
                .isEqualTo(SecurityErrorCatalog.INSUFFICIENT_SCOPE.code());
        assertThat(notification.at("/metadata/category").asText()).isEqualTo("AUTHORIZATION");
        assertThat(notification.path("metadata").size()).isEqualTo(1);
        assertThat(response.getHeader(HttpHeaders.WWW_AUTHENTICATE))
                .isEqualTo(
                        "Bearer error=\"insufficient_scope\", "
                                + "error_description=\"The access token does not grant the required scope\", "
                                + "error_uri=\"https://www.rfc-editor.org/rfc/rfc6750#section-3.1\", "
                                + "scope=\"payment.read payment.write\"");
        assertThat(response.getContentAsString())
                .doesNotContain("bearer-token-secret", "granted scope detail");
    }

    @Test
    void resolvesCsrfBeforeBearerScopeAndDoesNotWriteAChallenge() throws Exception {
        SecurityAccessDeniedHandler handler =
                new SecurityAccessDeniedHandler(
                        new DefaultNotificationResponseFactory(),
                        new NotificationJsonResponseWriter(objectMapper),
                        ErrorReporter.noop(),
                        ErrorMetricsRecorder.noop(),
                        emptyCustomizationPipeline(),
                        new DefaultSecurityAuthorizationFailureResolver(),
                        (request, authentication) -> Set.of("payment.write"),
                        new DefaultOAuth2SecurityChallengeWriter());
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/payments");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer bearer-token-secret");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handle(request, response, new CsrfException("csrf-token-secret"));

        JsonNode notification = objectMapper.readTree(response.getContentAsByteArray());
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(notification.path("code").asText())
                .isEqualTo(SecurityErrorCatalog.CSRF_ACCESS_DENIED.code());
        assertThat(response.getHeader(HttpHeaders.WWW_AUTHENTICATE)).isNull();
        assertThat(response.getContentAsString()).doesNotContain("csrf-token-secret");
    }

    @Test
    void delegatesContextResolutionAndChallengeWritingToReplaceableComponents() throws Exception {
        AtomicReference<SecurityFailureContext> resolvedContext = new AtomicReference<>();
        AtomicReference<SecurityFailureContext> challengeContext = new AtomicReference<>();
        DefaultSecurityAuthenticationFailureResolver defaultResolver =
                new DefaultSecurityAuthenticationFailureResolver();
        SecurityAuthenticationEntryPoint entryPoint =
                new SecurityAuthenticationEntryPoint(
                        new DefaultNotificationResponseFactory(),
                        new NotificationJsonResponseWriter(objectMapper),
                        ErrorReporter.noop(),
                        ErrorMetricsRecorder.noop(),
                        emptyCustomizationPipeline(),
                        context -> {
                            resolvedContext.set(context);
                            return defaultResolver.resolve(context);
                        },
                        (request, response, context, resolution) -> challengeContext.set(context));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/payments/42");
        request.setAttribute(
                HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/payments/{paymentId}");

        entryPoint.commence(
                request,
                new MockHttpServletResponse(),
                new org.springframework.security.authentication.BadCredentialsException("failure"));

        assertThat(resolvedContext.get()).isSameAs(challengeContext.get());
        assertThat(resolvedContext.get().method()).isEqualTo("GET");
        assertThat(resolvedContext.get().route()).isEqualTo("/payments/{paymentId}");
        assertThat(resolvedContext.get().bearerCredentialsPresent()).isFalse();
        assertThat(resolvedContext.get().authenticationType()).isEmpty();
    }

    @Test
    void enrichesSecurityMetadataBeforeResolvedErrorCustomizers() throws Exception {
        AtomicReference<Map<String, Object>> observedMetadata = new AtomicReference<>();
        ErrorCustomizationPipeline customizationPipeline =
                new ErrorCustomizationPipeline(
                        List.of(
                                (cause, resolvedError, request) -> {
                                    observedMetadata.set(resolvedError.notification().metadata());
                                    return resolvedError;
                                }),
                        List.of());
        SecurityAuthenticationEntryPoint entryPoint =
                new SecurityAuthenticationEntryPoint(
                        new DefaultNotificationResponseFactory(),
                        new NotificationJsonResponseWriter(objectMapper),
                        ErrorReporter.noop(),
                        ErrorMetricsRecorder.noop(),
                        customizationPipeline,
                        new DefaultSecurityAuthenticationFailureResolver(),
                        new DefaultOAuth2SecurityMetadataFactory(),
                        new DefaultOAuth2SecurityChallengeWriter());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/payments");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer bearer-token-secret");

        entryPoint.commence(
                request,
                new MockHttpServletResponse(),
                new OAuth2AuthenticationException(BearerTokenErrors.invalidToken("detail")));

        assertThat(observedMetadata.get())
                .containsEntry("category", "AUTHENTICATION")
                .containsKey("oauth2");
    }

    private static ErrorCustomizationPipeline emptyCustomizationPipeline() {
        return new ErrorCustomizationPipeline(List.of(), List.of());
    }
}
