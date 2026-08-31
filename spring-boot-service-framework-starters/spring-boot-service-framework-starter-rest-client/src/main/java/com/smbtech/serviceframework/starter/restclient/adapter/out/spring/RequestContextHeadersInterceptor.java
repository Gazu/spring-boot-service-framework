package com.smbtech.serviceframework.starter.restclient.adapter.out.spring;

import com.smbtech.serviceframework.starter.restclient.api.RequestContextManager;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

/** Provides request context headers interceptor behavior. */
final class RequestContextHeadersInterceptor implements ClientHttpRequestInterceptor {

    private static final Pattern HEADER_NAME = Pattern.compile("^[!#$%&'*+.^_`|~0-9A-Za-z-]+$");
    private static final Set<String> DEFAULT_BLOCKED_HEADERS =
            Set.of(
                    "authorization",
                    "proxy-authorization",
                    "cookie",
                    "set-cookie",
                    "host",
                    "content-length",
                    "transfer-encoding",
                    "connection");

    private final RequestContextManager requestContextManager;
    private final Set<String> blockedHeaders;

    /**
     * Creates a request context headers interceptor instance.
     *
     * @param requestContextManager request context manager value
     */
    public RequestContextHeadersInterceptor(RequestContextManager requestContextManager) {
        this(requestContextManager, Set.of());
    }

    /**
     * Creates a request context headers interceptor instance.
     *
     * @param requestContextManager request context manager value
     * @param blockedHeaders blocked headers value
     */
    public RequestContextHeadersInterceptor(
            RequestContextManager requestContextManager, Set<String> blockedHeaders) {
        this.requestContextManager =
                Objects.requireNonNull(
                        requestContextManager, "requestContextManager must not be null");
        this.blockedHeaders = blockedHeaders(blockedHeaders);
    }

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        requestContextManager
                .currentHeaders()
                .forEach(
                        (name, value) -> {
                            if (isSafeHeader(name, value)
                                    && !request.getHeaders().containsHeader(name)) {
                                request.getHeaders().add(name, value);
                            }
                        });
        return execution.execute(request, body);
    }

    private boolean isSafeHeader(String name, String value) {
        return name != null
                && value != null
                && HEADER_NAME.matcher(name).matches()
                && !value.isBlank()
                && !containsLineBreak(value)
                && !blockedHeaders.contains(normalize(name));
    }

    private boolean containsLineBreak(String value) {
        return value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0;
    }

    private Set<String> blockedHeaders(Set<String> configuredHeaders) {
        LinkedHashSet<String> blocked = new LinkedHashSet<>(DEFAULT_BLOCKED_HEADERS);
        Objects.requireNonNullElse(configuredHeaders, Set.<String>of())
                .forEach(
                        header -> {
                            if (header != null && !header.isBlank()) {
                                blocked.add(normalize(header));
                            }
                        });
        return Collections.unmodifiableSet(blocked);
    }

    private String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
