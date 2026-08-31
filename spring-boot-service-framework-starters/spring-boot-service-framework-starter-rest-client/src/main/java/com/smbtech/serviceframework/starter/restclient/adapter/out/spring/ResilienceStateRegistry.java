package com.smbtech.serviceframework.starter.restclient.adapter.out.spring;

import com.smbtech.serviceframework.httpclient.domain.CircuitBreakerPolicy;
import com.smbtech.serviceframework.httpclient.domain.HttpClientDefinition;
import com.smbtech.serviceframework.httpclient.exception.CircuitBreakerOpenException;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class ResilienceStateRegistry {

    private final Clock clock;
    private final Map<String, CircuitBreakerState> circuitBreakers = new ConcurrentHashMap<>();

    /**
     * Creates a resilience state registry instance.
     *
     * @param clock clock value
     */
    ResilienceStateRegistry(Clock clock) {
        this.clock = clock;
    }

    /**
     * Performs the circuit breaker operation.
     *
     * @param definition definition value
     * @return circuit breaker result
     */
    CircuitBreakerState circuitBreaker(HttpClientDefinition definition) {
        return circuitBreakers.computeIfAbsent(
                definition.name(),
                key ->
                        new CircuitBreakerState(
                                definition.name(),
                                definition.resilience().circuitBreaker(),
                                clock));
    }

    /** Provides circuit breaker state behavior. */
    static final class CircuitBreakerState {
        private final String clientName;
        private final CircuitBreakerPolicy policy;
        private final Clock clock;
        private int consecutiveFailures;
        private Instant openedAt;
        private boolean halfOpenTrialInProgress;

        private CircuitBreakerState(String clientName, CircuitBreakerPolicy policy, Clock clock) {
            this.clientName = clientName;
            this.policy = policy;
            this.clock = clock;
        }

        /** Performs the before call operation. */
        synchronized void beforeCall() {
            if (!policy.enabled()) {
                return;
            }
            if (openedAt == null) {
                return;
            }
            if (clock.instant().isBefore(openedAt.plus(policy.openDuration()))) {
                throw new CircuitBreakerOpenException(clientName);
            }
            if (halfOpenTrialInProgress) {
                throw new CircuitBreakerOpenException(clientName);
            }
            halfOpenTrialInProgress = true;
        }

        /** Performs the record success operation. */
        synchronized void recordSuccess() {
            if (!policy.enabled()) {
                return;
            }
            consecutiveFailures = 0;
            openedAt = null;
            halfOpenTrialInProgress = false;
        }

        /** Performs the record failure operation. */
        synchronized void recordFailure() {
            if (!policy.enabled()) {
                return;
            }
            halfOpenTrialInProgress = false;
            consecutiveFailures++;
            if (openedAt != null || consecutiveFailures >= policy.failureThreshold()) {
                openedAt = clock.instant();
            }
        }
    }
}
