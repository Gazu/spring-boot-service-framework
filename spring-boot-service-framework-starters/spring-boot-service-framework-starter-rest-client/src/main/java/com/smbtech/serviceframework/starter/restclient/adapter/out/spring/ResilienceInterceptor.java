package com.smbtech.serviceframework.starter.restclient.adapter.out.spring;

import com.smbtech.serviceframework.httpclient.domain.HttpClientDefinition;
import com.smbtech.serviceframework.httpclient.domain.ResiliencePolicy;
import com.smbtech.serviceframework.httpclient.domain.RetryPolicy;
import com.smbtech.serviceframework.httpclient.exception.CircuitBreakerOpenException;
import com.smbtech.serviceframework.httpclient.exception.HttpClientResponseException;
import java.io.IOException;
import java.io.InterruptedIOException;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

/** Provides resilience interceptor behavior. */
final class ResilienceInterceptor implements ClientHttpRequestInterceptor {

    private final HttpClientDefinition definition;
    private final ResilienceStateRegistry stateRegistry;

    /**
     * Creates a resilience interceptor instance.
     *
     * @param definition definition value
     * @param stateRegistry state registry value
     */
    public ResilienceInterceptor(
            HttpClientDefinition definition, ResilienceStateRegistry stateRegistry) {
        this.definition = definition;
        this.stateRegistry = stateRegistry;
    }

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        ResiliencePolicy resilience = definition.resilience();
        if (!resilience.enabled()) {
            return execution.execute(request, body);
        }

        RetryPolicy retry = resilience.retry();
        int maxAttempts = retry.enabled() ? retry.maxAttempts() : 1;
        int attempt = 1;

        while (true) {
            stateRegistry.circuitBreaker(definition).beforeCall();

            try {
                ClientHttpResponse response = execution.execute(request, body);
                if (isRetryableStatus(response, retry)) {
                    stateRegistry.circuitBreaker(definition).recordFailure();
                    if (attempt < maxAttempts) {
                        response.close();
                        sleep(retry, attempt++);
                        continue;
                    }
                } else {
                    stateRegistry.circuitBreaker(definition).recordSuccess();
                }
                return response;
            } catch (IOException | RuntimeException exception) {
                stateRegistry.circuitBreaker(definition).recordFailure();
                if (attempt >= maxAttempts || !isRetryableException(exception, retry)) {
                    throw exception;
                }
                sleep(retry, attempt++);
            }
        }
    }

    private boolean isRetryableStatus(ClientHttpResponse response, RetryPolicy retry)
            throws IOException {
        return retry.enabled() && retry.shouldRetryStatus(response.getStatusCode().value());
    }

    private boolean isRetryableException(Exception exception, RetryPolicy retry) {
        if (!retry.enabled()
                || !retry.retryOnExceptions()
                || exception instanceof CircuitBreakerOpenException) {
            return false;
        }
        if (exception instanceof HttpClientResponseException responseException) {
            return retry.shouldRetryStatus(responseException.statusCode());
        }
        return true;
    }

    private void sleep(RetryPolicy retry, int attempt) throws InterruptedIOException {
        if (retry.backoff().isZero()) {
            return;
        }
        try {
            Thread.sleep(retry.backoff().toMillis() * attempt);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            InterruptedIOException interrupted =
                    new InterruptedIOException("Interrupted during HTTP client retry backoff");
            interrupted.initCause(exception);
            throw interrupted;
        }
    }
}
