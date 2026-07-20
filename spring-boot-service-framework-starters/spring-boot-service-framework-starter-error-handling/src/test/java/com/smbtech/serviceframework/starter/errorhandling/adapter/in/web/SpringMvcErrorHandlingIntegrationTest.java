package com.smbtech.serviceframework.starter.errorhandling.adapter.in.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smbtech.serviceframework.commons.notification.Notification;
import com.smbtech.serviceframework.error.ErrorCategory;
import com.smbtech.serviceframework.error.ResolvedError;
import com.smbtech.serviceframework.error.ServiceExceptionThrowableErrorResolver;
import com.smbtech.serviceframework.error.ThrowableErrorResolutionPipeline;
import com.smbtech.serviceframework.starter.errorhandling.api.ErrorReporter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.resource.NoResourceFoundException;

class SpringMvcErrorHandlingIntegrationTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ObjectMapper applicationMapper = new ObjectMapper();
        NotificationHttpMessageConverter notificationConverter =
                new NotificationHttpMessageConverter(applicationMapper);
        ThrowableErrorResolutionPipeline pipeline =
                new ThrowableErrorResolutionPipeline(
                        List.of(
                                new ServiceExceptionThrowableErrorResolver(),
                                new ValidationExceptionResolver(),
                                new SpringMvcExceptionResolver()));
        ServiceFrameworkExceptionHandler exceptionHandler =
                new ServiceFrameworkExceptionHandler(
                        pipeline, new DefaultNotificationResponseFactory());
        mockMvc =
                MockMvcBuilders.standaloneSetup(new TestController())
                        .setControllerAdvice(exceptionHandler)
                        .setMessageConverters(
                                notificationConverter, new JacksonJsonHttpMessageConverter())
                        .build();
    }

    @Test
    void handlesBeanValidationWithSnakeCaseFieldViolations() throws Exception {
        mockMvc.perform(
                        post("/customers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content("{\"customerId\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(
                        jsonPath("$.code").value(ValidationExceptionResolver.VALIDATION_ERROR_CODE))
                .andExpect(jsonPath("$.field_name").value(""))
                .andExpect(jsonPath("$.metadata.schema_version").value("1"))
                .andExpect(jsonPath("$.metadata.category").value("VALIDATION"))
                .andExpect(jsonPath("$.metadata.request.route").value("/customers"))
                .andExpect(jsonPath("$.metadata.validation.type").value("bean_validation"))
                .andExpect(jsonPath("$.metadata.violations[0].field_name").value("customerId"))
                .andExpect(jsonPath("$.metadata.violations[0].code").exists())
                .andExpect(jsonPath("$.fieldName").doesNotExist());
    }

    @Test
    void handlesRequestParameterTypeBinding() throws Exception {
        mockMvc.perform(get("/parameters").param("limit", "not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ValidationExceptionResolver.BINDING_ERROR_CODE))
                .andExpect(jsonPath("$.metadata.violations[0].field_name").value("limit"))
                .andExpect(jsonPath("$.metadata.violations[0].code").value("type_mismatch"));
    }

    @Test
    void handlesMalformedJson() throws Exception {
        mockMvc.perform(
                        post("/customers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"customerId\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("E_SERVICE_FRAMEWORK_JSON_0001"))
                .andExpect(jsonPath("$.message").value("Request body is invalid"))
                .andExpect(jsonPath("$.metadata.validation.type").value("malformed_json"));
    }

    @Test
    void handlesMissingHeadersAndParameters() throws Exception {
        mockMvc.perform(get("/headers"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("E_SERVICE_FRAMEWORK_HEADER_0001"))
                .andExpect(jsonPath("$.metadata.violations[0].field_name").value("X-Customer-Id"));

        mockMvc.perform(get("/parameters"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("E_SERVICE_FRAMEWORK_PARAMETER_0001"))
                .andExpect(jsonPath("$.metadata.violations[0].field_name").value("limit"));
    }

    @Test
    void handlesUnsupportedMethodsAndMediaTypes() throws Exception {
        mockMvc.perform(post("/headers"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("E_SERVICE_FRAMEWORK_METHOD_0001"))
                .andExpect(jsonPath("$.metadata.category").value("METHOD_NOT_ALLOWED"))
                .andExpect(jsonPath("$.metadata.http.method").value("POST"))
                .andExpect(jsonPath("$.metadata.http.allowed_methods[0]").value("GET"));

        mockMvc.perform(
                        post("/customers")
                                .contentType(MediaType.TEXT_PLAIN)
                                .content("customerId=123"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("E_SERVICE_FRAMEWORK_MEDIA_TYPE_0001"));

        mockMvc.perform(
                        post("/customers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_XML)
                                .content("{\"customerId\":\"123\"}"))
                .andExpect(status().isNotAcceptable())
                .andExpect(jsonPath("$.code").value("E_SERVICE_FRAMEWORK_MEDIA_TYPE_0002"));
    }

    @Test
    void resolvesMissingRoutes() {
        SpringMvcExceptionResolver resolver = new SpringMvcExceptionResolver();
        ResolvedError resolvedError =
                resolver.resolve(
                        new NoResourceFoundException(
                                HttpMethod.GET, "/missing", "No static resource"));

        ResponseEntity<Notification> response =
                new DefaultNotificationResponseFactory().create(resolvedError);

        assertEquals(404, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("E_SERVICE_FRAMEWORK_ROUTE_0001", response.getBody().code());
    }

    @Test
    void usesSafeFallbackForUnexpectedExceptions() throws Exception {
        mockMvc.perform(get("/failure"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("E_SERVICE_FRAMEWORK_INTERNAL_0001"))
                .andExpect(jsonPath("$.message").value("The request could not be completed"))
                .andExpect(jsonPath("$.metadata.schema_version").value("1"))
                .andExpect(jsonPath("$.metadata.category").value("INTERNAL"))
                .andExpect(jsonPath("$.metadata.request.route").value("/failure"))
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.not(
                                                org.hamcrest.Matchers.containsString(
                                                        "database-password"))));
    }

    @Test
    void rejectsInvalidHandlerDependenciesAndUnsupportedResolvers() {
        ThrowableErrorResolutionPipeline pipeline = new ThrowableErrorResolutionPipeline(List.of());
        DefaultNotificationResponseFactory factory = new DefaultNotificationResponseFactory();

        assertThrows(
                NullPointerException.class,
                () -> new ServiceFrameworkExceptionHandler(null, factory));
        assertThrows(
                NullPointerException.class,
                () -> new ServiceFrameworkExceptionHandler(pipeline, null));
        assertThrows(
                NullPointerException.class,
                () -> new ServiceFrameworkExceptionHandler(pipeline, factory, null));
        assertThrows(
                NullPointerException.class,
                () ->
                        new ServiceFrameworkExceptionHandler(
                                pipeline, factory, ErrorReporter.noop(), null));
        assertThrows(
                IllegalArgumentException.class,
                () -> new SpringMvcExceptionResolver().resolve(new IllegalStateException()));
        assertThrows(
                IllegalArgumentException.class,
                () -> new ValidationExceptionResolver().resolve(new IllegalStateException()));
    }

    @Test
    void reportingFailureDoesNotReplaceTheNotificationResponse() {
        ThrowableErrorResolutionPipeline pipeline = new ThrowableErrorResolutionPipeline(List.of());
        ServiceFrameworkExceptionHandler handler =
                new ServiceFrameworkExceptionHandler(
                        pipeline,
                        new DefaultNotificationResponseFactory(),
                        (cause, resolvedError, request) -> {
                            throw new IllegalStateException("reporting failed");
                        });

        ResponseEntity<Notification> response =
                handler.handleException(
                        new IllegalStateException("application failed"),
                        new MockHttpServletRequest("GET", "/failure"));

        assertEquals(500, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("E_SERVICE_FRAMEWORK_INTERNAL_0001", response.getBody().code());
    }

    @Test
    void recordsResolvedCodeCategoryAndResponseStatus() {
        ThrowableErrorResolutionPipeline pipeline = new ThrowableErrorResolutionPipeline(List.of());
        AtomicReference<ResolvedError> recordedError = new AtomicReference<>();
        AtomicInteger recordedStatus = new AtomicInteger();
        ServiceFrameworkExceptionHandler handler =
                new ServiceFrameworkExceptionHandler(
                        pipeline,
                        new DefaultNotificationResponseFactory(),
                        ErrorReporter.noop(),
                        (resolvedError, statusCode) -> {
                            recordedError.set(resolvedError);
                            recordedStatus.set(statusCode);
                        });

        handler.handleException(
                new IllegalStateException("application failed"),
                new MockHttpServletRequest("GET", "/failure"));

        assertEquals(
                "E_SERVICE_FRAMEWORK_INTERNAL_0001", recordedError.get().notification().code());
        assertEquals(ErrorCategory.INTERNAL, recordedError.get().category());
        assertEquals(500, recordedStatus.get());
    }

    @Test
    void metricsFailureDoesNotReplaceTheNotificationResponse() {
        ThrowableErrorResolutionPipeline pipeline = new ThrowableErrorResolutionPipeline(List.of());
        ServiceFrameworkExceptionHandler handler =
                new ServiceFrameworkExceptionHandler(
                        pipeline,
                        new DefaultNotificationResponseFactory(),
                        ErrorReporter.noop(),
                        (resolvedError, statusCode) -> {
                            throw new IllegalStateException("metrics failed");
                        });

        ResponseEntity<Notification> response =
                handler.handleException(
                        new IllegalStateException("application failed"),
                        new MockHttpServletRequest("GET", "/failure"));

        assertEquals(500, response.getStatusCode().value());
    }

    @RestController
    static class TestController {

        @PostMapping(
                value = "/customers",
                consumes = MediaType.APPLICATION_JSON_VALUE,
                produces = MediaType.APPLICATION_JSON_VALUE)
        Map<String, String> create(@Valid @RequestBody CustomerRequest request) {
            return Map.of("customerId", request.customerId());
        }

        @GetMapping("/headers")
        String header(@RequestHeader("X-Customer-Id") String customerId) {
            return customerId;
        }

        @GetMapping("/parameters")
        int parameter(@RequestParam("limit") int limit) {
            return limit;
        }

        @GetMapping("/failure")
        String failure() {
            throw new IllegalStateException("database-password must not be exposed");
        }
    }

    record CustomerRequest(@NotBlank String customerId) {}
}
