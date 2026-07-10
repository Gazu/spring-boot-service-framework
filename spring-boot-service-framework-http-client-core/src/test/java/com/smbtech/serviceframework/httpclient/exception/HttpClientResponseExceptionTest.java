package com.smbtech.serviceframework.httpclient.exception;

import com.smbtech.serviceframework.commons.notification.Notification;
import com.smbtech.serviceframework.commons.notification.NotifyingException;
import com.smbtech.serviceframework.httpclient.domain.HttpErrorCategory;
import com.smbtech.serviceframework.httpclient.domain.HttpErrorResponse;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpClientResponseExceptionTest {

    @Test
    void exposesHttpErrorResponseAndCompleteBody() {
        HttpErrorResponse error = error("{\"code\":\"DOWNSTREAM_ERROR\",\"message\":\"full body\"}");

        HttpClientResponseException exception = new HttpClientResponseException(error);

        assertInstanceOf(NotifyingException.class, exception);
        assertSame(error, exception.error());
        assertEquals(503, exception.statusCode());
        assertEquals("{\"code\":\"DOWNSTREAM_ERROR\",\"message\":\"full body\"}", exception.responseBody());
        assertEquals(Map.of("Content-Type", "application/json"), exception.responseHeaders());
        assertEquals("application/json", exception.responseContentType());
        assertEquals("UTF-8", exception.responseCharset());
        assertEquals("Service Unavailable", exception.responseStatusText());
        assertFalse(exception.isResponseBodyTruncated());
        assertTrue(exception.notifications().isEmpty());
        assertTrue(exception.primaryNotification().isEmpty());
    }

    @Test
    void carriesStructuredNotifications() {
        HttpErrorResponse error = error("{\"message\":\"bad request\"}");
        Notification notification = Notification.error(
                "E_SERVICE_FRAMEWORK_HTTP_CLIENT_0503",
                "Service Unavailable received from HTTP client"
        );

        HttpClientResponseException exception = new HttpClientResponseException(error, notification);

        assertEquals(List.of(notification), exception.notifications());
        assertEquals(notification, exception.primaryNotification().orElseThrow());
        assertEquals("HTTP client request failed client=payments method=GET uri=https://payments.example/v1/orders status=503 reason=Service Unavailable", exception.getMessage());
    }

    @Test
    void preservesCauseWhenNotificationsAreProvided() {
        HttpErrorResponse error = error("{}");
        Notification notification = Notification.error(
                "E_SERVICE_FRAMEWORK_HTTP_CLIENT_0503",
                "Service Unavailable received from HTTP client"
        );
        RuntimeException cause = new RuntimeException("transport failed");

        HttpClientResponseException exception = new HttpClientResponseException(error, notification, cause);

        assertSame(cause, exception.getCause());
        assertEquals(notification, exception.primaryNotification().orElseThrow());
    }

    @Test
    void notificationsAreImmutableAndDefensivelyCopied() {
        HttpErrorResponse error = error("{}");
        Notification notification = Notification.error(
                "E_SERVICE_FRAMEWORK_HTTP_CLIENT_0503",
                "Service Unavailable received from HTTP client"
        );

        HttpClientResponseException exception = new HttpClientResponseException(error, List.of(notification));

        assertThrows(UnsupportedOperationException.class, () -> exception.notifications().add(notification));
    }

    private HttpErrorResponse error(String body) {
        return new HttpErrorResponse(
                "payments",
                "GET",
                "https://payments.example/v1/orders",
                503,
                "Service Unavailable",
                HttpErrorCategory.SERVER_ERROR,
                Map.of("Content-Type", "application/json"),
                body,
                "application/json",
                "UTF-8",
                false
        );
    }
}
