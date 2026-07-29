package com.smbtech.serviceframework.starter.restclient.adapter.out.interceptor;

import com.smbtech.serviceframework.httpclient.domain.HttpExchangeAuditFailure;
import com.smbtech.serviceframework.httpclient.domain.HttpExchangeAuditRequest;
import com.smbtech.serviceframework.httpclient.domain.HttpExchangeAuditResponse;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

final class HttpExchangeAuditSanitizer {

    private static final String REDACTED = "<redacted>";

    private static final Set<String> SENSITIVE_HEADERS =
            Set.of(
                    "authorization",
                    "proxy-authorization",
                    "cookie",
                    "set-cookie",
                    "x-api-key",
                    "api-key",
                    "x-auth-token",
                    "x-access-token",
                    "x-csrf-token",
                    "x-amz-security-token");

    private static final String SENSITIVE_NAME =
            "access[_-]?token|refresh[_-]?token|client[_-]?secret|password|passwd|secret|assertion|authorization|api[_-]?key|id[_-]?token|pin";

    private static final Pattern AUTHORIZATION_VALUE =
            Pattern.compile("(?i)\\b(Bearer|Basic)\\s+[^\\s,;]+", Pattern.CASE_INSENSITIVE);

    private static final Pattern JSON_SECRET =
            Pattern.compile(
                    "(?i)(\\\"(?:"
                            + SENSITIVE_NAME
                            + ")\\\"\\s*:\\s*)(\\\"(?:\\\\.|[^\\\"\\\\])*\\\"|[^,}\\s]+)");

    private static final Pattern KEY_VALUE_SECRET =
            Pattern.compile("(?i)(\\b(?:" + SENSITIVE_NAME + ")\\b\\s*[=:]\\s*)([^&\\s,;}]+)");

    private static final Pattern QUERY_SECRET =
            Pattern.compile("(?i)([?&](?:" + SENSITIVE_NAME + ")=)[^&#]*");

    HttpExchangeAuditRequest sanitize(HttpExchangeAuditRequest event) {
        return new HttpExchangeAuditRequest(
                event.method(),
                sanitizeUri(event.uri()),
                sanitizeHeaders(event.headers()),
                sanitizeText(event.body()));
    }

    HttpExchangeAuditResponse sanitize(HttpExchangeAuditResponse event) {
        return new HttpExchangeAuditResponse(
                event.method(),
                sanitizeUri(event.uri()),
                event.statusCode(),
                event.statusText(),
                sanitizeHeaders(event.headers()),
                sanitizeText(event.body()),
                event.duration());
    }

    HttpExchangeAuditFailure sanitize(HttpExchangeAuditFailure event) {
        return new HttpExchangeAuditFailure(
                event.method(),
                sanitizeUri(event.uri()),
                event.statusCode(),
                event.statusText(),
                sanitizeHeaders(event.headers()),
                sanitizeText(event.requestBody()),
                sanitizeText(event.responseBody()),
                event.duration(),
                event.exceptionType(),
                sanitizeText(event.exceptionMessage()),
                null);
    }

    private Map<String, String> sanitizeHeaders(Map<String, String> headers) {
        if (headers.isEmpty()) {
            return Map.of();
        }
        Map<String, String> sanitized = new LinkedHashMap<>();
        headers.forEach(
                (name, value) ->
                        sanitized.put(
                                name, isSensitiveHeader(name) ? REDACTED : sanitizeText(value)));
        return Map.copyOf(sanitized);
    }

    private boolean isSensitiveHeader(String name) {
        return name != null && SENSITIVE_HEADERS.contains(name.toLowerCase(Locale.ROOT));
    }

    private String sanitizeUri(String uri) {
        if (uri == null || uri.isEmpty()) {
            return "";
        }
        return QUERY_SECRET.matcher(uri).replaceAll("$1" + REDACTED);
    }

    private String sanitizeText(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        String sanitized = JSON_SECRET.matcher(value).replaceAll("$1\"" + REDACTED + "\"");
        sanitized = KEY_VALUE_SECRET.matcher(sanitized).replaceAll("$1" + REDACTED);
        return AUTHORIZATION_VALUE.matcher(sanitized).replaceAll("$1 " + REDACTED);
    }
}
