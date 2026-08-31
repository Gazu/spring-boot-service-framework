package com.smbtech.serviceframework.mock;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.smbtech.serviceframework.mock.port.in.MockCatalog;
import com.smbtech.serviceframework.mock.port.in.MockResponder;
import com.smbtech.serviceframework.mock.port.out.MockDefinitionSource;
import com.smbtech.serviceframework.mock.port.out.MockResponseSource;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.Test;

class MockCoreEncapsulationTest {

    @Test
    void exposesDefaultsThroughCorePorts() throws NoSuchMethodException {
        Method catalogFactory = MockCatalog.class.getMethod("from", MockDefinitionSource.class);
        Method responderFactory =
                MockResponder.class.getMethod("from", MockCatalog.class, MockResponseSource.class);

        assertTrue(Modifier.isPublic(catalogFactory.getModifiers()));
        assertTrue(Modifier.isStatic(catalogFactory.getModifiers()));
        assertTrue(Modifier.isPublic(responderFactory.getModifiers()));
        assertTrue(Modifier.isStatic(responderFactory.getModifiers()));
    }

    @Test
    void hidesDefaultImplementations() throws ClassNotFoundException {
        assertFalse(
                Modifier.isPublic(
                        Class.forName(
                                        "com.smbtech.serviceframework.mock.port.in.DefaultMockCatalog")
                                .getModifiers()));
        assertFalse(
                Modifier.isPublic(
                        Class.forName(
                                        "com.smbtech.serviceframework.mock.port.in.DefaultMockResponder")
                                .getModifiers()));
        assertThrows(
                ClassNotFoundException.class,
                () ->
                        Class.forName(
                                "com.smbtech.serviceframework.mock.service.DefaultMockCatalog"));
        assertThrows(
                ClassNotFoundException.class,
                () ->
                        Class.forName(
                                "com.smbtech.serviceframework.mock.service.DefaultMockResponder"));
    }
}
