package com.smbtech.serviceframework.starter.errorhandling.adapter.in.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.smbtech.serviceframework.commons.notification.Notification;
import com.smbtech.serviceframework.error.ErrorCategory;
import com.smbtech.serviceframework.error.ErrorExposure;
import com.smbtech.serviceframework.error.ResolvedError;
import java.util.EnumSet;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.HttpStatus;

class DefaultNotificationHttpStatusResolverTest {

    private static final Map<ErrorCategory, HttpStatus> EXPECTED_STATUSES =
            Map.ofEntries(
                    Map.entry(ErrorCategory.VALIDATION, HttpStatus.BAD_REQUEST),
                    Map.entry(ErrorCategory.AUTHENTICATION, HttpStatus.UNAUTHORIZED),
                    Map.entry(ErrorCategory.AUTHORIZATION, HttpStatus.FORBIDDEN),
                    Map.entry(ErrorCategory.NOT_FOUND, HttpStatus.NOT_FOUND),
                    Map.entry(ErrorCategory.CONFLICT, HttpStatus.CONFLICT),
                    Map.entry(ErrorCategory.DOWNSTREAM, HttpStatus.BAD_GATEWAY),
                    Map.entry(ErrorCategory.RATE_LIMIT, HttpStatus.TOO_MANY_REQUESTS),
                    Map.entry(ErrorCategory.METHOD_NOT_ALLOWED, HttpStatus.METHOD_NOT_ALLOWED),
                    Map.entry(
                            ErrorCategory.UNSUPPORTED_MEDIA_TYPE,
                            HttpStatus.UNSUPPORTED_MEDIA_TYPE),
                    Map.entry(ErrorCategory.NOT_ACCEPTABLE, HttpStatus.NOT_ACCEPTABLE),
                    Map.entry(ErrorCategory.INTERNAL, HttpStatus.INTERNAL_SERVER_ERROR));

    private final DefaultNotificationHttpStatusResolver resolver =
            new DefaultNotificationHttpStatusResolver();

    @ParameterizedTest
    @EnumSource(ErrorCategory.class)
    void mapsEveryErrorCategory(ErrorCategory category) {
        ResolvedError error =
                new ResolvedError(
                        Notification.error("E_TEST_0001", "Failure"),
                        category,
                        ErrorExposure.PUBLIC,
                        "Diagnostic");

        assertEquals(EXPECTED_STATUSES.get(category), resolver.resolve(error));
    }

    @Test
    void keepsTheStatusTableExhaustive() {
        assertEquals(EnumSet.allOf(ErrorCategory.class), EXPECTED_STATUSES.keySet());
    }

    @Test
    void rejectsMissingResolvedError() {
        assertThrows(NullPointerException.class, () -> resolver.resolve(null));
    }
}
