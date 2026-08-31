package com.smbtech.serviceframework.project.generator;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import org.junit.jupiter.api.Test;

class ProjectGeneratorEncapsulationTest {

    @Test
    void keepsGeneratorContractAndFactoryPublic() throws NoSuchMethodException {
        assertTrue(Modifier.isPublic(HexagonalProjectGenerator.class.getModifiers()));
        assertTrue(
                Modifier.isPublic(
                        HexagonalProjectGenerator.class.getMethod("create").getModifiers()));
    }

    @Test
    void hidesDefaultGeneratorAndCollaborators() throws ClassNotFoundException {
        assertFalse(Modifier.isPublic(DefaultHexagonalProjectGenerator.class.getModifiers()));
        assertFalse(Modifier.isPublic(ContractDescriptorLoader.class.getModifiers()));
        assertFalse(Modifier.isPublic(HexagonalProjectContributor.class.getModifiers()));
        assertThrows(
                ClassNotFoundException.class,
                () ->
                        Class.forName(
                                "com.smbtech.serviceframework.project.generator.internal.DefaultHexagonalProjectGenerator"));
    }
}
