package com.smbtech.serviceframework.actuator;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Modifier;
import org.junit.jupiter.api.Test;

class ActuatorCoreEncapsulationTest {

    @Test
    void keepsDefaultDiagnosticsImplementationInternal() throws ClassNotFoundException {
        Class<?> implementation =
                Class.forName(
                        "com.smbtech.serviceframework.actuator.port.in.DefaultFrameworkDiagnostics");

        assertFalse(Modifier.isPublic(implementation.getModifiers()));
        assertThrows(
                ClassNotFoundException.class,
                () ->
                        Class.forName(
                                "com.smbtech.serviceframework.actuator.service.DefaultFrameworkDiagnostics"));
    }
}
