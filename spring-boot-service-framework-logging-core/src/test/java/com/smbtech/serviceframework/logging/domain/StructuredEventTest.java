package com.smbtech.serviceframework.logging.domain;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StructuredEventTest {

    @Test
    void buildsImmutableStructuredEvent() {
        StructuredEvent event = StructuredEvent.builder(EventType.AUDIT)
                .message("Project {} updated", 42)
                .with("projectId", 42)
                .with("actor", actor -> actor.put("id", "user-7"))
                .tag("PROJECT")
                .sensitive()
                .build();

        assertEquals(EventType.AUDIT, event.type());
        assertEquals("Project {} updated", event.message());
        assertEquals(42, event.arguments().getFirst());
        assertEquals(Map.of("id", "user-7"), event.data().get("actor"));
        assertTrue(event.tags().contains("PROJECT"));
        assertTrue(event.isSensitive());
        assertThrows(UnsupportedOperationException.class, () -> event.data().put("x", "y"));
    }

    @Test
    void validatesTypeAndKeys() {
        assertThrows(IllegalArgumentException.class, () -> EventType.named(" "));
        assertThrows(
                IllegalArgumentException.class,
                () -> StructuredEvent.builder().with("", "value")
        );
    }
}
