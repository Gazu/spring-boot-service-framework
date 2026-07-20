package com.smbtech.serviceframework.mock.service;

import com.smbtech.serviceframework.mock.domain.MockDefinition;
import com.smbtech.serviceframework.mock.domain.MockRequest;
import com.smbtech.serviceframework.mock.domain.MockResponse;
import com.smbtech.serviceframework.mock.exception.MockException;
import com.smbtech.serviceframework.mock.port.in.MockCatalog;
import com.smbtech.serviceframework.mock.port.in.MockResponder;
import com.smbtech.serviceframework.mock.port.out.MockResponseSource;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/** Provides default mock responder behavior. */
public final class DefaultMockResponder implements MockResponder {

    private final MockCatalog catalog;
    private final MockResponseSource responseSource;
    private final MockDelay mockDelay;

    /**
     * Creates a default mock responder instance.
     *
     * @param catalog catalog value
     * @param responseSource response source value
     */
    public DefaultMockResponder(MockCatalog catalog, MockResponseSource responseSource) {
        this(catalog, responseSource, DefaultMockResponder::sleep);
    }

    DefaultMockResponder(
            MockCatalog catalog, MockResponseSource responseSource, MockDelay mockDelay) {
        this.catalog = Objects.requireNonNull(catalog, "catalog must not be null");
        this.responseSource =
                Objects.requireNonNull(responseSource, "responseSource must not be null");
        this.mockDelay = Objects.requireNonNull(mockDelay, "mockDelay must not be null");
    }

    @Override
    public Optional<MockResponse> respond(MockRequest request) {
        MockRequest normalizedRequest =
                Objects.requireNonNullElseGet(request, () -> new MockRequest(""));
        if (!normalizedRequest.hasKey()) {
            return Optional.empty();
        }

        return catalog.findByKey(normalizedRequest.key())
                .filter(MockDefinition::enabled)
                .map(definition -> load(definition, normalizedRequest));
    }

    private MockResponse load(MockDefinition definition, MockRequest request) {
        if (!definition.isUsable()) {
            throw new MockException(
                    "Mock is enabled but file is empty for key: " + definition.key());
        }

        mockDelay.apply(definition.key(), definition.delay());

        MockResponse response = responseSource.load(definition, request);
        if (response == null) {
            throw new MockException(
                    "Mock response source returned null for key: " + definition.key());
        }
        return response;
    }

    private static void sleep(String key, Duration delay) {
        if (delay == null || delay.isZero() || delay.isNegative()) {
            return;
        }

        try {
            Thread.sleep(delay.toMillis());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new MockException("Mock delay was interrupted for key: " + key, exception);
        }
    }
}

@FunctionalInterface
interface MockDelay {
    void apply(String key, Duration delay);
}
