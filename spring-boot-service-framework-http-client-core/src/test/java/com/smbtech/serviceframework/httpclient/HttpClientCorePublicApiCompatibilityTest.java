package com.smbtech.serviceframework.httpclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.smbtech.serviceframework.httpclient.port.in.HttpClientCatalog;
import com.smbtech.serviceframework.httpclient.port.in.HttpClientDefinitionValidator;
import com.smbtech.serviceframework.httpclient.port.out.HttpClientDefinitionSource;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.Test;

class HttpClientCorePublicApiCompatibilityTest {

    @Test
    void exposesDefaultBehaviorThroughNeutralPorts() throws NoSuchMethodException {
        Method catalogFactory =
                HttpClientCatalog.class.getMethod(
                        "from",
                        HttpClientDefinitionSource.class,
                        HttpClientDefinitionValidator.class);
        Method validatorFactory = HttpClientDefinitionValidator.class.getMethod("defaultValidator");

        assertEquals(HttpClientCatalog.class, catalogFactory.getReturnType());
        assertEquals(HttpClientDefinitionValidator.class, validatorFactory.getReturnType());
        assertTrue(Modifier.isPublic(catalogFactory.getModifiers()));
        assertTrue(Modifier.isStatic(catalogFactory.getModifiers()));
        assertTrue(Modifier.isPublic(validatorFactory.getModifiers()));
        assertTrue(Modifier.isStatic(validatorFactory.getModifiers()));
    }

    @Test
    void keepsDefaultImplementationsOutsideThePublicApi() throws ClassNotFoundException {
        assertFalse(
                Modifier.isPublic(
                        Class.forName(
                                        "com.smbtech.serviceframework.httpclient.port.in.DefaultHttpClientCatalog")
                                .getModifiers()));
        assertFalse(
                Modifier.isPublic(
                        Class.forName(
                                        "com.smbtech.serviceframework.httpclient.port.in.DefaultHttpClientDefinitionValidator")
                                .getModifiers()));
        assertThrows(
                ClassNotFoundException.class,
                () ->
                        Class.forName(
                                "com.smbtech.serviceframework.httpclient.service.ScopeValidator"));
    }
}
