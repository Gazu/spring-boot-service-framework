package com.smbtech.serviceframework.starter.errorhandling.adapter.in.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.smbtech.serviceframework.starter.errorhandling.api.security.SecurityFailureContext;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerMapping;

class SecurityFailureContextsTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void identifiesBearerWithoutRetainingCredentialsOrRawRequestPath() {
        setAuthentication(new TestingAuthenticationToken("session-user", "session-secret"));
        MockHttpServletRequest request = request("GET", "/payments/private-payment-id");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer bearer-token-secret");
        request.setAttribute(
                HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/payments/{paymentId}");

        SecurityFailureContext context =
                SecurityFailureContexts.authentication(
                        request,
                        new org.springframework.security.authentication.BadCredentialsException(
                                "invalid"));

        assertThat(context.bearerCredentialsPresent()).isTrue();
        assertThat(context.authenticationType()).isEqualTo("bearer");
        assertThat(context.route()).isEqualTo("/payments/{paymentId}");
        assertThat(context.toString())
                .doesNotContain(
                        "private-payment-id",
                        "bearer-token-secret",
                        "session-user",
                        "session-secret");
    }

    @Test
    void identifiesBasicAuthenticationFromTheStandardHeader() {
        setAuthentication(authenticatedSession("basic-user"));
        MockHttpServletRequest request = request("GET", "/secure");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Basic basic-credential-secret");

        SecurityFailureContext context =
                SecurityFailureContexts.authentication(
                        request,
                        new org.springframework.security.authentication.BadCredentialsException(
                                "invalid"));

        assertThat(context.bearerCredentialsPresent()).isFalse();
        assertThat(context.authenticationType()).isEqualTo("basic");
        assertThat(context.toString()).doesNotContain("basic-user", "basic-credential-secret");
    }

    @Test
    void identifiesAnonymousAuthenticationWithoutReadingItsPrincipal() {
        setAuthentication(
                new AnonymousAuthenticationToken(
                        "anonymous-key",
                        "anonymous-principal-secret",
                        AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

        SecurityFailureContext context =
                SecurityFailureContexts.authentication(
                        request("GET", "/secure"),
                        new org.springframework.security.authentication.BadCredentialsException(
                                "invalid"));

        assertThat(context.authenticationType()).isEqualTo("anonymous");
        assertThat(context.toString())
                .doesNotContain("anonymous-key", "anonymous-principal-secret");
    }

    @Test
    void identifiesSessionAndPassesAuthenticationOnlyToRequiredScopePolicy() {
        Authentication authentication = authenticatedSession("session-user-secret");
        setAuthentication(authentication);
        AtomicReference<Authentication> receivedAuthentication = new AtomicReference<>();
        MockHttpServletRequest request = request("POST", "/payments/private-payment-id");
        request.setAttribute(
                HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/payments/{paymentId}");

        SecurityFailureContext context =
                SecurityFailureContexts.authorization(
                        request,
                        new org.springframework.security.access.AccessDeniedException("denied"),
                        (httpRequest, currentAuthentication) -> {
                            receivedAuthentication.set(currentAuthentication);
                            return Set.of("payment.write");
                        });

        assertThat(receivedAuthentication.get()).isSameAs(authentication);
        assertThat(context.authenticationType()).isEqualTo("session");
        assertThat(context.requiredScopes()).containsExactly("payment.write");
        assertThat(context.toString()).doesNotContain("session-user-secret", "private-payment-id");
    }

    private static MockHttpServletRequest request(String method, String path) {
        return new MockHttpServletRequest(method, path);
    }

    private static Authentication authenticatedSession(String principal) {
        TestingAuthenticationToken authentication =
                new TestingAuthenticationToken(principal, "session-credential-secret", "ROLE_USER");
        authentication.setAuthenticated(true);
        return authentication;
    }

    private static void setAuthentication(Authentication authentication) {
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
