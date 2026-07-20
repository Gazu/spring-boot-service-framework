package com.smbtech.serviceframework.starter.restclient.adapter.out.error;

import com.smbtech.serviceframework.httpclient.domain.HttpClientDefinition;
import com.smbtech.serviceframework.httpclient.domain.HttpErrorResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;

/** Provides http error response mapper behavior. */
public final class HttpErrorResponseMapper {
    /** Creates a http error response mapper instance. */
    public HttpErrorResponseMapper() {}

    private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    /**
     * Performs the map operation.
     *
     * @param definition definition value
     * @param request request value
     * @param response response value
     * @return map result
     * @throws IOException when the operation cannot be completed
     */
    public HttpErrorResponse map(
            HttpClientDefinition definition, HttpRequest request, ClientHttpResponse response)
            throws IOException {
        HttpHeaders responseHeaders = response.getHeaders();
        int statusCode = response.getStatusCode().value();
        String contentType = contentType(responseHeaders);
        Charset charset = charset(contentType);

        return new HttpErrorResponse(
                definition.name(),
                request.getMethod().name(),
                request.getURI().toString(),
                statusCode,
                response.getStatusText(),
                HttpErrorResponse.categoryOf(statusCode),
                definition.errorHandling().includeHeaders() ? headers(responseHeaders) : Map.of(),
                definition.errorHandling().includeBody() ? body(response.getBody(), charset) : "",
                contentType,
                charset.name(),
                false);
    }

    private Map<String, String> headers(HttpHeaders headers) {
        if (headers == null || headers.isEmpty()) {
            return Map.of();
        }
        Map<String, String> result = new LinkedHashMap<>();
        headers.headerSet()
                .forEach(entry -> result.put(entry.getKey(), headerValue(entry.getValue())));
        return result;
    }

    private String headerValue(Iterable<String> values) {
        StringBuilder value = new StringBuilder();
        for (String item : values) {
            if (item == null) {
                continue;
            }
            if (!value.isEmpty()) {
                value.append(',');
            }
            value.append(item);
        }
        return value.toString();
    }

    private String contentType(HttpHeaders headers) {
        if (headers == null) {
            return "";
        }
        return Objects.requireNonNullElse(headers.getFirst(HttpHeaders.CONTENT_TYPE), "");
    }

    private Charset charset(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return DEFAULT_CHARSET;
        }
        try {
            Charset charset = MediaType.parseMediaType(contentType).getCharset();
            return charset == null ? DEFAULT_CHARSET : charset;
        } catch (IllegalArgumentException exception) {
            return DEFAULT_CHARSET;
        }
    }

    private String body(InputStream inputStream, Charset charset) throws IOException {
        if (inputStream == null) {
            return "";
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        inputStream.transferTo(output);
        return output.toString(Objects.requireNonNullElse(charset, DEFAULT_CHARSET));
    }
}
