package com.smbtech.serviceframework.starter.restclient.adapter.out.interceptor;

import com.smbtech.serviceframework.httpclient.domain.HttpClientDefinition;
import com.smbtech.serviceframework.httpclient.domain.ObservabilityPolicy;
import com.smbtech.serviceframework.httpclient.exception.HttpClientResponseException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

/** Provides micrometer http client observation interceptor behavior. */
public final class MicrometerHttpClientObservationInterceptor
        implements ClientHttpRequestInterceptor {

    private static final String UNKNOWN = "unknown";
    private static final String NONE = "none";

    private final HttpClientDefinition definition;
    private final MeterRegistry meterRegistry;

    /**
     * Creates a micrometer http client observation interceptor instance.
     *
     * @param definition definition value
     * @param meterRegistry meter registry value
     */
    public MicrometerHttpClientObservationInterceptor(
            HttpClientDefinition definition, MeterRegistry meterRegistry) {
        this.definition = definition;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        long startedAt = System.nanoTime();
        int statusCode = -1;
        String exception = NONE;

        try {
            ClientHttpResponse response = execution.execute(request, body);
            statusCode = response.getStatusCode().value();
            record(request, startedAt, statusCode, exception);
            return response;
        } catch (HttpClientResponseException responseException) {
            statusCode = responseException.statusCode();
            exception = exceptionName(responseException);
            record(request, startedAt, statusCode, exception);
            throw responseException;
        } catch (IOException | RuntimeException throwable) {
            exception = exceptionName(throwable);
            record(request, startedAt, statusCode, exception);
            throw throwable;
        }
    }

    private void record(HttpRequest request, long startedAt, int statusCode, String exception) {
        ObservabilityPolicy policy = definition.observability();
        if (!policy.enabled() || meterRegistry == null) {
            return;
        }

        Tags tags = tags(request, statusCode, exception);
        Duration duration = Duration.ofNanos(System.nanoTime() - startedAt);

        Timer.builder(policy.metricName())
                .description("HTTP client request duration")
                .tags(tags)
                .register(meterRegistry)
                .record(duration.toNanos(), TimeUnit.NANOSECONDS);

        if (!NONE.equals(exception) || statusCode >= 400) {
            Counter.builder(policy.metricName() + ".errors")
                    .description("HTTP client request errors")
                    .tags(tags)
                    .register(meterRegistry)
                    .increment();
        }
    }

    private Tags tags(HttpRequest request, int statusCode, String exception) {
        ObservabilityPolicy policy = definition.observability();
        Tags tags =
                Tags.of(
                        "client", definition.name(),
                        "method", request.getMethod().name(),
                        "outcome", outcome(statusCode, exception));

        if (policy.includeStatus()) {
            tags = tags.and("status", statusCode > 0 ? String.valueOf(statusCode) : UNKNOWN);
        }
        if (policy.includeException()) {
            tags = tags.and("exception", exception);
        }
        if (policy.includeUri()) {
            tags =
                    tags.and(
                            "uri",
                            request.getURI().getPath().isBlank()
                                    ? "/"
                                    : request.getURI().getPath());
        }
        for (Map.Entry<String, String> entry : policy.tags().entrySet()) {
            if (entry.getKey() != null && !entry.getKey().isBlank() && entry.getValue() != null) {
                tags = tags.and(entry.getKey(), entry.getValue());
            }
        }
        return tags;
    }

    private String outcome(int statusCode, String exception) {
        if (!NONE.equals(exception) && statusCode <= 0) {
            return "EXCEPTION";
        }
        if (statusCode >= 100 && statusCode <= 199) {
            return "INFORMATIONAL";
        }
        if (statusCode >= 200 && statusCode <= 299) {
            return "SUCCESS";
        }
        if (statusCode >= 300 && statusCode <= 399) {
            return "REDIRECTION";
        }
        if (statusCode >= 400 && statusCode <= 499) {
            return "CLIENT_ERROR";
        }
        if (statusCode >= 500 && statusCode <= 599) {
            return "SERVER_ERROR";
        }
        return UNKNOWN.toUpperCase();
    }

    private String exceptionName(Throwable throwable) {
        return throwable == null ? NONE : throwable.getClass().getSimpleName();
    }
}
