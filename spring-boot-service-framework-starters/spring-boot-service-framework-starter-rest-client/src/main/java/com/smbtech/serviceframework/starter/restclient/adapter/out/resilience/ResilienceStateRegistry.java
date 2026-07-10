package com.smbtech.serviceframework.starter.restclient.adapter.out.resilience;

import com.smbtech.serviceframework.httpclient.domain.CircuitBreakerPolicy;
import com.smbtech.serviceframework.httpclient.domain.HttpClientDefinition;
import com.smbtech.serviceframework.httpclient.exception.CircuitBreakerOpenException;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ResilienceStateRegistry {

    private final Clock clock;
    private final Map<String, CircuitBreakerState> circuitBreakers = new ConcurrentHashMap<>();

    public ResilienceStateRegistry(Clock clock) {
        this.clock = clock;
    }

    public CircuitBreakerState circuitBreaker(HttpClientDefinition definition) {
        return circuitBreakers.computeIfAbsent(
                definition.name(),
                key -> new CircuitBreakerState(definition.name(), definition.resilience().circuitBreaker(), clock)
        );
    }

    public static final class CircuitBreakerState {
        private final String clientName;
        private final CircuitBreakerPolicy policy;
        private final Clock clock;
        private int consecutiveFailures;
        private Instant openedAt;
        private boolean halfOpenTrialInProgress;

        private CircuitBreakerState(
                String clientName,
                CircuitBreakerPolicy policy,
                Clock clock
        ) {
            this.clientName = clientName;
            this.policy = policy;
            this.clock = clock;
        }

        public synchronized void beforeCall() {
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

        public synchronized void recordSuccess() {
            if (!policy.enabled()) {
                return;
            }
            consecutiveFailures = 0;
            openedAt = null;
            halfOpenTrialInProgress = false;
        }

        public synchronized void recordFailure() {
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
