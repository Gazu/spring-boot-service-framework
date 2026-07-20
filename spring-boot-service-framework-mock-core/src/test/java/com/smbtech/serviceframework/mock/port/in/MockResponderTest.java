package com.smbtech.serviceframework.mock.port.in;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.smbtech.serviceframework.mock.domain.MockRequest;
import com.smbtech.serviceframework.mock.domain.MockResponse;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MockResponderTest {

    @Test
    void contractCanReturnMockResponse() {
        MockResponder responder =
                request ->
                        request.hasKey()
                                ? Optional.of(
                                        MockResponse.ok(
                                                ("mock:" + request.key())
                                                        .getBytes(StandardCharsets.UTF_8)))
                                : Optional.empty();

        Optional<MockResponse> response = responder.respond(new MockRequest("payments"));

        assertTrue(response.isPresent());
        assertArrayEquals("mock:payments".getBytes(StandardCharsets.UTF_8), response.get().body());
    }

    @Test
    void contractCanReturnEmptyWhenMockDoesNotApply() {
        MockResponder responder = request -> Optional.empty();

        Optional<MockResponse> response = responder.respond(new MockRequest("payments"));

        assertTrue(response.isEmpty());
    }
}
