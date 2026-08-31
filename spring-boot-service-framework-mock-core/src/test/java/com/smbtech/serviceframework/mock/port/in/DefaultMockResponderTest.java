package com.smbtech.serviceframework.mock.port.in;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.smbtech.serviceframework.mock.domain.MockDefinition;
import com.smbtech.serviceframework.mock.domain.MockRequest;
import com.smbtech.serviceframework.mock.domain.MockResponse;
import com.smbtech.serviceframework.mock.exception.MockException;
import com.smbtech.serviceframework.mock.port.out.MockResponseSource;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class DefaultMockResponderTest {

    @Test
    void returnsEmptyWhenRequestHasNoKey() {
        AtomicInteger calls = new AtomicInteger();
        DefaultMockResponder responder =
                new DefaultMockResponder(
                        catalog(Map.of()),
                        (definition, request) -> {
                            calls.incrementAndGet();
                            return MockResponse.ok(new byte[0]);
                        });

        assertTrue(responder.respond(new MockRequest("")).isEmpty());
        assertTrue(responder.respond(null).isEmpty());
        assertEquals(0, calls.get());
    }

    @Test
    void returnsEmptyWhenMockIsMissingOrDisabled() {
        AtomicInteger calls = new AtomicInteger();
        DefaultMockResponder responder =
                new DefaultMockResponder(
                        catalog(
                                Map.of(
                                        "disabled",
                                        new MockDefinition(
                                                "disabled",
                                                false,
                                                "classpath:mocks/disabled.json",
                                                Duration.ZERO))),
                        (definition, request) -> {
                            calls.incrementAndGet();
                            return MockResponse.ok(new byte[0]);
                        });

        assertTrue(responder.respond(new MockRequest("missing")).isEmpty());
        assertTrue(responder.respond(new MockRequest("disabled")).isEmpty());
        assertEquals(0, calls.get());
    }

    @Test
    void throwsWhenEnabledMockIsNotUsable() {
        DefaultMockResponder responder =
                new DefaultMockResponder(
                        catalog(
                                Map.of(
                                        "broken",
                                        new MockDefinition("broken", true, "", Duration.ZERO))),
                        (definition, request) -> MockResponse.ok(new byte[0]));

        MockException exception =
                assertThrows(
                        MockException.class, () -> responder.respond(new MockRequest("broken")));
        assertEquals("Mock is enabled but file is empty for key: broken", exception.getMessage());
    }

    @Test
    void loadsConfiguredMockResponseAndAppliesDefinitionDelay() {
        AtomicReference<String> delayedKey = new AtomicReference<>();
        AtomicReference<Duration> delayedDuration = new AtomicReference<>();
        AtomicReference<MockDefinition> loadedDefinition = new AtomicReference<>();
        AtomicReference<MockRequest> loadedRequest = new AtomicReference<>();

        MockDefinition definition =
                new MockDefinition(
                        "payments", true, "classpath:mocks/payments.json", Duration.ofMillis(25));
        MockResponseSource responseSource =
                (mockDefinition, request) -> {
                    loadedDefinition.set(mockDefinition);
                    loadedRequest.set(request);
                    return new MockResponse(
                            201,
                            Map.of("X-Mock", java.util.List.of("true")),
                            "created".getBytes(StandardCharsets.UTF_8));
                };

        DefaultMockResponder responder =
                new DefaultMockResponder(
                        catalog(Map.of("payments", definition)),
                        responseSource,
                        (key, delay) -> {
                            delayedKey.set(key);
                            delayedDuration.set(delay);
                        });

        Optional<MockResponse> response = responder.respond(new MockRequest(" payments "));

        assertTrue(response.isPresent());
        assertEquals(201, response.get().status());
        assertArrayEquals("created".getBytes(StandardCharsets.UTF_8), response.get().body());
        assertEquals(definition, loadedDefinition.get());
        assertEquals("payments", loadedRequest.get().key());
        assertEquals("payments", delayedKey.get());
        assertEquals(Duration.ofMillis(25), delayedDuration.get());
    }

    @Test
    void throwsWhenResponseSourceReturnsNull() {
        DefaultMockResponder responder =
                new DefaultMockResponder(
                        catalog(
                                Map.of(
                                        "payments",
                                        new MockDefinition(
                                                "payments",
                                                true,
                                                "classpath:mocks/payments.json",
                                                Duration.ZERO))),
                        (definition, request) -> null);

        MockException exception =
                assertThrows(
                        MockException.class, () -> responder.respond(new MockRequest("payments")));
        assertEquals(
                "Mock response source returned null for key: payments", exception.getMessage());
    }

    private MockCatalog catalog(Map<String, MockDefinition> definitions) {
        return new DefaultMockCatalog(() -> definitions);
    }
}
