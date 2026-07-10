package com.smbtech.serviceframework.httpclient.domain;

import com.smbtech.serviceframework.commons.notification.Notification;
import com.smbtech.serviceframework.commons.notification.NotificationSeverity;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HttpErrorNotificationMapperTest {

    private final HttpErrorNotificationMapper mapper = new HttpErrorNotificationMapper();

    @Test
    void mapsHttpStatusToStableNotificationCode() {
        Notification notification = mapper.notification(error(404, "Not Found", HttpErrorCategory.CLIENT_ERROR));

        assertEquals("E_SERVICE_FRAMEWORK_HTTP_CLIENT_0404", notification.code());
        assertEquals(NotificationSeverity.ERROR, notification.severity());
        assertEquals("HTTP 404 Not Found response received from downstream service", notification.message());
    }

    @Test
    void mapsUnknownStatusToUnknownCode() {
        Notification notification = mapper.notification(error(-1, "", HttpErrorCategory.UNKNOWN));

        assertEquals(HttpErrorNotificationMapper.UNKNOWN_ERROR_CODE, notification.code());
        assertEquals("HTTP -1 response received from downstream service", notification.message());
    }

    @Test
    void includesSafeMetadataWithoutResponseBody() {
        Notification notification = mapper.notification(error(503, "Service Unavailable", HttpErrorCategory.SERVER_ERROR));

        assertEquals("payments", notification.metadata().get("clientName"));
        assertEquals("GET", notification.metadata().get("method"));
        assertEquals("https://payments.example/v1/orders", notification.metadata().get("uri"));
        assertEquals(503, notification.metadata().get("statusCode"));
        assertEquals("Service Unavailable", notification.metadata().get("reasonPhrase"));
        assertEquals("SERVER_ERROR", notification.metadata().get("category"));
        assertEquals("application/json", notification.metadata().get("contentType"));
        assertEquals("UTF-8", notification.metadata().get("charset"));
        assertEquals(false, notification.metadata().get("bodyTruncated"));
        assertFalse(notification.metadata().containsKey("body"));
    }

    @Test
    void returnsSingleNotificationList() {
        List<Notification> notifications = mapper.map(error(500, "Internal Server Error", HttpErrorCategory.SERVER_ERROR));

        assertEquals(1, notifications.size());
        assertEquals("E_SERVICE_FRAMEWORK_HTTP_CLIENT_0500", notifications.getFirst().code());
        assertThrows(UnsupportedOperationException.class, () -> notifications.add(notifications.getFirst()));
    }

    @Test
    void usesPolicyControlledCodePrefix() {
        ErrorHandlingPolicy policy = new ErrorHandlingPolicy(
                true,
                true,
                4096,
                true,
                true,
                "E_CUSTOM_REST"
        );

        Notification notification = mapper.notification(error(429, "Too Many Requests", HttpErrorCategory.CLIENT_ERROR), policy);

        assertEquals("E_CUSTOM_REST_0429", notification.code());
    }

    @Test
    void omitsMetadataWhenPolicyDisablesNotificationMetadata() {
        ErrorHandlingPolicy policy = new ErrorHandlingPolicy(
                true,
                true,
                4096,
                true,
                false,
                "E_SERVICE_FRAMEWORK_HTTP_CLIENT_"
        );

        Notification notification = mapper.notification(error(500, "Internal Server Error", HttpErrorCategory.SERVER_ERROR), policy);

        assertEquals(Map.of(), notification.metadata());
    }

    private HttpErrorResponse error(int statusCode, String reasonPhrase, HttpErrorCategory category) {
        return new HttpErrorResponse(
                "payments",
                "GET",
                "https://payments.example/v1/orders",
                statusCode,
                reasonPhrase,
                category,
                Map.of("Content-Type", "application/json"),
                "{\"message\":\"full downstream body\"}",
                "application/json",
                "UTF-8",
                false
        );
    }
}
