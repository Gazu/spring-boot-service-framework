package com.smbtech.serviceframework.httpclient.exception;

import com.smbtech.serviceframework.commons.notification.Notification;
import com.smbtech.serviceframework.commons.notification.NotifyingException;
import com.smbtech.serviceframework.httpclient.domain.HttpErrorResponse;
import com.smbtech.serviceframework.httpclient.port.out.HttpErrorResponseBodyReader;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Raised for a non-success downstream HTTP response while retaining its safe response snapshot and
 * optional body decoder.
 *
 * @serial exclude
 */
public class HttpClientResponseException extends NotifyingException {

    private final HttpErrorResponse error;
    private final HttpErrorResponseBodyReader errorResponseBodyReader;

    /**
     * Creates a http client response exception instance.
     *
     * @param error error value
     */
    public HttpClientResponseException(HttpErrorResponse error) {
        this(error, List.of());
    }

    /**
     * Creates a http client response exception instance.
     *
     * @param error error value
     * @param notification notification value
     */
    public HttpClientResponseException(HttpErrorResponse error, Notification notification) {
        this(error, List.of(Objects.requireNonNull(notification, "notification must not be null")));
    }

    /**
     * Creates a http client response exception instance.
     *
     * @param error error value
     * @param notification notification value
     * @param cause cause value
     */
    public HttpClientResponseException(
            HttpErrorResponse error, Notification notification, Throwable cause) {
        this(
                error,
                List.of(Objects.requireNonNull(notification, "notification must not be null")),
                cause);
    }

    /**
     * Creates a http client response exception instance.
     *
     * @param error error value
     * @param notification notification value
     * @param cause cause value
     * @param errorResponseBodyReader error response body reader value
     */
    public HttpClientResponseException(
            HttpErrorResponse error,
            Notification notification,
            Throwable cause,
            HttpErrorResponseBodyReader errorResponseBodyReader) {
        this(
                error,
                List.of(Objects.requireNonNull(notification, "notification must not be null")),
                cause,
                errorResponseBodyReader);
    }

    /**
     * Creates a http client response exception instance.
     *
     * @param error error value
     * @param notifications notifications value
     */
    public HttpClientResponseException(HttpErrorResponse error, List<Notification> notifications) {
        super(notifications, message(error));
        this.error = Objects.requireNonNull(error, "error must not be null");
        this.errorResponseBodyReader = null;
    }

    /**
     * Creates a http client response exception instance.
     *
     * @param error error value
     * @param notifications notifications value
     * @param cause cause value
     */
    public HttpClientResponseException(
            HttpErrorResponse error, List<Notification> notifications, Throwable cause) {
        this(error, notifications, cause, null);
    }

    /**
     * Creates a http client response exception instance.
     *
     * @param error error value
     * @param notifications notifications value
     * @param cause cause value
     * @param errorResponseBodyReader error response body reader value
     */
    public HttpClientResponseException(
            HttpErrorResponse error,
            List<Notification> notifications,
            Throwable cause,
            HttpErrorResponseBodyReader errorResponseBodyReader) {
        super(notifications, message(error), cause);
        this.error = Objects.requireNonNull(error, "error must not be null");
        this.errorResponseBodyReader = errorResponseBodyReader;
    }

    /**
     * Performs the with error response body reader operation.
     *
     * @param errorResponseBodyReader error response body reader value
     * @return with error response body reader result
     */
    public HttpClientResponseException withErrorResponseBodyReader(
            HttpErrorResponseBodyReader errorResponseBodyReader) {
        return new HttpClientResponseException(
                error,
                notifications(),
                getCause(),
                Objects.requireNonNull(
                        errorResponseBodyReader, "errorResponseBodyReader must not be null"));
    }

    /**
     * Performs the error operation.
     *
     * @return error result
     */
    public HttpErrorResponse error() {
        return error;
    }

    /**
     * Performs the status code operation.
     *
     * @return status code result
     */
    public int statusCode() {
        return error.statusCode();
    }

    /**
     * Performs the response body operation.
     *
     * @return response body result
     */
    public String responseBody() {
        return error.body();
    }

    /**
     * Returns the configured error response as string.
     *
     * @return get error response as string result
     */
    public String getErrorResponseAsString() {
        return responseBody();
    }

    /**
     * Returns the configured json error response as object.
     *
     * @param type type value
     * @return get json error response as object result
     * @param <T> generic value type
     */
    public <T> T getJsonErrorResponseAsObject(Class<T> type) {
        return requireErrorResponseBodyReader()
                .read(error, Objects.requireNonNull(type, "type must not be null"));
    }

    /**
     * Returns the configured json error response as object.
     *
     * @param type type value
     * @return get json error response as object result
     * @param <T> generic value type
     */
    public <T> T getJsonErrorResponseAsObject(Type type) {
        return requireErrorResponseBodyReader()
                .read(error, Objects.requireNonNull(type, "type must not be null"));
    }

    /**
     * Performs the response headers operation.
     *
     * @return response headers result
     */
    public Map<String, String> responseHeaders() {
        return error.headers();
    }

    /**
     * Performs the response charset operation.
     *
     * @return response charset result
     */
    public String responseCharset() {
        return error.charset();
    }

    /**
     * Performs the response content type operation.
     *
     * @return response content type result
     */
    public String responseContentType() {
        return error.contentType();
    }

    /**
     * Performs the response status text operation.
     *
     * @return response status text result
     */
    public String responseStatusText() {
        return error.reasonPhrase();
    }

    /**
     * Reports whether response body truncated.
     *
     * @return is response body truncated result
     */
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
                + " client="
                + error.clientName()
                + " method="
                + error.method()
                + " uri="
                + error.uri()
                + " status="
                + error.statusCode()
                + (error.reasonPhrase().isBlank() ? "" : " reason=" + error.reasonPhrase());
    }
}
