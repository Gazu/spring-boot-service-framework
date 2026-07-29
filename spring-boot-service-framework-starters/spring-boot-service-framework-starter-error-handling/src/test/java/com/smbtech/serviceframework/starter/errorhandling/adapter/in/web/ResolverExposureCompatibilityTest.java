package com.smbtech.serviceframework.starter.errorhandling.adapter.in.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import com.smbtech.serviceframework.commons.notification.Notification;
import com.smbtech.serviceframework.error.ErrorExposure;
import com.smbtech.serviceframework.error.FallbackThrowableErrorResolver;
import com.smbtech.serviceframework.error.ResolvedError;
import com.smbtech.serviceframework.error.ServiceException;
import com.smbtech.serviceframework.error.ServiceExceptionThrowableErrorResolver;
import com.smbtech.serviceframework.httpclient.domain.HttpErrorResponse;
import com.smbtech.serviceframework.httpclient.exception.HttpClientResponseException;
import com.smbtech.serviceframework.starter.errorhandling.adapter.in.security.DefaultSecurityAuthenticationFailureResolver;
import com.smbtech.serviceframework.starter.errorhandling.adapter.in.security.DefaultSecurityAuthorizationFailureResolver;
import com.smbtech.serviceframework.starter.errorhandling.api.security.SecurityErrorCatalog;
import com.smbtech.serviceframework.starter.errorhandling.api.security.SecurityFailureContext;
import com.smbtech.serviceframework.starter.errorhandling.autoconfigure.ConfiguredErrorExposurePolicy;
import com.smbtech.serviceframework.starter.errorhandling.autoconfigure.ErrorHandlingProperties;
import com.smbtech.serviceframework.starter.errorhandling.customizer.ErrorCustomizationPipeline;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindException;
import org.springframework.validation.method.MethodValidationException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

class ResolverExposureCompatibilityTest {

    private final DefaultNotificationResponseFactory responseFactory =
            new DefaultNotificationResponseFactory();

    @ParameterizedTest(name = "{0}")
    @MethodSource("publicResolverCases")
    void preservesCurrentPublicResolverBehavior(
            String resolverName, ResolvedError resolvedError, String expectedCode) {
        assertEquals(ErrorExposure.PUBLIC, resolvedError.exposure());
        assertResponseCode(resolvedError, expectedCode);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("internalResolverCases")
    void preservesCurrentInternalResolverBehavior(
            String resolverName, ResolvedError resolvedError, String sourceCode) {
        assertEquals(ErrorExposure.INTERNAL, resolvedError.exposure());
        assertEquals(sourceCode, resolvedError.notification().code());
        assertResponseCode(resolvedError, sourceCode);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource({"publicResolverCases", "internalResolverCases"})
    void globalInternalExposureOverridesEveryResolver(
            String resolverName, ResolvedError resolvedError, String ignoredCode) {
        ResolvedError effective = applyGlobalExposure(resolvedError, ErrorExposure.INTERNAL);

        assertEquals(ErrorExposure.INTERNAL, effective.exposure());
        assertResponseCode(effective, resolvedError.notification().code());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource({"publicResolverCases", "internalResolverCases"})
    void globalPublicExposureOverridesEveryResolver(
            String resolverName, ResolvedError resolvedError, String ignoredCode) {
        ResolvedError effective = applyGlobalExposure(resolvedError, ErrorExposure.PUBLIC);

        assertEquals(ErrorExposure.PUBLIC, effective.exposure());
        assertResponseCode(effective, resolvedError.notification().code());
    }

    private void assertResponseCode(ResolvedError resolvedError, String expectedCode) {
        Notification body = responseFactory.create(resolvedError).getBody();

        assertNotNull(body);
        assertEquals(expectedCode, body.code());
    }

    private static ResolvedError applyGlobalExposure(
            ResolvedError resolvedError, ErrorExposure exposure) {
        ErrorHandlingProperties properties = new ErrorHandlingProperties();
        properties.getResponse().setExposure(exposure);
        ErrorCustomizationPipeline pipeline =
                new ErrorCustomizationPipeline(
                        List.of(), List.of(), new ConfiguredErrorExposurePolicy(properties));
        return pipeline.customize(
                new IllegalStateException("failure"),
                resolvedError,
                new MockHttpServletRequest("GET", "/failure"));
    }

    private static Stream<Arguments> publicResolverCases() {
        return Stream.of(
                serviceExceptionCase(),
                springMvcCase(),
                requestValidationCase(),
                httpClientCase(),
                authenticationCase(),
                authorizationCase());
    }

    private static Stream<Arguments> internalResolverCases() {
        return Stream.of(fallbackCase(), internalValidationCase());
    }

    private static Arguments serviceExceptionCase() {
        String code = "E_CUSTOMER_0001";
        ResolvedError resolvedError =
                new ServiceExceptionThrowableErrorResolver()
                        .resolve(new ServiceException(Notification.error(code, "Customer failed")));
        return Arguments.of("ServiceExceptionThrowableErrorResolver", resolvedError, code);
    }

    private static Arguments springMvcCase() {
        String code = "E_SERVICE_FRAMEWORK_ROUTE_0001";
        ResolvedError resolvedError =
                new SpringMvcExceptionResolver()
                        .resolve(
                                new NoResourceFoundException(
                                        HttpMethod.GET, "/missing", "No static resource"));
        return Arguments.of("SpringMvcExceptionResolver", resolvedError, code);
    }

    private static Arguments requestValidationCase() {
        String code = ValidationExceptionResolver.VALIDATION_ERROR_CODE;
        ResolvedError resolvedError =
                new ValidationExceptionResolver().resolve(new BindException(new Object(), "body"));
        return Arguments.of("ValidationExceptionResolver request", resolvedError, code);
    }

    private static Arguments httpClientCase() {
        String code = "E_SERVICE_FRAMEWORK_HTTP_CLIENT_0503";
        HttpErrorResponse error =
                new HttpErrorResponse(
                        "payments",
                        "POST",
                        "https://payments.example/orders",
                        503,
                        "Service unavailable",
                        HttpErrorResponse.categoryOf(503),
                        Map.of(),
                        "",
                        "application/json",
                        "UTF-8",
                        false);
        ResolvedError resolvedError =
                new HttpClientExceptionResolver()
                        .resolve(new HttpClientResponseException(error, List.of()));
        return Arguments.of("HttpClientExceptionResolver", resolvedError, code);
    }

    private static Arguments authenticationCase() {
        ResolvedError resolvedError =
                new DefaultSecurityAuthenticationFailureResolver()
                        .resolve(
                                new SecurityFailureContext(
                                        new BadCredentialsException("Credentials missing")))
                        .resolvedError();
        return Arguments.of(
                "DefaultSecurityAuthenticationFailureResolver",
                resolvedError,
                SecurityErrorCatalog.AUTHENTICATION_REQUIRED.code());
    }

    private static Arguments authorizationCase() {
        ResolvedError resolvedError =
                new DefaultSecurityAuthorizationFailureResolver()
                        .resolve(
                                new SecurityFailureContext(
                                        new AccessDeniedException("Access denied")))
                        .resolvedError();
        return Arguments.of(
                "DefaultSecurityAuthorizationFailureResolver",
                resolvedError,
                SecurityErrorCatalog.ACCESS_DENIED.code());
    }

    private static Arguments fallbackCase() {
        ResolvedError resolvedError =
                new FallbackThrowableErrorResolver()
                        .resolve(new IllegalStateException("Internal failure"));
        return Arguments.of(
                "FallbackThrowableErrorResolver",
                resolvedError,
                FallbackThrowableErrorResolver.DEFAULT_ERROR_CODE);
    }

    private static Arguments internalValidationCase() {
        String code = "E_SERVICE_FRAMEWORK_INTERNAL_VALIDATION_0001";
        MethodValidationException exception = mock(MethodValidationException.class);
        ResolvedError resolvedError = new ValidationExceptionResolver().resolve(exception);
        return Arguments.of("ValidationExceptionResolver return value", resolvedError, code);
    }
}
