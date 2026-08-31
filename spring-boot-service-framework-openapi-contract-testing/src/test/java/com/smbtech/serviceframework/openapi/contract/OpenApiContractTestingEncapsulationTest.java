package com.smbtech.serviceframework.openapi.contract;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import java.util.List;
import org.junit.jupiter.api.Test;

class OpenApiContractTestingEncapsulationTest {

    @Test
    void keepsConsumerContractsPublic() {
        for (Class<?> type : publicContracts()) {
            assertTrue(Modifier.isPublic(type.getModifiers()), type.getName());
        }
    }

    @Test
    void hidesParserModels() {
        for (Class<?> type : internalModels()) {
            assertFalse(Modifier.isPublic(type.getModifiers()), type.getName());
        }
    }

    private List<Class<?>> publicContracts() {
        return List.of(
                OpenApiContract.class,
                OpenApiContractLoader.class,
                OpenApiContractTestCase.class,
                OpenApiContractTestResult.class,
                OpenApiContractViolation.class,
                OpenApiContractViolationCode.class,
                OpenApiMvcContractTester.class,
                OpenApiOperation.class,
                OpenApiResponse.class);
    }

    private List<Class<?>> internalModels() {
        return List.of(
                OpenApiRequestBody.class,
                OpenApiRequestDefinition.class,
                OpenApiRequestParameter.class);
    }
}
