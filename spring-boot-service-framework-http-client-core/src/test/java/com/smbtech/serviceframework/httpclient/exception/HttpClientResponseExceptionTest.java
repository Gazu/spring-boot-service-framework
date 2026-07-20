package com.smbtech.serviceframework.httpclient.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.smbtech.serviceframework.commons.notification.Notification;
import com.smbtech.serviceframework.commons.notification.NotifyingException;
import com.smbtech.serviceframework.httpclient.domain.HttpErrorCategory;
import com.smbtech.serviceframework.httpclient.domain.HttpErrorResponse;
import com.smbtech.serviceframework.httpclient.port.out.HttpErrorResponseBodyReader;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class HttpClientResponseExceptionTest {

    @Test
    void exposesHttpErrorResponseAndCompleteBody() {
        HttpErrorResponse error =
                error("{\"code\":\"DOWNSTREAM_ERROR\",\"message\":\"full body\"}");

        HttpClientResponseException exception = new HttpClientResponseException(error);

        assertInstanceOf(NotifyingException.class, exception);
        assertSame(error, exception.error());
        assertEquals(503, exception.statusCode());
        assertEquals(
                "{\"code\":\"DOWNSTREAM_ERROR\",\"message\":\"full body\"}",
                exception.responseBody());
        assertEquals(
                "{\"code\":\"DOWNSTREAM_ERROR\",\"message\":\"full body\"}",
                exception.getErrorResponseAsString());
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
        Notification notification =
                Notification.error(
                        "E_SERVICE_FRAMEWORK_HTTP_CLIENT_0503",
                        "Service Unavailable received from HTTP client");

        HttpClientResponseException exception =
                new HttpClientResponseException(error, notification);

        assertEquals(List.of(notification), exception.notifications());
        assertEquals(notification, exception.primaryNotification().orElseThrow());
        assertEquals(
                "HTTP client request failed client=payments method=GET uri=https://payments.example/v1/orders status=503 reason=Service Unavailable",
                exception.getMessage());
    }

    @Test
    void preservesCauseWhenNotificationsAreProvided() {
        HttpErrorResponse error = error("{}");
        Notification notification =
                Notification.error(
                        "E_SERVICE_FRAMEWORK_HTTP_CLIENT_0503",
                        "Service Unavailable received from HTTP client");
        RuntimeException cause = new RuntimeException("transport failed");

        HttpClientResponseException exception =
                new HttpClientResponseException(error, notification, cause);

        assertSame(cause, exception.getCause());
        assertEquals(notification, exception.primaryNotification().orElseThrow());
    }

    @Test
    void notificationsAreImmutableAndDefensivelyCopied() {
        HttpErrorResponse error = error("{}");
        Notification notification =
                Notification.error(
                        "E_SERVICE_FRAMEWORK_HTTP_CLIENT_0503",
                        "Service Unavailable received from HTTP client");

        HttpClientResponseException exception =
                new HttpClientResponseException(error, List.of(notification));

        assertThrows(
                UnsupportedOperationException.class,
                () -> exception.notifications().add(notification));
    }

    @Test
    void jsonErrorResponseRequiresConfiguredBodyReader() {
        HttpErrorResponse error = error("{\"message\":\"bad request\"}");
        HttpClientResponseException exception = new HttpClientResponseException(error);

        HttpErrorResponseBodyReaderNotConfiguredException thrown =
                assertThrows(
                        HttpErrorResponseBodyReaderNotConfiguredException.class,
                        () -> exception.getJsonErrorResponseAsObject(ErrorPayload.class));

        assertSame(error, thrown.error());
        assertEquals(
                "HTTP error response body reader is not configured client=payments method=GET uri=https://payments.example/v1/orders status=503",
                thrown.getMessage());
    }

    @Test
    void genericJsonErrorResponseRequiresConfiguredBodyReader() {
        HttpErrorResponse error = error("{\"message\":\"bad request\"}");
        HttpClientResponseException exception = new HttpClientResponseException(error);

        HttpErrorResponseBodyReaderNotConfiguredException thrown =
                assertThrows(
                        HttpErrorResponseBodyReaderNotConfiguredException.class,
                        () -> exception.getJsonErrorResponseAsObject((Type) ErrorPayload.class));

        assertSame(error, thrown.error());
    }

    @Test
    void jsonErrorResponseRejectsNullClassType() {
        HttpErrorResponse error = error("{\"message\":\"bad request\"}");
        HttpClientResponseException exception =
                new HttpClientResponseException(error)
                        .withErrorResponseBodyReader(
                                new CapturingBodyReader(new ErrorPayload("bad request")));

        NullPointerException thrown =
                assertThrows(
                        NullPointerException.class,
                        () -> exception.getJsonErrorResponseAsObject((Class<ErrorPayload>) null));

        assertEquals("type must not be null", thrown.getMessage());
    }

    @Test
    void genericJsonErrorResponseRejectsNullType() {
        HttpErrorResponse error = error("{\"message\":\"bad request\"}");
        HttpClientResponseException exception =
                new HttpClientResponseException(error)
                        .withErrorResponseBodyReader(
                                new CapturingBodyReader(new ErrorPayload("bad request")));

        NullPointerException thrown =
                assertThrows(
                        NullPointerException.class,
                        () -> exception.getJsonErrorResponseAsObject((Type) null));

        assertEquals("type must not be null", thrown.getMessage());
    }

    @Test
    void withErrorResponseBodyReaderRejectsNullReader() {
        HttpErrorResponse error = error("{\"message\":\"bad request\"}");
        HttpClientResponseException exception = new HttpClientResponseException(error);

        NullPointerException thrown =
                assertThrows(
                        NullPointerException.class,
                        () -> exception.withErrorResponseBodyReader(null));

        assertEquals("errorResponseBodyReader must not be null", thrown.getMessage());
    }

    @Test
    void jsonErrorResponseIsReadWithConfiguredBodyReader() {
        HttpErrorResponse error = error("{\"message\":\"bad request\"}");
        ErrorPayload payload = new ErrorPayload("bad request");
        CapturingBodyReader bodyReader = new CapturingBodyReader(payload);
        HttpClientResponseException exception =
                new HttpClientResponseException(error).withErrorResponseBodyReader(bodyReader);

        ErrorPayload decoded = exception.getJsonErrorResponseAsObject(ErrorPayload.class);

        assertSame(payload, decoded);
        assertSame(error, bodyReader.error);
        assertEquals(ErrorPayload.class, bodyReader.type);
    }

    @Test
    void genericJsonErrorResponseIsReadWithConfiguredBodyReader() {
        HttpErrorResponse error = error("{\"message\":\"bad request\"}");
        ErrorPayload payload = new ErrorPayload("bad request");
        CapturingBodyReader bodyReader = new CapturingBodyReader(payload);
        HttpClientResponseException exception =
                new HttpClientResponseException(error).withErrorResponseBodyReader(bodyReader);
        Type type = ErrorPayload.class;

        ErrorPayload decoded = exception.getJsonErrorResponseAsObject(type);

        assertSame(payload, decoded);
        assertSame(error, bodyReader.error);
        assertEquals(type, bodyReader.type);
    }

    @Test
    void withErrorResponseBodyReaderKeepsOriginalExceptionState() {
        HttpErrorResponse error = error("{\"message\":\"bad request\"}");
        Notification notification =
                Notification.error(
                        "E_SERVICE_FRAMEWORK_HTTP_CLIENT_0503",
                        "Service Unavailable received from HTTP client");
        RuntimeException cause = new RuntimeException("transport failed");
        CapturingBodyReader bodyReader = new CapturingBodyReader(new ErrorPayload("bad request"));

        HttpClientResponseException exception =
                new HttpClientResponseException(error, List.of(notification), cause)
                        .withErrorResponseBodyReader(bodyReader);

        assertSame(error, exception.error());
        assertEquals(List.of(notification), exception.notifications());
        assertSame(cause, exception.getCause());
        assertEquals(
                "HTTP client request failed client=payments method=GET uri=https://payments.example/v1/orders status=503 reason=Service Unavailable",
                exception.getMessage());
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
                false);
    }

    private record ErrorPayload(String message) {}

    private static final class CapturingBodyReader implements HttpErrorResponseBodyReader {

        private final Object payload;
        private HttpErrorResponse error;
        private Type type;

        private CapturingBodyReader(Object payload) {
            this.payload = payload;
        }

        @Override
        public <T> T read(HttpErrorResponse error, Class<T> type) {
            this.error = error;
            this.type = type;
            return type.cast(payload);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T read(HttpErrorResponse error, Type type) {
            this.error = error;
            this.type = type;
            return (T) payload;
        }
    }
}
