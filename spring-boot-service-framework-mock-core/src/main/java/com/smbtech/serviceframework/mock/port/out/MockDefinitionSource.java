package com.smbtech.serviceframework.mock.port.out;

import com.smbtech.serviceframework.mock.domain.MockDefinition;

import java.util.Map;

public interface MockDefinitionSource {

    Map<String, MockDefinition> loadDefinitions();
}
