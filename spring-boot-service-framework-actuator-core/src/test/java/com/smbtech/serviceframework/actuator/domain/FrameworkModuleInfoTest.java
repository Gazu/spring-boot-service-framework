package com.smbtech.serviceframework.actuator.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FrameworkModuleInfoTest {

    @Test
    void createsImmutableSanitizedModuleInformation() {
        List<Object> capabilities = new ArrayList<>(List.of("health"));
        FrameworkModuleInfo module =
                new FrameworkModuleInfo(
                        " actuator-core ",
                        " 0.4.0 ",
                        Map.of("capabilities", capabilities, "clientSecret", "must-not-leak"));
        capabilities.add("info");

        assertEquals("actuator-core", module.name());
        assertEquals("0.4.0", module.version());
        assertEquals(List.of("health"), module.attributes().get("capabilities"));
        assertEquals("[REDACTED]", module.attributes().get("clientSecret"));
        assertThrows(
                UnsupportedOperationException.class, () -> module.attributes().put("new", true));
    }

    @Test
    void supportsInformationWithoutAttributesAndRejectsBlankIdentity() {
        assertTrue(FrameworkModuleInfo.of("logging", "1.0.0").attributes().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> FrameworkModuleInfo.of(" ", "1.0.0"));
        assertThrows(IllegalArgumentException.class, () -> FrameworkModuleInfo.of("logging", " "));
    }
}
