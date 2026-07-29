package com.smbtech.serviceframework.starter.errorhandling.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.smbtech.serviceframework.error.ErrorExposure;
import com.smbtech.serviceframework.error.FallbackThrowableErrorResolver;
import com.smbtech.serviceframework.starter.errorhandling.adapter.in.security.DefaultSecurityAuthenticationFailureResolver;
import com.smbtech.serviceframework.starter.errorhandling.adapter.in.security.DefaultSecurityAuthorizationFailureResolver;
import com.smbtech.serviceframework.starter.errorhandling.adapter.in.security.SecurityAccessDeniedHandler;
import com.smbtech.serviceframework.starter.errorhandling.adapter.in.security.SecurityAuthenticationEntryPoint;
import com.smbtech.serviceframework.starter.errorhandling.api.security.RequiredScopeResolver;
import com.smbtech.serviceframework.starter.errorhandling.api.security.SecurityAuthenticationFailureResolver;
import com.smbtech.serviceframework.starter.errorhandling.api.security.SecurityAuthorizationFailureResolver;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationManagerResolver;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.BearerTokenErrors;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import tools.jackson.databind.ObjectMapper;

class SpringSecurityErrorHandlingIntegrationTest {

    private static final String AUTHENTICATION_REQUIRED =
            "E_SERVICE_FRAMEWORK_SECURITY_AUTHENTICATION_0001";
    private static final String INVALID_REQUEST =
            "E_SERVICE_FRAMEWORK_SECURITY_AUTHENTICATION_0002";
    private static final String INVALID_TOKEN = "E_SERVICE_FRAMEWORK_SECURITY_AUTHENTICATION_0003";
    private static final String PROVIDER_FAILURE =
            "E_SERVICE_FRAMEWORK_SECURITY_AUTHENTICATION_0004";
    private static final String ACCESS_DENIED = "E_SERVICE_FRAMEWORK_SECURITY_AUTHORIZATION_0001";
    private static final String INSUFFICIENT_SCOPE =
            "E_SERVICE_FRAMEWORK_SECURITY_AUTHORIZATION_0002";
    private static final String CSRF_REJECTED = "E_SERVICE_FRAMEWORK_SECURITY_AUTHORIZATION_0003";

    private final WebApplicationContextRunner contextRunner = contextRunner(ErrorExposure.PUBLIC);

    private final WebApplicationContextRunner internalContextRunner =
            contextRunner(ErrorExposure.INTERNAL);

    @Test
    void returnsAuthenticationRequiredWithoutCredentials() {
        contextRunner.run(
                context ->
                        perform(
                                context,
                                mockMvc ->
                                        mockMvc.perform(get("/authenticated"))
                                                .andExpect(status().isUnauthorized())
                                                .andExpect(
                                                        content()
                                                                .contentTypeCompatibleWith(
                                                                        MediaType.APPLICATION_JSON))
                                                .andExpect(
                                                        jsonPath("$.code")
                                                                .value(AUTHENTICATION_REQUIRED))
                                                .andExpect(
                                                        jsonPath("$.metadata.category")
                                                                .value("AUTHENTICATION"))
                                                .andExpect(
                                                        jsonPath("$.message")
                                                                .value(
                                                                        FallbackThrowableErrorResolver
                                                                                .DEFAULT_PUBLIC_MESSAGE))
                                                .andExpect(
                                                        jsonPath("$.metadata.security")
                                                                .doesNotExist())));
    }

    @Test
    void returnsInvalidBearerRequest() {
        contextRunner.run(
                context ->
                        perform(
                                context,
                                mockMvc ->
                                        mockMvc.perform(
                                                        get("/authenticated")
                                                                .header(
                                                                        HttpHeaders.AUTHORIZATION,
                                                                        "Bearer jwt-invalid-request"))
                                                .andExpect(status().isUnauthorized())
                                                .andExpect(
                                                        jsonPath("$.code").value(INVALID_REQUEST))
                                                .andExpect(
                                                        jsonPath("$.metadata.oauth2")
                                                                .doesNotExist())
                                                .andExpect(
                                                        header().string(
                                                                        HttpHeaders
                                                                                .WWW_AUTHENTICATE,
                                                                        org.hamcrest.Matchers
                                                                                .containsString(
                                                                                        "error=\"invalid_request\"")))));
    }

    @Test
    void returnsInvalidTokenWithConsistentBodyAndChallenge() {
        contextRunner.run(
                context ->
                        perform(
                                context,
                                mockMvc ->
                                        mockMvc.perform(
                                                        get("/authenticated")
                                                                .header(
                                                                        HttpHeaders.AUTHORIZATION,
                                                                        "Bearer jwt-invalid"))
                                                .andExpect(status().isUnauthorized())
                                                .andExpect(jsonPath("$.code").value(INVALID_TOKEN))
                                                .andExpect(
                                                        jsonPath("$.metadata.oauth2")
                                                                .doesNotExist())
                                                .andExpect(
                                                        header().string(
                                                                        HttpHeaders
                                                                                .WWW_AUTHENTICATE,
                                                                        org.hamcrest.Matchers.allOf(
                                                                                org.hamcrest
                                                                                        .Matchers
                                                                                        .containsString(
                                                                                                "error=\"invalid_token\""),
                                                                                org.hamcrest
                                                                                        .Matchers
                                                                                        .containsString(
                                                                                                "error_description=\"The access token is invalid\""))))));
    }

    @Test
    void returnsBadGatewayForAuthenticationProviderFailure() {
        contextRunner.run(
                context ->
                        perform(
                                context,
                                mockMvc ->
                                        mockMvc.perform(
                                                        get("/authenticated")
                                                                .header(
                                                                        HttpHeaders.AUTHORIZATION,
                                                                        "Bearer opaque-provider-down"))
                                                .andExpect(status().isBadGateway())
                                                .andExpect(
                                                        jsonPath("$.code").value(PROVIDER_FAILURE))
                                                .andExpect(
                                                        jsonPath("$.metadata.category")
                                                                .value("DOWNSTREAM"))
                                                .andExpect(
                                                        jsonPath("$.metadata.security")
                                                                .doesNotExist())
                                                .andExpect(
                                                        jsonPath("$.metadata.oauth2")
                                                                .doesNotExist())
                                                .andExpect(
                                                        header().doesNotExist(
                                                                        HttpHeaders
                                                                                .WWW_AUTHENTICATE))));
    }

    @Test
    void distinguishesGenericAccessDeniedFromInsufficientScope() {
        contextRunner.run(
                context ->
                        perform(
                                context,
                                mockMvc -> {
                                    mockMvc.perform(get("/admin").with(user("user")))
                                            .andExpect(status().isForbidden())
                                            .andExpect(jsonPath("$.code").value(ACCESS_DENIED))
                                            .andExpect(
                                                    jsonPath("$.metadata.security").doesNotExist())
                                            .andExpect(jsonPath("$.metadata.oauth2").doesNotExist())
                                            .andExpect(
                                                    header().doesNotExist(
                                                                    HttpHeaders.WWW_AUTHENTICATE));

                                    mockMvc.perform(
                                                    get("/payments")
                                                            .header(
                                                                    HttpHeaders.AUTHORIZATION,
                                                                    "Bearer jwt-valid"))
                                            .andExpect(status().isForbidden())
                                            .andExpect(jsonPath("$.code").value(INSUFFICIENT_SCOPE))
                                            .andExpect(jsonPath("$.metadata.oauth2").doesNotExist())
                                            .andExpect(
                                                    header().string(
                                                                    HttpHeaders.WWW_AUTHENTICATE,
                                                                    org.hamcrest.Matchers.allOf(
                                                                            org.hamcrest.Matchers
                                                                                    .containsString(
                                                                                            "error=\"insufficient_scope\""),
                                                                            org.hamcrest.Matchers
                                                                                    .containsString(
                                                                                            "scope=\"payment.write\""))));
                                }));
    }

    @Test
    void returnsCsrfNotificationThroughTheConfiguredAccessDeniedHandler() {
        contextRunner.run(
                context ->
                        perform(
                                context,
                                mockMvc ->
                                        mockMvc.perform(post("/csrf").with(user("user")))
                                                .andExpect(status().isForbidden())
                                                .andExpect(jsonPath("$.code").value(CSRF_REJECTED))
                                                .andExpect(
                                                        jsonPath("$.metadata.security")
                                                                .doesNotExist())
                                                .andExpect(
                                                        jsonPath("$.metadata.oauth2")
                                                                .doesNotExist())));
    }

    @Test
    void supportsJwtAndOpaqueAuthenticationInTheSameResourceServer() {
        contextRunner.run(
                context ->
                        perform(
                                context,
                                mockMvc -> {
                                    TokenAuthenticationManagers managers =
                                            context.getBean(TokenAuthenticationManagers.class);

                                    mockMvc.perform(
                                                    get("/authenticated")
                                                            .header(
                                                                    HttpHeaders.AUTHORIZATION,
                                                                    "Bearer jwt-valid"))
                                            .andExpect(status().isOk())
                                            .andExpect(content().string("jwt-user"));
                                    mockMvc.perform(
                                                    get("/authenticated")
                                                            .header(
                                                                    HttpHeaders.AUTHORIZATION,
                                                                    "Bearer opaque-valid"))
                                            .andExpect(status().isOk())
                                            .andExpect(content().string("opaque-user"));

                                    assertThat(managers.jwtAuthentications()).isEqualTo(1);
                                    assertThat(managers.opaqueAuthentications()).isEqualTo(1);
                                }));
    }

    @Test
    void usesApplicationSecurityResolversInsideTheFilterChain() {
        AtomicBoolean authenticationResolverCalled = new AtomicBoolean();
        AtomicBoolean authorizationResolverCalled = new AtomicBoolean();
        AtomicBoolean requiredScopeResolverCalled = new AtomicBoolean();
        SecurityAuthenticationFailureResolver authenticationResolver =
                failureContext -> {
                    authenticationResolverCalled.set(true);
                    return new DefaultSecurityAuthenticationFailureResolver()
                            .resolve(failureContext);
                };
        RequiredScopeResolver requiredScopeResolver =
                (request, authentication) -> {
                    requiredScopeResolverCalled.set(true);
                    return Set.of("orders.approve");
                };
        SecurityAuthorizationFailureResolver authorizationResolver =
                failureContext -> {
                    authorizationResolverCalled.set(true);
                    return new DefaultSecurityAuthorizationFailureResolver()
                            .resolve(failureContext);
                };

        contextRunner
                .withBean(SecurityAuthenticationFailureResolver.class, () -> authenticationResolver)
                .withBean(SecurityAuthorizationFailureResolver.class, () -> authorizationResolver)
                .withBean(RequiredScopeResolver.class, () -> requiredScopeResolver)
                .run(
                        context ->
                                perform(
                                        context,
                                        mockMvc -> {
                                            mockMvc.perform(
                                                            get("/authenticated")
                                                                    .header(
                                                                            HttpHeaders
                                                                                    .AUTHORIZATION,
                                                                            "Bearer jwt-invalid"))
                                                    .andExpect(status().isUnauthorized())
                                                    .andExpect(
                                                            jsonPath("$.code")
                                                                    .value(INVALID_TOKEN));
                                            mockMvc.perform(
                                                            get("/payments")
                                                                    .header(
                                                                            HttpHeaders
                                                                                    .AUTHORIZATION,
                                                                            "Bearer jwt-valid"))
                                                    .andExpect(status().isForbidden())
                                                    .andExpect(
                                                            jsonPath("$.metadata.oauth2")
                                                                    .doesNotExist())
                                                    .andExpect(
                                                            header().string(
                                                                            HttpHeaders
                                                                                    .WWW_AUTHENTICATE,
                                                                            org.hamcrest.Matchers
                                                                                    .containsString(
                                                                                            "scope=\"orders.approve\"")));

                                            assertThat(
                                                            context.getBean(
                                                                    SecurityAuthenticationFailureResolver
                                                                            .class))
                                                    .isSameAs(authenticationResolver);
                                            assertThat(
                                                            context.getBean(
                                                                    SecurityAuthorizationFailureResolver
                                                                            .class))
                                                    .isSameAs(authorizationResolver);
                                            assertThat(context.getBean(RequiredScopeResolver.class))
                                                    .isSameAs(requiredScopeResolver);
                                            assertThat(authenticationResolverCalled).isTrue();
                                            assertThat(authorizationResolverCalled).isTrue();
                                            assertThat(requiredScopeResolverCalled).isTrue();
                                        }));
    }

    @Test
    void internalExposureReturnsDetailedSanitizedSecurityBodies() {
        internalContextRunner.run(
                context ->
                        perform(
                                context,
                                mockMvc -> {
                                    mockMvc.perform(get("/authenticated"))
                                            .andExpect(status().isUnauthorized())
                                            .andExpect(
                                                    jsonPath("$.code")
                                                            .value(AUTHENTICATION_REQUIRED))
                                            .andExpect(
                                                    jsonPath("$.metadata.category")
                                                            .value("AUTHENTICATION"))
                                            .andExpect(
                                                    jsonPath("$.metadata.security.reason")
                                                            .value("authentication_required"));

                                    mockMvc.perform(
                                                    get("/payments")
                                                            .header(
                                                                    HttpHeaders.AUTHORIZATION,
                                                                    "Bearer jwt-valid"))
                                            .andExpect(status().isForbidden())
                                            .andExpect(jsonPath("$.code").value(INSUFFICIENT_SCOPE))
                                            .andExpect(
                                                    jsonPath("$.metadata.category")
                                                            .value("AUTHORIZATION"))
                                            .andExpect(
                                                    jsonPath("$.metadata.oauth2.error")
                                                            .value("insufficient_scope"))
                                            .andExpect(
                                                    jsonPath("$.metadata.oauth2.scope")
                                                            .value("payment.write"))
                                            .andExpect(
                                                    header().string(
                                                                    HttpHeaders.WWW_AUTHENTICATE,
                                                                    org.hamcrest.Matchers
                                                                            .containsString(
                                                                                    "error=\"insufficient_scope\"")));
                                }));
    }

    private static WebApplicationContextRunner contextRunner(ErrorExposure exposure) {
        return new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(ErrorHandlingAutoConfiguration.class))
                .withUserConfiguration(SecurityTestConfiguration.class)
                .withPropertyValues(
                        "smbtech.error-handling.response.exposure=" + exposure.name(),
                        "smbtech.error-handling.security.oauth2-metadata.include-required-scope=true");
    }

    private static void perform(WebApplicationContext context, MockMvcOperation operation) {
        try {
            operation.run(
                    MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build());
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }

    @FunctionalInterface
    private interface MockMvcOperation {
        void run(MockMvc mockMvc) throws Exception;
    }

    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    @EnableWebSecurity
    static class SecurityTestConfiguration {

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        SecurityController securityController() {
            return new SecurityController();
        }

        @Bean
        TokenAuthenticationManagers tokenAuthenticationManagers() {
            return new TokenAuthenticationManagers();
        }

        @Bean
        AuthenticationManagerResolver<HttpServletRequest> authenticationManagerResolver(
                TokenAuthenticationManagers managers) {
            return request ->
                    managers.forAuthorization(request.getHeader(HttpHeaders.AUTHORIZATION));
        }

        @Bean
        @ConditionalOnMissingBean
        RequiredScopeResolver requiredScopeResolver() {
            return (request, authentication) ->
                    "/payments".equals(request.getRequestURI())
                            ? Set.of("payment.write")
                            : Set.of();
        }

        @Bean
        SecurityFilterChain securityFilterChain(
                HttpSecurity http,
                AuthenticationManagerResolver<HttpServletRequest> authenticationManagerResolver,
                SecurityAuthenticationEntryPoint authenticationEntryPoint,
                SecurityAccessDeniedHandler accessDeniedHandler)
                throws Exception {
            http.authorizeHttpRequests(
                            authorize ->
                                    authorize
                                            .requestMatchers("/admin")
                                            .hasRole("ADMIN")
                                            .requestMatchers("/payments")
                                            .hasAuthority("SCOPE_payment.write")
                                            .anyRequest()
                                            .authenticated())
                    .exceptionHandling(
                            exceptions ->
                                    exceptions
                                            .authenticationEntryPoint(authenticationEntryPoint)
                                            .accessDeniedHandler(accessDeniedHandler))
                    .oauth2ResourceServer(
                            resourceServer ->
                                    resourceServer
                                            .authenticationManagerResolver(
                                                    authenticationManagerResolver)
                                            .authenticationEntryPoint(authenticationEntryPoint)
                                            .accessDeniedHandler(accessDeniedHandler));
            return http.build();
        }
    }

    @RestController
    static class SecurityController {

        @GetMapping("/authenticated")
        String authenticated(Authentication authentication) {
            return authentication.getName();
        }

        @GetMapping("/admin")
        String admin() {
            return "admin";
        }

        @GetMapping("/payments")
        String payments() {
            return "payments";
        }

        @PostMapping("/csrf")
        String csrf() {
            return "csrf";
        }
    }

    static final class TokenAuthenticationManagers {

        private static final Collection<? extends GrantedAuthority> READ_AUTHORITY =
                List.of(new SimpleGrantedAuthority("SCOPE_payment.read"));

        private final AtomicInteger jwtAuthentications = new AtomicInteger();
        private final AtomicInteger opaqueAuthentications = new AtomicInteger();

        AuthenticationManager forAuthorization(String authorization) {
            return authorization != null && authorization.startsWith("Bearer opaque-")
                    ? this::authenticateOpaque
                    : this::authenticateJwt;
        }

        int jwtAuthentications() {
            return jwtAuthentications.get();
        }

        int opaqueAuthentications() {
            return opaqueAuthentications.get();
        }

        private Authentication authenticateJwt(Authentication authentication) {
            String token = token(authentication);
            jwtAuthentications.incrementAndGet();
            if ("jwt-invalid-request".equals(token)) {
                throw new OAuth2AuthenticationException(
                        BearerTokenErrors.invalidRequest("Malformed request"));
            }
            if ("jwt-invalid".equals(token)) {
                throw new OAuth2AuthenticationException(
                        BearerTokenErrors.invalidToken("Rejected token"));
            }
            Instant now = Instant.now();
            Jwt jwt =
                    new Jwt(
                            token,
                            now,
                            now.plusSeconds(300),
                            Map.of("alg", "RS256"),
                            Map.of("sub", "jwt-user"));
            return new JwtAuthenticationToken(jwt, READ_AUTHORITY, "jwt-user");
        }

        private Authentication authenticateOpaque(Authentication authentication) {
            String token = token(authentication);
            opaqueAuthentications.incrementAndGet();
            if ("opaque-provider-down".equals(token)) {
                throw new OAuth2AuthenticationException(
                        new OAuth2Error(
                                OAuth2ErrorCodes.TEMPORARILY_UNAVAILABLE,
                                "provider-secret",
                                "https://internal.example/provider"));
            }
            Instant now = Instant.now();
            DefaultOAuth2AuthenticatedPrincipal principal =
                    new DefaultOAuth2AuthenticatedPrincipal(
                            "opaque-user",
                            Map.of("sub", "opaque-user"),
                            List.copyOf(READ_AUTHORITY));
            OAuth2AccessToken accessToken =
                    new OAuth2AccessToken(
                            OAuth2AccessToken.TokenType.BEARER,
                            token,
                            now,
                            now.plusSeconds(300),
                            Set.of("payment.read"));
            return new BearerTokenAuthentication(principal, accessToken, READ_AUTHORITY);
        }

        private static String token(Authentication authentication) {
            if (!(authentication instanceof BearerTokenAuthenticationToken bearerToken)) {
                throw new IllegalArgumentException("Expected BearerTokenAuthenticationToken");
            }
            return bearerToken.getToken();
        }
    }
}
