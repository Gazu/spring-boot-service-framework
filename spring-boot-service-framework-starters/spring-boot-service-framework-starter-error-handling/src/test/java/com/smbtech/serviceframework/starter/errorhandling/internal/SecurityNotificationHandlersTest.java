package com.smbtech.serviceframework.starter.errorhandling.internal;

import static com.smbtech.serviceframework.starter.errorhandling.internal.ErrorHandlingWebTestFixtures.responseFactory;
import static com.smbtech.serviceframework.starter.errorhandling.serialization.ErrorHandlingSerializationTestFixtures.responseWriter;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.smbtech.serviceframework.commons.notification.Notification;
import com.smbtech.serviceframework.error.ErrorExposure;
import com.smbtech.serviceframework.error.ThrowableErrorResolver;
import com.smbtech.serviceframework.starter.errorhandling.api.ErrorReporter;
import com.smbtech.serviceframework.starter.errorhandling.api.NotificationResponseFactory;
import com.smbtech.serviceframework.starter.errorhandling.api.NotificationResponseWriter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class SecurityNotificationHandlersTest {

    @Test
    void authenticationEntryPointWritesSnakeCaseNotificationWithoutDiagnostics() throws Exception {
        ObjectMapper applicationMapper = new ObjectMapper();
        var originalModules = applicationMapper.registeredModules();
        SecurityAuthenticationEntryPoint entryPoint =
                new SecurityAuthenticationEntryPoint(
                        responseFactory(), responseWriter(applicationMapper));
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(
                new MockHttpServletRequest(),
                response,
                new BadCredentialsException("Bearer security-token password=security-secret"));

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentType().startsWith("application/json"));
        JsonNode json = applicationMapper.readTree(response.getContentAsByteArray());
        assertEquals(SecurityAuthenticationEntryPoint.ERROR_CODE, json.get("code").asText());
        assertEquals(ThrowableErrorResolver.DEFAULT_PUBLIC_MESSAGE, json.get("message").asText());
        assertEquals("", json.get("field_name").asText());
        assertEquals(Map.of("category", "AUTHENTICATION"), mapperValue(json.get("metadata")));
        assertFalse(json.has("fieldName"));
        assertFalse(response.getContentAsString().contains("security-token"));
        assertFalse(response.getContentAsString().contains("security-secret"));

        assertEquals(originalModules, applicationMapper.registeredModules());
    }

    @Test
    void accessDeniedHandlerWritesTheSameNotificationContract() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        SecurityAccessDeniedHandler deniedHandler =
                new SecurityAccessDeniedHandler(responseFactory(), responseWriter(mapper));
        MockHttpServletResponse response = new MockHttpServletResponse();

        deniedHandler.handle(
                new MockHttpServletRequest(),
                response,
                new AccessDeniedException("scope payment.write denied for token=secret"));

        assertEquals(403, response.getStatus());
        JsonNode json = mapper.readTree(response.getContentAsByteArray());
        assertEquals(SecurityAccessDeniedHandler.ERROR_CODE, json.get("code").asText());
        assertEquals(ThrowableErrorResolver.DEFAULT_PUBLIC_MESSAGE, json.get("message").asText());
        assertEquals(Map.of("category", "AUTHORIZATION"), mapperValue(json.get("metadata")));
        assertEquals(
                Set.of("code", "message", "severity", "field_name", "metadata", "id", "timestamp"),
                json.properties().stream()
                        .map(java.util.Map.Entry::getKey)
                        .collect(java.util.stream.Collectors.toSet()));
        assertFalse(response.getContentAsString().contains("payment.write"));
        assertFalse(response.getContentAsString().contains("secret"));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> mapperValue(JsonNode value) {
        return new ObjectMapper().convertValue(value, Map.class);
    }

    @Test
    void doesNotWriteWhenResponseIsAlreadyCommitted() throws Exception {
        SecurityAuthenticationEntryPoint entryPoint =
                new SecurityAuthenticationEntryPoint(
                        responseFactory(), responseWriter(new ObjectMapper()));
        SecurityAccessDeniedHandler deniedHandler =
                new SecurityAccessDeniedHandler(
                        responseFactory(), responseWriter(new ObjectMapper()));
        MockHttpServletResponse authenticationResponse = new MockHttpServletResponse();
        authenticationResponse.setCommitted(true);
        MockHttpServletResponse authorizationResponse = new MockHttpServletResponse();
        authorizationResponse.setCommitted(true);

        entryPoint.commence(
                new MockHttpServletRequest(),
                authenticationResponse,
                new BadCredentialsException("failure"));
        deniedHandler.handle(
                new MockHttpServletRequest(),
                authorizationResponse,
                new AccessDeniedException("failure"));

        assertEquals(0, authenticationResponse.getContentAsByteArray().length);
        assertEquals(0, authorizationResponse.getContentAsByteArray().length);
    }

    @Test
    void usesReplaceableResponseFactoryAndWriter() throws Exception {
        Notification replacement =
                Notification.warning("W_SECURITY_CUSTOM", "Custom security response");
        ResponseEntity<Notification> replacementResponse =
                ResponseEntity.status(499).body(replacement);
        NotificationResponseFactory factory = resolvedError -> replacementResponse;
        AtomicReference<ResponseEntity<Notification>> writtenResponse = new AtomicReference<>();
        NotificationResponseWriter writer =
                (responseEntity, servletResponse) -> writtenResponse.set(responseEntity);
        SecurityAuthenticationEntryPoint entryPoint =
                new SecurityAuthenticationEntryPoint(factory, writer);
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();

        entryPoint.commence(
                new MockHttpServletRequest(),
                servletResponse,
                new BadCredentialsException("failure"));

        assertSame(replacementResponse, writtenResponse.get());
        assertEquals(499, servletResponse.getStatus());
    }

    @Test
    void sanitizesSecurityResponseAfterApplicationCustomizers() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        ErrorCustomizationPipeline customizationPipeline =
                new ErrorCustomizationPipeline(
                        List.of(),
                        List.of(
                                (response, resolvedError, request) ->
                                        ResponseEntity.status(response.getStatusCode())
                                                .body(
                                                        Notification.error(
                                                                "E_CUSTOM_SECURITY",
                                                                "Bearer customizer-token was rejected"))),
                        resolvedError -> ErrorExposure.INTERNAL);
        SecurityAuthenticationEntryPoint entryPoint =
                new SecurityAuthenticationEntryPoint(
                        responseFactory(),
                        responseWriter(mapper),
                        ErrorReporter.noop(),
                        (resolvedError, statusCode) -> {},
                        customizationPipeline);
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(
                new MockHttpServletRequest(),
                response,
                new BadCredentialsException("Bearer original-token was rejected"));

        JsonNode json = mapper.readTree(response.getContentAsByteArray());
        assertEquals("E_CUSTOM_SECURITY", json.get("code").asText());
        assertEquals("Bearer <redacted> was rejected", json.get("message").asText());
        assertFalse(response.getContentAsString().contains("customizer-token"));
        assertFalse(response.getContentAsString().contains("original-token"));
    }

    @Test
    void reportsSecurityFailuresWithTheResolvedErrorAndRequest() throws Exception {
        NotificationResponseFactory factory = responseFactory();
        NotificationResponseWriter writer = responseWriter(new ObjectMapper());
        AtomicReference<String> reportedCode = new AtomicReference<>();
        AtomicReference<String> reportedPath = new AtomicReference<>();
        SecurityAuthenticationEntryPoint entryPoint =
                new SecurityAuthenticationEntryPoint(
                        factory,
                        writer,
                        (cause, resolvedError, request) -> {
                            reportedCode.set(resolvedError.notification().code());
                            reportedPath.set(request.getRequestURI());
                        });
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/payments");

        entryPoint.commence(
                request, new MockHttpServletResponse(), new BadCredentialsException("failure"));

        assertEquals(SecurityAuthenticationEntryPoint.ERROR_CODE, reportedCode.get());
        assertEquals("/payments", reportedPath.get());
    }

    @Test
    void recordsSecurityResponseMetricsAfterWriting() throws Exception {
        NotificationResponseFactory factory = responseFactory();
        NotificationResponseWriter writer = responseWriter(new ObjectMapper());
        AtomicReference<String> code = new AtomicReference<>();
        AtomicInteger status = new AtomicInteger();
        SecurityAccessDeniedHandler deniedHandler =
                new SecurityAccessDeniedHandler(
                        factory,
                        writer,
                        ErrorReporter.noop(),
                        (resolvedError, statusCode) -> {
                            code.set(resolvedError.notification().code());
                            status.set(statusCode);
                        });

        deniedHandler.handle(
                new MockHttpServletRequest("GET", "/payments"),
                new MockHttpServletResponse(),
                new AccessDeniedException("failure"));

        assertEquals(SecurityAccessDeniedHandler.ERROR_CODE, code.get());
        assertEquals(403, status.get());
    }

    @Test
    void rejectsMissingDependenciesAndArguments() {
        NotificationResponseFactory factory = responseFactory();
        NotificationResponseWriter writer = responseWriter(new ObjectMapper());
        SecurityAuthenticationEntryPoint entryPoint =
                new SecurityAuthenticationEntryPoint(factory, writer);
        SecurityAccessDeniedHandler deniedHandler =
                new SecurityAccessDeniedHandler(factory, writer);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThrows(
                NullPointerException.class,
                () -> new SecurityAuthenticationEntryPoint(null, writer));
        assertThrows(
                NullPointerException.class,
                () -> new SecurityAuthenticationEntryPoint(factory, null));
        assertThrows(
                NullPointerException.class, () -> new SecurityAccessDeniedHandler(null, writer));
        assertThrows(
                NullPointerException.class, () -> new SecurityAccessDeniedHandler(factory, null));
        assertThrows(
                NullPointerException.class,
                () ->
                        new SecurityAuthenticationEntryPoint(
                                factory, writer, ErrorReporter.noop(), null));
        assertThrows(
                NullPointerException.class,
                () -> new SecurityAccessDeniedHandler(factory, writer, ErrorReporter.noop(), null));
        assertThrows(
                NullPointerException.class,
                () -> entryPoint.commence(null, response, new BadCredentialsException("failure")));
        assertThrows(
                NullPointerException.class,
                () -> deniedHandler.handle(request, null, new AccessDeniedException("failure")));
    }
}
