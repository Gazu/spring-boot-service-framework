package com.smbtech.serviceframework.mock.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class MockDefinitionTest {

    @Test
    void enabledDefinitionWithFileIsUsable() {
        MockDefinition definition =
                new MockDefinition(
                        " payments-success ",
                        true,
                        " classpath:mocks/payments-success.json ",
                        Duration.ofMillis(50));

        assertEquals("payments-success", definition.key());
        assertEquals("classpath:mocks/payments-success.json", definition.file());
        assertEquals(Duration.ofMillis(50), definition.delay());
        assertTrue(definition.isUsable());
    }

    @Test
    void disabledDefinitionIsNotUsable() {
        MockDefinition definition = MockDefinition.disabled("payments");

        assertEquals("payments", definition.key());
        assertFalse(definition.enabled());
        assertFalse(definition.isUsable());
    }

    @Test
    void enabledDefinitionWithoutFileIsNotUsable() {
        MockDefinition definition = new MockDefinition("payments", true, "", null);

        assertEquals(Duration.ZERO, definition.delay());
        assertFalse(definition.isUsable());
    }
}
