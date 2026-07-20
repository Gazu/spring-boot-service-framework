package com.smbtech.serviceframework.starter.errorhandling.serialization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NotificationMetadataKeyNormalizerTest {

    private final NotificationMetadataKeyNormalizer normalizer =
            new NotificationMetadataKeyNormalizer();

    @Test
    void normalizesCommonKeyFormatsAndAcronyms() {
        assertEquals("correlation_id", normalizer.normalizeKey("correlationId"));
        assertEquals("field_name", normalizer.normalizeKey("field-name"));
        assertEquals("http_status_code", normalizer.normalizeKey("HTTPStatusCode"));
        assertEquals("customer_id", normalizer.normalizeKey(" customer ID "));
        assertEquals("already_snake_case", normalizer.normalizeKey("already_snake_case"));
    }

    @Test
    void normalizesMapsListsAndArraysRecursively() {
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("correlationId", "correlation-123");
        source.put(
                "requestContext",
                Map.of("customerId", "123", "fieldErrors", List.of(Map.of("fieldName", "email"))));
        source.put("statusValues", new Object[] {Map.of("statusCode", 400), "invalid"});

        Map<String, Object> normalized = normalizer.normalize(source);

        assertEquals("correlation-123", normalized.get("correlation_id"));
        Map<?, ?> requestContext = (Map<?, ?>) normalized.get("request_context");
        assertEquals("123", requestContext.get("customer_id"));
        List<?> fieldErrors = (List<?>) requestContext.get("field_errors");
        assertEquals("email", ((Map<?, ?>) fieldErrors.getFirst()).get("field_name"));
        List<?> statusValues = (List<?>) normalized.get("status_values");
        assertEquals(400, ((Map<?, ?>) statusValues.getFirst()).get("status_code"));
        assertThrows(UnsupportedOperationException.class, () -> normalized.put("other", "value"));
    }

    @Test
    void normalizesMixedCollectionsAndPrimitiveArraysRecursively() {
        LinkedHashSet<Object> events = new LinkedHashSet<>();
        events.add(Map.of("eventType", "created"));
        events.add(new int[] {200, 201});
        Map<String, Object> source = Map.of("requestEvents", events, "nullableValue", "unchanged");

        Map<String, Object> normalized = normalizer.normalize(source);

        List<?> normalizedEvents = (List<?>) normalized.get("request_events");
        assertEquals("created", ((Map<?, ?>) normalizedEvents.get(0)).get("event_type"));
        assertEquals(List.of(200, 201), normalizedEvents.get(1));
        assertEquals("unchanged", normalized.get("nullable_value"));
        assertThrows(UnsupportedOperationException.class, () -> normalizedEvents.add(null));
    }

    @Test
    void returnsAnEmptyImmutableMapForMissingMetadata() {
        assertEquals(Map.of(), normalizer.normalize(null));
        assertEquals(Map.of(), normalizer.normalize(Map.of()));
        assertThrows(
                UnsupportedOperationException.class,
                () -> normalizer.normalize(null).put("other", "value"));
    }

    @Test
    void detectsNormalizedKeyCollisions() {
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("customerId", "first");
        source.put("customer_id", "second");

        assertThrows(IllegalArgumentException.class, () -> normalizer.normalize(source));
    }

    @Test
    void detectsCycles() {
        Map<String, Object> cyclicMap = new LinkedHashMap<>();
        cyclicMap.put("self", cyclicMap);
        List<Object> cyclicList = new ArrayList<>();
        cyclicList.add(cyclicList);

        assertThrows(IllegalArgumentException.class, () -> normalizer.normalize(cyclicMap));
        assertThrows(
                IllegalArgumentException.class,
                () -> normalizer.normalize(Map.of("items", cyclicList)));
    }

    @Test
    void rejectsInvalidKeys() {
        assertThrows(NullPointerException.class, () -> normalizer.normalizeKey(null));
        assertThrows(IllegalArgumentException.class, () -> normalizer.normalizeKey(" "));
        assertThrows(IllegalArgumentException.class, () -> normalizer.normalizeKey("---"));
    }
}
