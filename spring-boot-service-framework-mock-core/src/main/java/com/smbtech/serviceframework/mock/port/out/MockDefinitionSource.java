package com.smbtech.serviceframework.mock.port.out;

import com.smbtech.serviceframework.mock.domain.MockDefinition;
import java.util.Map;

/** Defines the mock definition source contract. */
public interface MockDefinitionSource {

    /**
     * Loads definitions.
     *
     * @return load definitions result
     */
    Map<String, MockDefinition> loadDefinitions();
}
