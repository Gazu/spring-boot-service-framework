package com.smbtech.serviceframework.logging;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.smbtech.serviceframework.logging.port.in.StructuredLogger;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.Test;

class LoggingCoreEncapsulationTest {

    @Test
    void exposesDefaultBehaviorThroughTheInputPort() throws NoSuchMethodException {
        Method factory =
                StructuredLogger.class.getMethod(
                        "create",
                        com.smbtech.serviceframework.logging.port.out.LogEventSink.class,
                        boolean.class);

        assertTrue(Modifier.isPublic(factory.getModifiers()));
        assertTrue(Modifier.isStatic(factory.getModifiers()));
    }

    @Test
    void hidesTheDefaultImplementation() throws ClassNotFoundException {
        assertFalse(
                Modifier.isPublic(
                        Class.forName(
                                        "com.smbtech.serviceframework.logging.port.in.DefaultStructuredLogger")
                                .getModifiers()));
        assertThrows(
                ClassNotFoundException.class,
                () ->
                        Class.forName(
                                "com.smbtech.serviceframework.logging.application.StructuredLoggingService"));
    }
}
