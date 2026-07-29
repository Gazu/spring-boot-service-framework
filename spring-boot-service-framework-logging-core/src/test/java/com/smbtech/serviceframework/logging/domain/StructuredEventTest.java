package com.smbtech.serviceframework.logging.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StructuredEventTest {

    @Test
    void buildsImmutableStructuredEvent() {
        StructuredEvent event =
                StructuredEvent.builder(EventType.AUDIT)
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
                IllegalArgumentException.class, () -> StructuredEvent.builder().with("", "value"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void recursivelyCopiesArgumentsAndStructuredData() {
        List<Object> scopes = new ArrayList<>(List.of("payment.read"));
        Map<String, Object> oauth2 = new LinkedHashMap<>();
        oauth2.put("scopes", scopes);

        StructuredEvent event =
                StructuredEvent.builder()
                        .message("Token attributes {}", oauth2)
                        .with("oauth2", oauth2)
                        .build();
        scopes.add("payment.write");
        oauth2.put("grant", "client_credentials");

        Map<String, Object> immutableData = (Map<String, Object>) event.data().get("oauth2");
        List<Object> immutableScopes = (List<Object>) immutableData.get("scopes");
        Map<String, Object> immutableArgument = (Map<String, Object>) event.arguments().getFirst();
        assertEquals(List.of("payment.read"), immutableScopes);
        assertEquals(Map.of("scopes", List.of("payment.read")), immutableArgument);
        assertThrows(
                UnsupportedOperationException.class,
                () -> immutableData.put("grant", "client_credentials"));
        assertThrows(
                UnsupportedOperationException.class, () -> immutableScopes.add("payment.write"));
    }
}
