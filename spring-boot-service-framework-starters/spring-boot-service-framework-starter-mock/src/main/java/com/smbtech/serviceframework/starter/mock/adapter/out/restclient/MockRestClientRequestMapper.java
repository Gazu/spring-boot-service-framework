package com.smbtech.serviceframework.starter.mock.adapter.out.restclient;

import com.smbtech.serviceframework.mock.domain.MockRequest;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.HttpRequest;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

/** Provides mock rest client request mapper behavior. */
final class MockRestClientRequestMapper {

    /** Header consulted when selecting the configured mock definition. */
    public static final String DEFAULT_MOCK_KEY_HEADER = "X-Mock-Key";

    private final String mockKeyHeader;

    /** Creates a mock rest client request mapper instance. */
    MockRestClientRequestMapper() {
        this(DEFAULT_MOCK_KEY_HEADER);
    }

    /**
     * Creates a mock rest client request mapper instance.
     *
     * @param mockKeyHeader mock key header value
     */
    MockRestClientRequestMapper(String mockKeyHeader) {
        this.mockKeyHeader =
                Objects.requireNonNullElse(mockKeyHeader, DEFAULT_MOCK_KEY_HEADER).trim();
    }

    /**
     * Performs the to mock request operation.
     *
     * @param request request value
     * @param body body value
     * @return to mock request result
     */
    public MockRequest toMockRequest(HttpRequest request, byte[] body) {
        Objects.requireNonNull(request, "request must not be null");
        URI uri = request.getURI();

        return new MockRequest(
                resolveKey(request, uri),
                request.getMethod().name(),
                normalizePath(uri.getPath()),
                headers(request),
                queryParams(uri),
                body,
                attributes(request, uri));
    }

    private String resolveKey(HttpRequest request, URI uri) {
        String headerKey = request.getHeaders().getFirst(mockKeyHeader);
        if (headerKey != null && !headerKey.isBlank()) {
            return headerKey;
        }
        return normalizePathAsKey(uri.getPath());
    }

    private String normalizePathAsKey(String path) {
        String normalizedPath = normalizePath(path);
        if (normalizedPath.isBlank() || "/".equals(normalizedPath)) {
            return "";
        }
        return normalizedPath.startsWith("/") ? normalizedPath.substring(1) : normalizedPath;
    }

    private String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }
        return path.trim();
    }

    private Map<String, List<String>> headers(HttpRequest request) {
        Map<String, List<String>> headers = new LinkedHashMap<>();
        request.getHeaders().forEach((name, values) -> headers.put(name, List.copyOf(values)));
        return headers;
    }

    private Map<String, List<String>> queryParams(URI uri) {
        MultiValueMap<String, String> queryParams =
                UriComponentsBuilder.fromUri(uri).build().getQueryParams();
        Map<String, List<String>> result = new LinkedHashMap<>();
        queryParams.forEach((name, values) -> result.put(name, List.copyOf(values)));
        return result;
    }

    private Map<String, Object> attributes(HttpRequest request, URI uri) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("uri", uri.toString());
        attributes.put("scheme", Objects.requireNonNullElse(uri.getScheme(), ""));
        attributes.put("host", Objects.requireNonNullElse(uri.getHost(), ""));
        attributes.put("port", uri.getPort());
        attributes.putAll(request.getAttributes());
        return attributes;
    }
}
