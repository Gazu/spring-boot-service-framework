package com.smbtech.serviceframework.starter.errorhandling.adapter.in.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smbtech.serviceframework.commons.notification.Notification;
import com.smbtech.serviceframework.starter.errorhandling.adapter.in.web.DefaultNotificationResponseFactory;
import com.smbtech.serviceframework.starter.errorhandling.api.ErrorReporter;
import com.smbtech.serviceframework.starter.errorhandling.api.NotificationResponseFactory;
import com.smbtech.serviceframework.starter.errorhandling.api.NotificationResponseWriter;
import com.smbtech.serviceframework.starter.errorhandling.serialization.NotificationJsonResponseWriter;
import com.smbtech.serviceframework.starter.errorhandling.serialization.NotificationJsonSerializer;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

class SecurityNotificationHandlersTest {

    @Test
    void authenticationEntryPointWritesSnakeCaseNotificationWithoutDiagnostics() throws Exception {
        ObjectMapper applicationMapper = new ObjectMapper();
        JsonSerializer<?> originalSerializer =
                applicationMapper
                        .getSerializerProviderInstance()
                        .findValueSerializer(Notification.class);
        SecurityAuthenticationEntryPoint entryPoint =
                new SecurityAuthenticationEntryPoint(
                        new DefaultNotificationResponseFactory(),
                        new NotificationJsonResponseWriter(applicationMapper));
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(
                new MockHttpServletRequest(),
                response,
                new BadCredentialsException("Bearer security-token password=security-secret"));

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentType().startsWith("application/json"));
        JsonNode json = applicationMapper.readTree(response.getContentAsByteArray());
        assertEquals(SecurityAuthenticationEntryPoint.ERROR_CODE, json.get("code").asText());
        assertEquals(SecurityAuthenticationEntryPoint.PUBLIC_MESSAGE, json.get("message").asText());
        assertEquals("", json.get("field_name").asText());
        assertTrue(json.get("metadata").isObject());
        assertFalse(json.has("fieldName"));
        assertFalse(response.getContentAsString().contains("security-token"));
        assertFalse(response.getContentAsString().contains("security-secret"));

        JsonSerializer<?> unchangedSerializer =
                applicationMapper
                        .getSerializerProviderInstance()
                        .findValueSerializer(Notification.class);
        assertSame(originalSerializer, unchangedSerializer);
        assertFalse(unchangedSerializer instanceof NotificationJsonSerializer);
    }

    @Test
    void accessDeniedHandlerWritesTheSameNotificationContract() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        SecurityAccessDeniedHandler deniedHandler =
                new SecurityAccessDeniedHandler(
                        new DefaultNotificationResponseFactory(),
                        new NotificationJsonResponseWriter(mapper));
        MockHttpServletResponse response = new MockHttpServletResponse();

        deniedHandler.handle(
                new MockHttpServletRequest(),
                response,
                new AccessDeniedException("scope payment.write denied for token=secret"));

        assertEquals(403, response.getStatus());
        JsonNode json = mapper.readTree(response.getContentAsByteArray());
        assertEquals(SecurityAccessDeniedHandler.ERROR_CODE, json.get("code").asText());
        assertEquals(SecurityAccessDeniedHandler.PUBLIC_MESSAGE, json.get("message").asText());
        assertEquals(
                Set.of("code", "message", "severity", "field_name", "metadata", "id", "timestamp"),
                json.properties().stream()
                        .map(java.util.Map.Entry::getKey)
                        .collect(java.util.stream.Collectors.toSet()));
        assertFalse(response.getContentAsString().contains("payment.write"));
        assertFalse(response.getContentAsString().contains("secret"));
    }

    @Test
    void doesNotWriteWhenResponseIsAlreadyCommitted() throws Exception {
        SecurityAuthenticationEntryPoint entryPoint =
                new SecurityAuthenticationEntryPoint(
                        new DefaultNotificationResponseFactory(),
                        new NotificationJsonResponseWriter(new ObjectMapper()));
        SecurityAccessDeniedHandler deniedHandler =
                new SecurityAccessDeniedHandler(
                        new DefaultNotificationResponseFactory(),
                        new NotificationJsonResponseWriter(new ObjectMapper()));
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
    void reportsSecurityFailuresWithTheResolvedErrorAndRequest() throws Exception {
        NotificationResponseFactory factory = new DefaultNotificationResponseFactory();
        NotificationResponseWriter writer = new NotificationJsonResponseWriter(new ObjectMapper());
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
        NotificationResponseFactory factory = new DefaultNotificationResponseFactory();
        NotificationResponseWriter writer = new NotificationJsonResponseWriter(new ObjectMapper());
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
        NotificationResponseFactory factory = new DefaultNotificationResponseFactory();
        NotificationResponseWriter writer = new NotificationJsonResponseWriter(new ObjectMapper());
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
