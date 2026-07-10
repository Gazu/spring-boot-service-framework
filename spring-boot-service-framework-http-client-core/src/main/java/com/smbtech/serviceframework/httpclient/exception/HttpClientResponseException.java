package com.smbtech.serviceframework.httpclient.exception;

import com.smbtech.serviceframework.commons.notification.Notification;
import com.smbtech.serviceframework.commons.notification.NotifyingException;
import com.smbtech.serviceframework.httpclient.domain.HttpErrorResponse;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class HttpClientResponseException extends NotifyingException {

    private final HttpErrorResponse error;

    public HttpClientResponseException(HttpErrorResponse error) {
        this(error, List.of());
    }

    public HttpClientResponseException(HttpErrorResponse error, Notification notification) {
        this(error, List.of(Objects.requireNonNull(notification, "notification must not be null")));
    }

    public HttpClientResponseException(HttpErrorResponse error, Notification notification, Throwable cause) {
        this(error, List.of(Objects.requireNonNull(notification, "notification must not be null")), cause);
    }

    public HttpClientResponseException(HttpErrorResponse error, List<Notification> notifications) {
        super(notifications, message(error));
        this.error = Objects.requireNonNull(error, "error must not be null");
    }

    public HttpClientResponseException(HttpErrorResponse error, List<Notification> notifications, Throwable cause) {
        super(notifications, message(error), cause);
        this.error = Objects.requireNonNull(error, "error must not be null");
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
