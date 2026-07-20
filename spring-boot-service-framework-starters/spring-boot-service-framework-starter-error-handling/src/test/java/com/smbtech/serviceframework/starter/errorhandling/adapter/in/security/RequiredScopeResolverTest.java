package com.smbtech.serviceframework.starter.errorhandling.adapter.in.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.smbtech.serviceframework.starter.errorhandling.api.security.RequiredScopeResolver;
import com.smbtech.serviceframework.starter.errorhandling.api.security.SecurityFailureContext;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;

class RequiredScopeResolverTest {

    @Test
    void defaultResolverNeverUsesGrantedAuthoritiesAsRequiredScopes() {
        DefaultRequiredScopeResolver resolver = new DefaultRequiredScopeResolver();
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/payments");
        Authentication authentication =
                new TestingAuthenticationToken(
                        "client", "credential", "SCOPE_payment.read", "SCOPE_payment.write");

        Set<String> requiredScopes = resolver.resolve(request, authentication);

        assertThat(requiredScopes).isEmpty();
        assertThatThrownBy(() -> requiredScopes.add("payment.delete"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void defaultResolverSupportsMissingAuthentication() {
        DefaultRequiredScopeResolver resolver = new DefaultRequiredScopeResolver();

        assertThat(resolver.resolve(new MockHttpServletRequest("GET", "/secure"), null)).isEmpty();
        assertThatThrownBy(() -> resolver.resolve(null, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void applicationResolverCanUseRequestPolicyWithoutInspectingGrantedScopes() {
        AtomicReference<Authentication> receivedAuthentication = new AtomicReference<>();
        RequiredScopeResolver resolver =
                (request, authentication) -> {
                    receivedAuthentication.set(authentication);
                    return "/payments/{paymentId}".equals(request.getAttribute("routeTemplate"))
                            ? new LinkedHashSet<>(Set.of("payment.write", "payment.read"))
                            : Set.of();
                };
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/payments/123");
        request.setAttribute("routeTemplate", "/payments/{paymentId}");
        Authentication authentication =
                new TestingAuthenticationToken("client", "credential", "SCOPE_profile.read");

        Set<String> requiredScopes = resolver.resolve(request, authentication);
        SecurityFailureContext context =
                new SecurityFailureContext(
                        new org.springframework.security.access.AccessDeniedException("denied"),
                        request.getMethod(),
                        request.getAttribute("routeTemplate").toString(),
                        "correlation-123",
                        true,
                        "bearer",
                        requiredScopes);

        assertThat(receivedAuthentication.get()).isSameAs(authentication);
        assertThat(context.requiredScopes()).containsExactly("payment.read", "payment.write");
    }
}
