package com.smbtech.serviceframework.starter.errorhandling.adapter.out.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.smbtech.serviceframework.commons.notification.Notification;
import com.smbtech.serviceframework.error.ErrorCategory;
import com.smbtech.serviceframework.error.ErrorExposure;
import com.smbtech.serviceframework.error.ResolvedError;
import com.smbtech.serviceframework.error.metadata.SecurityErrorMetadata;
import com.smbtech.serviceframework.error.metadata.StandardErrorMetadata;
import com.smbtech.serviceframework.logging.domain.EventType;
import com.smbtech.serviceframework.logging.domain.LogLevel;
import com.smbtech.serviceframework.logging.domain.Sensitivity;
import com.smbtech.serviceframework.logging.domain.StructuredEvent;
import com.smbtech.serviceframework.logging.port.in.StructuredLogger;
import com.smbtech.serviceframework.logging.port.out.CorrelationContext;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

class StructuredErrorReporterTest {

    @ParameterizedTest
    @EnumSource(ErrorExposure.class)
    void logsDiagnosticsCauseAndCategoryForEveryExposure(ErrorExposure exposure) {
        CapturingLogger logger = new CapturingLogger();
        StructuredErrorReporter reporter =
                new StructuredErrorReporter(logger, correlationContext(Map.of()));
        IllegalArgumentException rootCause = new IllegalArgumentException("password=root-secret");
        IllegalStateException cause = new IllegalStateException("token=cause-secret", rootCause);
        ResolvedError resolvedError =
                new ResolvedError(
                        Notification.error("E_DEPENDENCY_0001", "Dependency failed"),
                        ErrorCategory.DOWNSTREAM,
                        exposure,
                        "authorization=diagnostic-secret");

        reporter.report(cause, resolvedError, new MockHttpServletRequest("GET", "/dependency"));

        assertEquals("E_DEPENDENCY_0001", logger.event.data().get("code"));
        assertEquals("DOWNSTREAM", logger.event.data().get("category"));
        assertEquals(
                IllegalStateException.class.getName(), logger.event.data().get("exceptionType"));
        assertEquals("authorization=<redacted>", logger.event.data().get("diagnostic"));
        Map<?, ?> loggedCause = (Map<?, ?>) logger.event.data().get("cause");
        assertEquals(IllegalStateException.class.getName(), loggedCause.get("type"));
        assertEquals(IllegalArgumentException.class.getName(), loggedCause.get("rootType"));
    }

    @Test
    void reportsInternalFailureWithStructuredRequestAndSanitizedDiagnostics() {
        CapturingLogger logger = new CapturingLogger();
        StructuredErrorReporter reporter =
                new StructuredErrorReporter(
                        logger,
                        correlationContext(
                                Map.of(StructuredErrorReporter.TRANSACTION_ID_KEY, "tx-123")));
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/customers/123");
        IllegalArgumentException rootCause = new IllegalArgumentException("password=root-secret");
        IllegalStateException cause = new IllegalStateException("token=request-secret", rootCause);
        ResolvedError resolvedError =
                new ResolvedError(
                        Notification.error("E_CUSTOMER_0001", "Request failed"),
                        ErrorCategory.INTERNAL,
                        ErrorExposure.INTERNAL,
                        "Authorization: Bearer header.payload.signature password=diagnostic-secret");

        reporter.report(cause, resolvedError, request);

        assertEquals(LogLevel.ERROR, logger.level);
        assertNotNull(logger.event);
        assertEquals(EventType.ERROR, logger.event.type());
        assertEquals("E_CUSTOMER_0001", logger.event.data().get("code"));
        assertEquals("INTERNAL", logger.event.data().get("category"));
        assertEquals("tx-123", logger.event.data().get("correlationId"));
        assertEquals(
                Map.of("method", "POST", "path", "/customers/123"),
                logger.event.data().get("request"));
        Map<?, ?> loggedCause = (Map<?, ?>) logger.event.data().get("cause");
        assertEquals(IllegalStateException.class.getName(), loggedCause.get("type"));
        assertEquals("token=<redacted>", loggedCause.get("message"));
        assertEquals(IllegalArgumentException.class.getName(), loggedCause.get("rootType"));
        assertEquals("password=<redacted>", loggedCause.get("rootMessage"));
        assertEquals(
                "Authorization: <redacted> <redacted> password=<redacted>",
                logger.event.data().get("diagnostic"));
        assertEquals(Sensitivity.SENSITIVE, logger.event.sensitivity());
        assertFalse(logger.event.throwable().toString().contains("request-secret"));
        assertFalse(logger.event.throwable().getCause().toString().contains("root-secret"));
    }

    @Test
    void usesWarnForExpectedRequestErrorsAndSkipsDisabledEvents() {
        CapturingLogger logger = new CapturingLogger();
        StructuredErrorReporter reporter =
                new StructuredErrorReporter(logger, correlationContext(Map.of()));
        ResolvedError resolvedError =
                new ResolvedError(
                        Notification.error("E_REQUEST_0001", "Invalid request"),
                        ErrorCategory.VALIDATION,
                        ErrorExposure.PUBLIC,
                        "Validation failed");

        reporter.report(
                new IllegalArgumentException("invalid"),
                resolvedError,
                new MockHttpServletRequest("GET", "/customers"));

        assertEquals(LogLevel.WARN, logger.level);
        assertEquals("", logger.event.data().get("correlationId"));

        logger.enabled = false;
        logger.event = null;
        reporter.report(
                new IllegalArgumentException("invalid"),
                resolvedError,
                new MockHttpServletRequest("GET", "/customers"));

        assertNull(logger.event);
    }

    @Test
    void omitsDiagnosticDataWhenDisabled() {
        CapturingLogger logger = new CapturingLogger();
        StructuredErrorReporter reporter =
                new StructuredErrorReporter(logger, correlationContext(Map.of()), false);

        reporter.report(
                new IllegalStateException("failure"),
                new ResolvedError(
                        Notification.error("E_INTERNAL_0001", "Request failed"),
                        ErrorCategory.INTERNAL,
                        ErrorExposure.INTERNAL,
                        "password=diagnostic-secret"),
                new MockHttpServletRequest("GET", "/failure"));

        assertFalse(logger.event.data().containsKey("diagnostic"));
    }

    @Test
    void reportsFinalStatusSecurityReasonAndInternalOAuth2ErrorCode() {
        CapturingLogger logger = new CapturingLogger();
        StructuredErrorReporter reporter =
                new StructuredErrorReporter(
                        logger,
                        correlationContext(
                                Map.of(StructuredErrorReporter.TRANSACTION_ID_KEY, "tx-security")));
        OAuth2AuthenticationException cause =
                new OAuth2AuthenticationException(
                        new OAuth2Error(
                                "temporarily_unavailable",
                                "token=provider-secret",
                                "https://provider.internal/errors/42"));
        ResolvedError error =
                new ResolvedError(
                        Notification.builder()
                                .code("E_SERVICE_FRAMEWORK_SECURITY_AUTHENTICATION_0004")
                                .message("Authentication provider is unavailable")
                                .metadata(
                                        StandardErrorMetadata.builder(ErrorCategory.DOWNSTREAM)
                                                .security(
                                                        new SecurityErrorMetadata(
                                                                "provider_failure", "bearer"))
                                                .build()
                                                .toMap())
                                .build(),
                        ErrorCategory.DOWNSTREAM,
                        ErrorExposure.PUBLIC,
                        "oauth2Error=temporarily_unavailable token=diagnostic-secret");

        reporter.report(cause, error, new MockHttpServletRequest("GET", "/secure"), 502);

        assertEquals(502, logger.event.data().get("status"));
        assertEquals("provider_failure", logger.event.data().get("securityReason"));
        assertEquals("temporarily_unavailable", logger.event.data().get("oauth2ErrorCode"));
        assertEquals(
                OAuth2AuthenticationException.class.getName(),
                logger.event.data().get("exceptionType"));
        assertEquals(
                "oauth2Error=temporarily_unavailable token=<redacted>",
                logger.event.data().get("diagnostic"));
        assertFalse(logger.event.data().toString().contains("provider-secret"));
        assertFalse(logger.event.data().toString().contains("provider.internal"));
    }

    private static CorrelationContext correlationContext(Map<String, String> values) {
        return new CorrelationContext() {
            @Override
            public Map<String, String> snapshot() {
                return values;
            }

            @Override
            public Scope open(Map<String, String> ignored) {
                return () -> {};
            }
        };
    }

    private static final class CapturingLogger implements StructuredLogger {
        private boolean enabled = true;
        private LogLevel level;
        private StructuredEvent event;

        @Override
        public boolean isEnabled(LogLevel level, EventType eventType) {
            assertSame(EventType.ERROR, eventType);
            return enabled;
        }

        @Override
        public void log(LogLevel level, StructuredEvent event) {
            this.level = level;
            this.event = event;
        }
    }
}
