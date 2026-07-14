package com.smbtech.serviceframework.httpclient.exception;

import com.smbtech.serviceframework.commons.notification.Notification;
import com.smbtech.serviceframework.commons.notification.NotifyingException;
import com.smbtech.serviceframework.httpclient.domain.HttpErrorResponse;
import com.smbtech.serviceframework.httpclient.port.out.HttpErrorResponseBodyReader;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class HttpClientResponseException extends NotifyingException {

    private final HttpErrorResponse error;
    private final HttpErrorResponseBodyReader errorResponseBodyReader;

    public HttpClientResponseException(HttpErrorResponse error) {
        this(error, List.of());
    }

    public HttpClientResponseException(HttpErrorResponse error, Notification notification) {
        this(error, List.of(Objects.requireNonNull(notification, "notification must not be null")));
    }

    public HttpClientResponseException(HttpErrorResponse error, Notification notification, Throwable cause) {
        this(error, List.of(Objects.requireNonNull(notification, "notification must not be null")), cause);
    }

    public HttpClientResponseException(
            HttpErrorResponse error,
            Notification notification,
            Throwable cause,
            HttpErrorResponseBodyReader errorResponseBodyReader
    ) {
        this(
                error,
                List.of(Objects.requireNonNull(notification, "notification must not be null")),
                cause,
                errorResponseBodyReader
        );
    }

    public HttpClientResponseException(HttpErrorResponse error, List<Notification> notifications) {
        super(notifications, message(error));
        this.error = Objects.requireNonNull(error, "error must not be null");
        this.errorResponseBodyReader = null;
    }

    public HttpClientResponseException(HttpErrorResponse error, List<Notification> notifications, Throwable cause) {
        this(error, notifications, cause, null);
    }

    public HttpClientResponseException(
            HttpErrorResponse error,
            List<Notification> notifications,
            Throwable cause,
            HttpErrorResponseBodyReader errorResponseBodyReader
    ) {
        super(notifications, message(error), cause);
        this.error = Objects.requireNonNull(error, "error must not be null");
        this.errorResponseBodyReader = errorResponseBodyReader;
    }

    public HttpClientResponseException withErrorResponseBodyReader(
            HttpErrorResponseBodyReader errorResponseBodyReader
    ) {
        return new HttpClientResponseException(
                error,
                notifications(),
                getCause(),
                Objects.requireNonNull(errorResponseBodyReader, "errorResponseBodyReader must not be null")
        );
    }

    public HttpErrorResponse error() {
        return error;
    }

    public int statusCode() {
        return error.statusCode();
    }

    public String responseBody() {
        return error.body();
    }

    public String getErrorResponseAsString() {
        return responseBody();
    }

    public <T> T getJsonErrorResponseAsObject(Class<T> type) {
        return requireErrorResponseBodyReader().read(error, Objects.requireNonNull(type, "type must not be null"));
    }

    public <T> T getJsonErrorResponseAsObject(Type type) {
        return requireErrorResponseBodyReader().read(error, Objects.requireNonNull(type, "type must not be null"));
    }

    public Map<String, String> responseHeaders() {
        return error.headers();
    }

    public String responseCharset() {
        return error.charset();
    }

    public String responseContentType() {
        return error.contentType();
    }

    public String responseStatusText() {
        return error.reasonPhrase();
    }

    public boolean isResponseBodyTruncated() {
        return error.bodyTruncated();
    }

    private HttpErrorResponseBodyReader requireErrorResponseBodyReader() {
        if (errorResponseBodyReader == null) {
            throw new HttpErrorResponseBodyReaderNotConfiguredException(error);
        }
        return errorResponseBodyReader;
    }

    private static String message(HttpErrorResponse error) {
        Objects.requireNonNull(error, "error must not be null");
        return "HTTP client request failed"
                + " client=" + error.clientName()
                + " method=" + error.method()
                + " uri=" + error.uri()
                + " status=" + error.statusCode()
                + (error.reasonPhrase().isBlank() ? "" : " reason=" + error.reasonPhrase());
    }
}
