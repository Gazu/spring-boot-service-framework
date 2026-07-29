package com.smbtech.serviceframework.starter.errorhandling.adapter.in.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.smbtech.serviceframework.error.FallbackThrowableErrorResolver;
import com.smbtech.serviceframework.starter.errorhandling.adapter.in.web.DefaultNotificationResponseFactory;
import com.smbtech.serviceframework.starter.errorhandling.serialization.NotificationJsonResponseWriter;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class SecurityHandlersCompatibilityTest {

    private static final Set<String> NOTIFICATION_FIELDS =
            Set.of("code", "message", "severity", "field_name", "metadata", "id", "timestamp");

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final NotificationJsonResponseWriter responseWriter =
            new NotificationJsonResponseWriter(objectMapper);

    @Test
    void preservesAuthenticationResponseContract() throws Exception {
        SecurityAuthenticationEntryPoint entryPoint =
                new SecurityAuthenticationEntryPoint(
                        new DefaultNotificationResponseFactory(), responseWriter);
        MockHttpServletResponse response = new MockHttpServletResponse();
        BadCredentialsException failure =
                new BadCredentialsException(
                        "Bearer eyJhbGciOiJSUzI1NiJ9.payload.signature "
                                + "password=authentication-secret root_cause=provider.internal");

        entryPoint.commence(new MockHttpServletRequest("GET", "/secure"), response, failure);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).startsWith("application/json");
        assertNotification(
                response, "E_SERVICE_FRAMEWORK_SECURITY_AUTHENTICATION_0001", "AUTHENTICATION");
        assertThat(response.getContentAsString())
                .doesNotContain(
                        failure.getMessage(),
                        "eyJhbGciOiJSUzI1NiJ9",
                        "authentication-secret",
                        "provider.internal",
                        "BadCredentialsException");
    }

    @Test
    void preservesAuthorizationResponseContract() throws Exception {
        SecurityAccessDeniedHandler deniedHandler =
                new SecurityAccessDeniedHandler(
                        new DefaultNotificationResponseFactory(), responseWriter);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AccessDeniedException failure =
                new AccessDeniedException(
                        "scope=payment.write authorization=Bearer authorization-secret "
                                + "root_cause=authorization.internal");

        deniedHandler.handle(new MockHttpServletRequest("POST", "/admin"), response, failure);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).startsWith("application/json");
        assertNotification(
                response, "E_SERVICE_FRAMEWORK_SECURITY_AUTHORIZATION_0001", "AUTHORIZATION");
        assertThat(response.getContentAsString())
                .doesNotContain(
                        failure.getMessage(),
                        "payment.write",
                        "authorization-secret",
                        "authorization.internal",
                        "AccessDeniedException");
    }

    private void assertNotification(
            MockHttpServletResponse response, String expectedCode, String expectedCategory)
            throws Exception {
        JsonNode json = objectMapper.readTree(response.getContentAsByteArray());

        assertThat(
                        json.properties().stream()
                                .map(java.util.Map.Entry::getKey)
                                .collect(Collectors.toSet()))
                .containsExactlyInAnyOrderElementsOf(NOTIFICATION_FIELDS);
        assertThat(json.path("code").asText()).isEqualTo(expectedCode);
        assertThat(json.path("message").asText())
                .isEqualTo(FallbackThrowableErrorResolver.DEFAULT_PUBLIC_MESSAGE);
        assertThat(json.path("severity").asText()).isEqualTo("ERROR");
        assertThat(json.path("field_name").asText()).isEmpty();
        assertThat(json.path("metadata").isObject()).isTrue();
        assertThat(json.at("/metadata/category").asText()).isEqualTo(expectedCategory);
        assertThat(json.path("metadata").size()).isEqualTo(1);
        assertThatCode(() -> UUID.fromString(json.path("id").asText())).doesNotThrowAnyException();
        assertThatCode(() -> Instant.parse(json.path("timestamp").asText()))
                .doesNotThrowAnyException();
        assertThat(json.has("fieldName")).isFalse();
    }
}
