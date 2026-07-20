package com.smbtech.serviceframework.starter.errorhandling.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.smbtech.serviceframework.commons.notification.Notification;
import com.smbtech.serviceframework.error.ErrorCategory;
import com.smbtech.serviceframework.error.ErrorExposure;
import com.smbtech.serviceframework.error.ResolvedError;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class CompositeErrorReporterTest {

    @Test
    void invokesAllReportersInOrderEvenWhenOneFails() {
        List<String> calls = new ArrayList<>();
        CompositeErrorReporter reporter =
                new CompositeErrorReporter(
                        List.of(
                                reporter("second", 10, calls, false),
                                reporter("first", -10, calls, true),
                                reporter("third", 20, calls, false)));

        assertThrows(
                IllegalStateException.class,
                () ->
                        reporter.report(
                                new IllegalStateException("failure"),
                                new ResolvedError(
                                        Notification.error("E_INTERNAL_0001", "failure"),
                                        ErrorCategory.INTERNAL,
                                        ErrorExposure.INTERNAL,
                                        "diagnostic"),
                                new MockHttpServletRequest()));

        assertEquals(List.of("first", "second", "third"), calls);
        assertEquals(3, reporter.reporters().size());
    }

    @Test
    void propagatesFinalStatusToStatusAwareReporters() {
        AtomicInteger status = new AtomicInteger();
        ErrorReporter statusAwareReporter =
                new ErrorReporter() {
                    @Override
                    public void report(
                            Throwable cause,
                            ResolvedError resolvedError,
                            jakarta.servlet.http.HttpServletRequest request) {}

                    @Override
                    public void report(
                            Throwable cause,
                            ResolvedError resolvedError,
                            jakarta.servlet.http.HttpServletRequest request,
                            int statusCode) {
                        status.set(statusCode);
                    }
                };
        CompositeErrorReporter reporter = new CompositeErrorReporter(List.of(statusAwareReporter));

        reporter.report(
                new IllegalStateException("failure"),
                new ResolvedError(
                        Notification.error("E_INTERNAL_0001", "failure"),
                        ErrorCategory.INTERNAL,
                        ErrorExposure.INTERNAL,
                        "diagnostic"),
                new MockHttpServletRequest(),
                503);

        assertEquals(503, status.get());
    }

    private static ErrorReporter reporter(
            String name, int order, List<String> calls, boolean fail) {
        return new ErrorReporter() {
            @Override
            public void report(
                    Throwable cause,
                    ResolvedError resolvedError,
                    jakarta.servlet.http.HttpServletRequest request) {
                calls.add(name);
                if (fail) {
                    throw new IllegalStateException(name);
                }
            }

            @Override
            public int order() {
                return order;
            }
        };
    }
}
