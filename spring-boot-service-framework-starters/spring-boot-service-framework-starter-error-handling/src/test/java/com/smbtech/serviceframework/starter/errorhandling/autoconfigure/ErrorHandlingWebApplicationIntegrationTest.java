package com.smbtech.serviceframework.starter.errorhandling.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.smbtech.serviceframework.commons.notification.Notification;
import com.smbtech.serviceframework.error.ErrorExposure;
import com.smbtech.serviceframework.error.FallbackThrowableErrorResolver;
import com.smbtech.serviceframework.error.ServiceException;
import com.smbtech.serviceframework.httpclient.domain.HttpErrorResponse;
import com.smbtech.serviceframework.httpclient.exception.HttpClientResponseException;
import com.smbtech.serviceframework.starter.errorhandling.adapter.in.security.SecurityAccessDeniedHandler;
import com.smbtech.serviceframework.starter.errorhandling.adapter.in.security.SecurityAuthenticationEntryPoint;
import com.smbtech.serviceframework.starter.errorhandling.api.ErrorExposurePolicy;
import com.smbtech.serviceframework.starter.errorhandling.api.NotificationResponseFactory;
import com.smbtech.serviceframework.starter.errorhandling.api.NotificationSerializer;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class ErrorHandlingWebApplicationIntegrationTest {

    private final WebApplicationContextRunner contextRunner = contextRunner(ErrorExposure.PUBLIC);

    private final WebApplicationContextRunner internalContextRunner =
            contextRunner(ErrorExposure.INTERNAL);

    @Test
    void autoConfiguredMockMvcReturnsSnakeCaseValidationResponse() {
        internalContextRunner.run(
                context -> {
                    assertThat(context).hasNotFailed();

                    try {
                        mockMvc(context)
                                .perform(
                                        post("/customers")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .accept(MediaType.APPLICATION_JSON)
                                                .content("{\"customerId\":\"\"}"))
                                .andExpect(status().isBadRequest())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$.field_name").value(""))
                                .andExpect(jsonPath("$.metadata.schema_version").value("1"))
                                .andExpect(jsonPath("$.metadata.category").value("VALIDATION"))
                                .andExpect(jsonPath("$.metadata.retryable").value(false))
                                .andExpect(jsonPath("$.metadata.request.method").value("POST"))
                                .andExpect(jsonPath("$.metadata.request.route").value("/customers"))
                                .andExpect(
                                        jsonPath("$.metadata.validation.type")
                                                .value("bean_validation"))
                                .andExpect(
                                        jsonPath("$.metadata.violations[0].field_name")
                                                .value("customerId"))
                                .andExpect(jsonPath("$.metadata.violations[0].code").exists())
                                .andExpect(jsonPath("$.fieldName").doesNotExist());
                    } catch (Exception exception) {
                        throw new AssertionError(exception);
                    }
                });
    }

    @Test
    void autoConfiguredMockMvcConvertsHttpClientFailureToSafeNotification() {
        internalContextRunner.run(
                context -> {
                    assertThat(context).hasNotFailed();

                    try {
                        mockMvc(context)
                                .perform(get("/downstream").accept(MediaType.APPLICATION_JSON))
                                .andExpect(status().isBadGateway())
                                .andExpect(
                                        jsonPath("$.code")
                                                .value("E_SERVICE_FRAMEWORK_HTTP_CLIENT_0503"))
                                .andExpect(jsonPath("$.field_name").value(""))
                                .andExpect(jsonPath("$.metadata.schema_version").value("1"))
                                .andExpect(jsonPath("$.metadata.category").value("DOWNSTREAM"))
                                .andExpect(jsonPath("$.metadata.retryable").value(true))
                                .andExpect(
                                        jsonPath("$.metadata.request.route").value("/downstream"))
                                .andExpect(
                                        jsonPath("$.metadata.dependency.name").value("downstream"))
                                .andExpect(
                                        jsonPath("$.metadata.dependency.failure_type")
                                                .value("server_error"))
                                .andExpect(
                                        content()
                                                .string(
                                                        org.hamcrest.Matchers.not(
                                                                org.hamcrest.Matchers
                                                                        .containsString(
                                                                                "downstream-secret"))))
                                .andExpect(
                                        content()
                                                .string(
                                                        org.hamcrest.Matchers.not(
                                                                org.hamcrest.Matchers
                                                                        .containsString(
                                                                                "body-secret"))));
                    } catch (Exception exception) {
                        throw new AssertionError(exception);
                    }
                });
    }

    @Test
    void autoConfiguredSecurityHandlersUseTheSameSnakeCaseContract() {
        contextRunner.run(
                context -> {
                    SecurityAuthenticationEntryPoint entryPoint =
                            context.getBean(SecurityAuthenticationEntryPoint.class);
                    SecurityAccessDeniedHandler deniedHandler =
                            context.getBean(SecurityAccessDeniedHandler.class);
                    ObjectMapper objectMapper = context.getBean(ObjectMapper.class);
                    MockHttpServletResponse unauthorized = new MockHttpServletResponse();
                    MockHttpServletResponse forbidden = new MockHttpServletResponse();

                    try {
                        entryPoint.commence(
                                new MockHttpServletRequest("GET", "/secure"),
                                unauthorized,
                                new BadCredentialsException("Bearer authentication-secret"));
                        deniedHandler.handle(
                                new MockHttpServletRequest("GET", "/admin"),
                                forbidden,
                                new AccessDeniedException("token=authorization-secret"));

                        JsonNode unauthorizedJson =
                                objectMapper.readTree(unauthorized.getContentAsByteArray());
                        JsonNode forbiddenJson =
                                objectMapper.readTree(forbidden.getContentAsByteArray());
                        assertThat(unauthorized.getStatus()).isEqualTo(401);
                        assertThat(forbidden.getStatus()).isEqualTo(403);
                        assertThat(unauthorizedJson.has("field_name")).isTrue();
                        assertThat(forbiddenJson.has("field_name")).isTrue();
                        assertThat(unauthorizedJson.has("fieldName")).isFalse();
                        assertThat(forbiddenJson.has("fieldName")).isFalse();
                        assertThat(unauthorizedJson.at("/metadata/category").asText())
                                .isEqualTo("AUTHENTICATION");
                        assertThat(unauthorizedJson.at("/metadata/security").isMissingNode())
                                .isTrue();
                        assertThat(forbiddenJson.at("/metadata/category").asText())
                                .isEqualTo("AUTHORIZATION");
                        assertThat(forbiddenJson.at("/metadata/security").isMissingNode()).isTrue();
                        assertThat(unauthorized.getContentAsString())
                                .doesNotContain("authentication-secret");
                        assertThat(forbidden.getContentAsString())
                                .doesNotContain("authorization-secret");
                    } catch (IOException exception) {
                        throw new AssertionError(exception);
                    }
                });
    }

    @Test
    void applicationBeansReplaceFactoryAndSerializerInTheMvcPipeline() {
        Notification replacement =
                Notification.warning("W_APPLICATION_0001", "Application response");
        NotificationResponseFactory responseFactory =
                resolvedError -> ResponseEntity.unprocessableContent().body(replacement);
        NotificationSerializer serializer =
                (notification, generator, serializers) -> {
                    generator.writeStartObject();
                    generator.writeStringProperty("replacement_code", notification.code());
                    generator.writeEndObject();
                };

        contextRunner
                .withBean(NotificationResponseFactory.class, () -> responseFactory)
                .withBean(NotificationSerializer.class, () -> serializer)
                .run(
                        context -> {
                            assertThat(context).hasNotFailed();
                            assertThat(context.getBean(NotificationResponseFactory.class))
                                    .isSameAs(responseFactory);
                            assertThat(context.getBean(NotificationSerializer.class))
                                    .isSameAs(serializer);

                            try {
                                mockMvc(context)
                                        .perform(get("/failure").accept(MediaType.APPLICATION_JSON))
                                        .andExpect(status().isUnprocessableContent())
                                        .andExpect(
                                                content()
                                                        .contentTypeCompatibleWith(
                                                                MediaType.APPLICATION_JSON))
                                        .andExpect(
                                                content()
                                                        .json(
                                                                "{\"replacement_code\":\"W_APPLICATION_0001\"}"));
                            } catch (Exception exception) {
                                throw new AssertionError(exception);
                            }
                        });
    }

    @Test
    void internalExposureReturnsDetailedSanitizedMvcAndDownstreamFailures() {
        internalContextRunner.run(
                context -> {
                    assertThat(context).hasNotFailed();
                    try {
                        MockMvc mockMvc = mockMvc(context);
                        mockMvc.perform(
                                        post("/customers")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content("{"))
                                .andExpect(status().isBadRequest())
                                .andExpect(
                                        jsonPath("$.code").value("E_SERVICE_FRAMEWORK_JSON_0001"))
                                .andExpect(jsonPath("$.metadata.category").value("VALIDATION"));

                        mockMvc.perform(
                                        post("/customers")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content("{\"customerId\":\"\"}"))
                                .andExpect(status().isBadRequest())
                                .andExpect(
                                        jsonPath("$.code")
                                                .value("E_SERVICE_FRAMEWORK_VALIDATION_0001"))
                                .andExpect(jsonPath("$.metadata.violations").isArray());

                        mockMvc.perform(get("/downstream"))
                                .andExpect(status().isBadGateway())
                                .andExpect(
                                        jsonPath("$.code")
                                                .value("E_SERVICE_FRAMEWORK_HTTP_CLIENT_0503"))
                                .andExpect(jsonPath("$.metadata.category").value("DOWNSTREAM"))
                                .andExpect(jsonPath("$.metadata.dependency").isMap());

                        mockMvc.perform(get("/service-failure"))
                                .andExpect(status().isInternalServerError())
                                .andExpect(jsonPath("$.code").value("E_APPLICATION_SERVICE_0001"));

                        mockMvc.perform(get("/failure"))
                                .andExpect(status().isInternalServerError())
                                .andExpect(
                                        jsonPath("$.code")
                                                .value(
                                                        FallbackThrowableErrorResolver
                                                                .DEFAULT_ERROR_CODE));
                    } catch (Exception exception) {
                        throw new AssertionError(exception);
                    }
                });
    }

    @Test
    void publicExposureReturnsServiceExceptionNotification() {
        contextRunner.run(
                context -> {
                    try {
                        mockMvc(context)
                                .perform(get("/service-failure"))
                                .andExpect(status().isInternalServerError())
                                .andExpect(jsonPath("$.code").value("E_APPLICATION_SERVICE_0001"))
                                .andExpect(
                                        jsonPath("$.message")
                                                .value(
                                                        FallbackThrowableErrorResolver
                                                                .DEFAULT_PUBLIC_MESSAGE));
                    } catch (Exception exception) {
                        throw new AssertionError(exception);
                    }
                });
    }

    @Test
    void applicationExposurePolicyOverridesTheConfiguredPropertyInMvc() {
        ErrorExposurePolicy applicationPolicy = resolvedError -> ErrorExposure.PUBLIC;

        internalContextRunner
                .withBean(ErrorExposurePolicy.class, () -> applicationPolicy)
                .run(
                        context -> {
                            assertThat(context.getBean(ErrorExposurePolicy.class))
                                    .isSameAs(applicationPolicy);
                            assertThat(context)
                                    .doesNotHaveBean(ConfiguredErrorExposurePolicy.class);
                            try {
                                mockMvc(context)
                                        .perform(get("/service-failure"))
                                        .andExpect(status().isInternalServerError())
                                        .andExpect(
                                                jsonPath("$.code")
                                                        .value("E_APPLICATION_SERVICE_0001"));
                            } catch (Exception exception) {
                                throw new AssertionError(exception);
                            }
                        });
    }

    private static WebApplicationContextRunner contextRunner(ErrorExposure exposure) {
        return new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(ErrorHandlingAutoConfiguration.class))
                .withUserConfiguration(WebTestConfiguration.class)
                .withPropertyValues("smbtech.error-handling.response.exposure=" + exposure.name());
    }

    private static MockMvc mockMvc(WebApplicationContext context) {
        return MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    static class WebTestConfiguration {

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        TestController testController() {
            return new TestController();
        }
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

        @GetMapping("/downstream")
        String downstream() {
            throw new HttpClientResponseException(
                    new HttpErrorResponse(
                            "payments",
                            "POST",
                            "https://payments.example/orders?token=downstream-secret",
                            503,
                            "Service Unavailable",
                            HttpErrorResponse.categoryOf(503),
                            Map.of("Authorization", "Bearer downstream-secret"),
                            "{\"password\":\"body-secret\"}",
                            "application/json",
                            "UTF-8",
                            false));
        }

        @GetMapping("/failure")
        String failure() {
            throw new IllegalStateException("internal failure");
        }

        @GetMapping("/service-failure")
        String serviceFailure() {
            throw new ServiceException(
                    Notification.error("E_APPLICATION_SERVICE_0001", "Service operation failed"),
                    "password=service-diagnostic-secret");
        }
    }

    record CustomerRequest(@NotBlank String customerId) {}
}
