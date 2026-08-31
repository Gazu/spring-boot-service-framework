package com.smbtech.serviceframework.starter.mock;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.smbtech.serviceframework.mock.port.in.MockCatalog;
import com.smbtech.serviceframework.mock.port.in.MockResponder;
import com.smbtech.serviceframework.mock.port.out.MockDefinitionSource;
import com.smbtech.serviceframework.mock.port.out.MockResponseSource;
import com.smbtech.serviceframework.starter.mock.api.MockService;
import java.lang.reflect.Modifier;
import java.util.List;
import org.junit.jupiter.api.Test;

class MockStarterEncapsulationTest {

    @Test
    void keepsConsumerContractsPublic() {
        assertTrue(Modifier.isPublic(MockService.class.getModifiers()));
        assertTrue(Modifier.isPublic(MockCatalog.class.getModifiers()));
        assertTrue(Modifier.isPublic(MockResponder.class.getModifiers()));
        assertTrue(Modifier.isPublic(MockDefinitionSource.class.getModifiers()));
        assertTrue(Modifier.isPublic(MockResponseSource.class.getModifiers()));
    }

    @Test
    void hidesRuntimeImplementations() throws ClassNotFoundException {
        for (String className : internalImplementations()) {
            assertFalse(Modifier.isPublic(Class.forName(className).getModifiers()), className);
        }
    }

    private List<String> internalImplementations() {
        return List.of(
                "com.smbtech.serviceframework.starter.mock.adapter.in.openapi.OpenApiMockContractLoader",
                "com.smbtech.serviceframework.starter.mock.adapter.in.openapi.OpenApiMockEndpoint",
                "com.smbtech.serviceframework.starter.mock.adapter.in.openapi.OpenApiMockServerRegistrar",
                "com.smbtech.serviceframework.starter.mock.adapter.in.spring.MockResponseEntityMapper",
                "com.smbtech.serviceframework.starter.mock.adapter.in.spring.SpringMockService",
                "com.smbtech.serviceframework.starter.mock.adapter.out.properties.PropertiesMockDefinitionSource",
                "com.smbtech.serviceframework.starter.mock.adapter.out.resource.ResourceMockResponseSource",
                "com.smbtech.serviceframework.starter.mock.adapter.out.restclient.MockClientHttpResponse",
                "com.smbtech.serviceframework.starter.mock.adapter.out.restclient.MockRestClientInterceptor",
                "com.smbtech.serviceframework.starter.mock.adapter.out.restclient.MockRestClientRequestMapper");
    }
}
