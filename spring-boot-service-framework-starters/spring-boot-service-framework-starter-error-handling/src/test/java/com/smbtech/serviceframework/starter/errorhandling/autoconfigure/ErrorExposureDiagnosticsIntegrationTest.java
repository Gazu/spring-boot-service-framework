package com.smbtech.serviceframework.starter.errorhandling.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import com.smbtech.serviceframework.commons.notification.Notification;
import com.smbtech.serviceframework.commons.notification.NotificationSeverity;
import com.smbtech.serviceframework.error.ErrorCategory;
import com.smbtech.serviceframework.error.ErrorDefinition;
import com.smbtech.serviceframework.error.ErrorExposure;
import com.smbtech.serviceframework.error.ResolvedError;
import com.smbtech.serviceframework.error.ServiceException;
import com.smbtech.serviceframework.starter.errorhandling.adapter.in.security.SecurityAuthenticationEntryPoint;
import com.smbtech.serviceframework.starter.errorhandling.adapter.in.web.ServiceFrameworkExceptionHandler;
import com.smbtech.serviceframework.starter.errorhandling.api.ErrorReporter;
import com.smbtech.serviceframework.starter.errorhandling.api.security.SecurityErrorCatalog;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import tools.jackson.databind.ObjectMapper;

class ErrorExposureDiagnosticsIntegrationTest {

    private static final String ERROR_CODE = "E_DEPENDENCY_0001";
    private static final String DIAGNOSTIC = "token=diagnostic-secret";

    private final WebApplicationContextRunner contextRunner =
            new WebApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(ErrorHandlingAutoConfiguration.class))
                    .withBean(ObjectMapper.class, ObjectMapper::new);

    @ParameterizedTest
    @EnumSource(ErrorExposure.class)
    void reportsOriginalDiagnosticsCauseAndCategoryForEveryExposure(ErrorExposure exposure) {
        AtomicReference<ReportedFailure> reported = new AtomicReference<>();
        ErrorReporter reporter =
                (cause, resolvedError, request) ->
                        reported.set(new ReportedFailure(cause, resolvedError));
        IllegalStateException rootCause = new IllegalStateException("password=root-cause-secret");
        ServiceException failure = ServiceException.from(errorDefinition(), DIAGNOSTIC, rootCause);

        contextRunner
                .withPropertyValues("smbtech.error-handling.response.exposure=" + exposure.name())
                .withBean(ErrorReporter.class, () -> reporter)
                .run(
                        context -> {
                            Notification response =
                                    context.getBean(ServiceFrameworkExceptionHandler.class)
                                            .handleException(
                                                    failure,
                                                    new MockHttpServletRequest(
                                                            "GET", "/dependency"))
                                            .getBody();
                            ReportedFailure captured = reported.get();

                            assertThat(response).isNotNull();
                            assertThat(response.code()).isEqualTo(ERROR_CODE);
                            assertThat(captured).isNotNull();
                            assertThat(captured.cause()).isSameAs(failure);
                            assertThat(captured.cause().getCause()).isSameAs(rootCause);
                            assertThat(captured.resolvedError().notification().code())
                                    .isEqualTo(ERROR_CODE);
                            assertThat(captured.resolvedError().diagnosticMessage())
                                    .isEqualTo(DIAGNOSTIC);
                            assertThat(captured.resolvedError().category())
                                    .isEqualTo(ErrorCategory.DOWNSTREAM);
                            assertThat(captured.resolvedError().exposure()).isEqualTo(exposure);
                        });
    }

    @ParameterizedTest
    @EnumSource(ErrorExposure.class)
    void securityReporterKeepsOriginalFailureAndDiagnosticsForEveryExposure(
            ErrorExposure exposure) {
        AtomicReference<ReportedFailure> reported = new AtomicReference<>();
        ErrorReporter reporter =
                (cause, resolvedError, request) ->
                        reported.set(new ReportedFailure(cause, resolvedError));
        BadCredentialsException failure =
                new BadCredentialsException("token=authentication-secret");

        contextRunner
                .withPropertyValues("smbtech.error-handling.response.exposure=" + exposure.name())
                .withBean(ErrorReporter.class, () -> reporter)
                .run(
                        context -> {
                            MockHttpServletResponse response = new MockHttpServletResponse();
                            try {
                                context.getBean(SecurityAuthenticationEntryPoint.class)
                                        .commence(
                                                new MockHttpServletRequest("GET", "/secure"),
                                                response,
                                                failure);
                            } catch (IOException exception) {
                                throw new AssertionError(exception);
                            }

                            ReportedFailure captured = reported.get();
                            assertThat(captured).isNotNull();
                            assertThat(captured.cause()).isSameAs(failure);
                            assertThat(captured.resolvedError().notification().code())
                                    .isEqualTo(SecurityErrorCatalog.AUTHENTICATION_REQUIRED.code());
                            assertThat(captured.resolvedError().diagnosticMessage())
                                    .contains(
                                            BadCredentialsException.class.getName(),
                                            "token=authentication-secret");
                            assertThat(captured.resolvedError().category())
                                    .isEqualTo(ErrorCategory.AUTHENTICATION);
                            assertThat(captured.resolvedError().exposure()).isEqualTo(exposure);
                            assertThat(response.getContentAsString())
                                    .contains(SecurityErrorCatalog.AUTHENTICATION_REQUIRED.code());
                        });
    }

    private static ErrorDefinition errorDefinition() {
        return new ErrorDefinition() {
            @Override
            public String code() {
                return ERROR_CODE;
            }

            @Override
            public ErrorCategory category() {
                return ErrorCategory.DOWNSTREAM;
            }

            @Override
            public String publicMessage() {
                return "Dependency failed";
            }

            @Override
            public NotificationSeverity severity() {
                return NotificationSeverity.ERROR;
            }
        };
    }

    private record ReportedFailure(Throwable cause, ResolvedError resolvedError) {}
}
