package com.smbtech.serviceframework.mock.domain;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MockResponseTest {

    @Test
    void normalizesNullableValues() {
        MockResponse response = new MockResponse(0, null, null, null, null);

        assertEquals(200, response.status());
        assertTrue(response.headers().isEmpty());
        assertArrayEquals(new byte[0], response.body());
        assertEquals(Duration.ZERO, response.delay());
        assertTrue(response.metadata().isEmpty());
        assertFalse(response.hasBody());
        assertFalse(response.hasDelay());
    }

    @Test
    void copiesMutableInputs() {
        Map<String, List<String>> headers = new LinkedHashMap<>();
        headers.put("Content-Type", new ArrayList<>(List.of("application/json")));
        byte[] body = "{\"ok\":true}".getBytes(StandardCharsets.UTF_8);

        MockResponse response =
                new MockResponse(
                        201, headers, body, Duration.ofMillis(25), Map.of("scenario", "created"));

        headers.get("Content-Type").add("text/plain");
        body[0] = '[';

        assertEquals(201, response.status());
        assertEquals(List.of("application/json"), response.headers().get("Content-Type"));
        assertArrayEquals("{\"ok\":true}".getBytes(StandardCharsets.UTF_8), response.body());
        assertTrue(response.hasBody());
        assertTrue(response.hasDelay());
    }

    @Test
    void exposesImmutableMapsAndBodyCopy() {
        MockResponse response = MockResponse.ok("ok".getBytes(StandardCharsets.UTF_8));

        assertThrows(
                UnsupportedOperationException.class,
                () -> response.headers().put("X-Test", List.of("one")));

        byte[] body = response.body();
        body[0] = 'O';

        assertArrayEquals("ok".getBytes(StandardCharsets.UTF_8), response.body());
    }

    @Test
    @SuppressWarnings("unchecked")
    void recursivelyCopiesMetadata() {
        List<Object> values = new ArrayList<>(List.of("created"));
        Map<String, Object> nested = new LinkedHashMap<>();
        nested.put("states", values);

        MockResponse response =
                new MockResponse(200, Map.of(), null, Duration.ZERO, Map.of("scenario", nested));
        values.add("updated");

        Map<String, Object> immutableNested =
                (Map<String, Object>) response.metadata().get("scenario");
        List<Object> immutableValues = (List<Object>) immutableNested.get("states");
        assertEquals(List.of("created"), immutableValues);
        assertThrows(UnsupportedOperationException.class, () -> immutableValues.add("updated"));
    }
}
