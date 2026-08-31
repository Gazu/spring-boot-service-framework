package com.smbtech.serviceframework.starter.restclient.adapter.out.spring;

import com.smbtech.serviceframework.httpclient.domain.HttpClientDefinition;
import com.smbtech.serviceframework.httpclient.domain.HttpErrorResponse;
import com.smbtech.serviceframework.httpclient.domain.HttpExchangeAuditFailure;
import com.smbtech.serviceframework.httpclient.domain.HttpExchangeAuditRequest;
import com.smbtech.serviceframework.httpclient.domain.HttpExchangeAuditResponse;
import com.smbtech.serviceframework.httpclient.exception.HttpClientResponseException;
import com.smbtech.serviceframework.httpclient.port.out.HttpExchangeAuditSink;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

/** Provides audit log interceptor behavior. */
final class AuditLogInterceptor implements ClientHttpRequestInterceptor {

    private static final String TRUNCATED_SUFFIX = "...[truncated]";

    private final HttpClientDefinition definition;
    private final HttpExchangeAuditSink auditSink;
    private final HttpExchangeAuditSanitizer sanitizer = new HttpExchangeAuditSanitizer();

    /**
     * Creates a audit log interceptor instance.
     *
     * @param definition definition value
     * @param auditSink audit sink value
     */
    public AuditLogInterceptor(HttpClientDefinition definition, HttpExchangeAuditSink auditSink) {
        this.definition = definition;
        this.auditSink = auditSink;
    }

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        long startedAt = System.nanoTime();
        String method = request.getMethod().name();
        String uri = request.getURI().toString();
        String requestBody = body(body);

        try {
            if (definition.audit().includeRequest()) {
                auditSink.request(
                        definition,
                        sanitizer.sanitize(
                                new HttpExchangeAuditRequest(
                                        method, uri, headers(request), requestBody)));
            }

            ClientHttpResponse response = execution.execute(request, body);

            if (definition.audit().includeResponse()) {
                auditSink.response(
                        definition,
                        sanitizer.sanitize(
                                new HttpExchangeAuditResponse(
                                        method,
                                        uri,
                                        response.getStatusCode().value(),
                                        response.getStatusText(),
                                        headers(response),
                                        body(response),
                                        durationSince(startedAt))));
            }
            return response;
        } catch (IOException | RuntimeException exception) {
            auditSink.failure(
                    definition,
                    sanitizer.sanitize(failure(method, uri, requestBody, startedAt, exception)));
            throw exception;
        }
    }

    private Map<String, String> headers(HttpRequest request) {
        if (!definition.audit().includeHeaders()) {
            return Map.of();
        }
        return flatten(request.getHeaders());
    }

    private Map<String, String> headers(ClientHttpResponse response) {
        if (!definition.audit().includeHeaders()) {
            return Map.of();
        }
        return flatten(response.getHeaders());
    }

    private String body(byte[] body) {
        if (!definition.audit().includeBody() || body == null || body.length == 0) {
            return "";
        }
        return truncate(new String(body, StandardCharsets.UTF_8));
    }

    private String body(ClientHttpResponse response) throws IOException {
        if (!definition.audit().includeBody()) {
            return "";
        }
        return body(response.getBody());
    }

    private String body(InputStream inputStream) throws IOException {
        int limit = Math.max(1, definition.audit().maxBodySize());
        ByteArrayOutputStream output = new ByteArrayOutputStream(Math.min(limit, 1024));
        byte[] buffer = new byte[Math.min(limit + 1, 1024)];
        int total = 0;
        boolean truncated = false;

        while (total <= limit) {
            int remaining = limit + 1 - total;
            int read = inputStream.read(buffer, 0, Math.min(buffer.length, remaining));
            if (read == -1) {
                break;
            }
            int accepted = Math.min(read, Math.max(0, limit - total));
            if (accepted > 0) {
                output.write(buffer, 0, accepted);
            }
            total += read;
            if (total > limit) {
                truncated = true;
                break;
            }
        }

        String value = output.toString(StandardCharsets.UTF_8);
        return truncated ? value + TRUNCATED_SUFFIX : value;
    }

    private String truncate(String value) {
        int limit = Math.max(1, definition.audit().maxBodySize());
        if (value.length() <= limit) {
            return value;
        }
        return value.substring(0, limit) + TRUNCATED_SUFFIX;
    }

    private HttpExchangeAuditFailure failure(
            String method, String uri, String requestBody, long startedAt, Exception exception) {
        if (exception instanceof HttpClientResponseException responseException) {
            HttpErrorResponse error = responseException.error();
            return new HttpExchangeAuditFailure(
                    method,
                    uri,
                    error.statusCode(),
                    error.reasonPhrase(),
                    definition.audit().includeHeaders() ? error.headers() : Map.of(),
                    requestBody,
                    definition.audit().includeBody() ? truncate(error.body()) : "",
                    durationSince(startedAt),
                    exception.getClass().getName(),
                    exception.getMessage(),
                    exception);
        }

        return new HttpExchangeAuditFailure(
                method,
                uri,
                HttpExchangeAuditFailure.STATUS_CODE_UNAVAILABLE,
                "",
                Map.of(),
                requestBody,
                "",
                durationSince(startedAt),
                exception.getClass().getName(),
                exception.getMessage(),
                exception);
    }

    private Duration durationSince(long startedAt) {
        return Duration.ofNanos(System.nanoTime() - startedAt);
    }

    private Map<String, String> flatten(HttpHeaders headers) {
        Map<String, String> result = new LinkedHashMap<>();
        headers.headerSet()
                .forEach(entry -> result.put(entry.getKey(), String.join(",", entry.getValue())));
        return result;
    }
}
