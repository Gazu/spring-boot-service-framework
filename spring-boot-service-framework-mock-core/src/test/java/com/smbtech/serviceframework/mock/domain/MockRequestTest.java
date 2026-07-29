package com.smbtech.serviceframework.mock.domain;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MockRequestTest {

    @Test
    void normalizesNullableValues() {
        MockRequest request = new MockRequest(null, null, null, null, null, null, null);

        assertEquals("", request.key());
        assertEquals("", request.method());
        assertEquals("", request.path());
        assertTrue(request.headers().isEmpty());
        assertTrue(request.queryParams().isEmpty());
        assertArrayEquals(new byte[0], request.body());
        assertTrue(request.attributes().isEmpty());
        assertFalse(request.hasKey());
    }

    @Test
    void copiesMutableInputs() {
        Map<String, List<String>> headers = new LinkedHashMap<>();
        headers.put("X-Test", new ArrayList<>(List.of("one")));
        byte[] body = "request".getBytes(StandardCharsets.UTF_8);

        MockRequest request =
                new MockRequest(
                        " payments ",
                        " GET ",
                        " /v1/payments ",
                        headers,
                        Map.of("status", List.of("open")),
                        body,
                        Map.of("source", "test"));

        headers.get("X-Test").add("two");
        body[0] = 'R';

        assertEquals("payments", request.key());
        assertEquals("GET", request.method());
        assertEquals("/v1/payments", request.path());
        assertEquals(List.of("one"), request.headers().get("X-Test"));
        assertArrayEquals("request".getBytes(StandardCharsets.UTF_8), request.body());
        assertTrue(request.hasKey());
    }

    @Test
    void exposesImmutableMapsAndBodyCopy() {
        MockRequest request =
                new MockRequest(
                        "payments",
                        "POST",
                        "/payments",
                        Map.of("X-Test", List.of("one")),
                        Map.of(),
                        "request".getBytes(StandardCharsets.UTF_8),
                        Map.of());

        assertThrows(
                UnsupportedOperationException.class,
                () -> request.headers().put("X-Other", List.of("two")));
        assertThrows(
                UnsupportedOperationException.class,
                () -> request.headers().get("X-Test").add("two"));

        byte[] body = request.body();
        body[0] = 'R';

        assertArrayEquals("request".getBytes(StandardCharsets.UTF_8), request.body());
    }

    @Test
    @SuppressWarnings("unchecked")
    void recursivelyCopiesAttributes() {
        List<Object> values = new ArrayList<>(List.of("one"));
        Map<String, Object> nested = new LinkedHashMap<>();
        nested.put("values", values);

        MockRequest request =
                new MockRequest(
                        "payments",
                        "GET",
                        "/payments",
                        Map.of(),
                        Map.of(),
                        null,
                        Map.of("filter", nested));
        values.add("two");

        Map<String, Object> immutableNested =
                (Map<String, Object>) request.attributes().get("filter");
        List<Object> immutableValues = (List<Object>) immutableNested.get("values");
        assertEquals(List.of("one"), immutableValues);
        assertThrows(UnsupportedOperationException.class, () -> immutableValues.add("two"));
    }
}
