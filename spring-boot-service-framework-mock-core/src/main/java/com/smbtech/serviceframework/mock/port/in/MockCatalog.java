package com.smbtech.serviceframework.mock.port.in;

import com.smbtech.serviceframework.mock.domain.MockDefinition;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface MockCatalog {

    Optional<MockDefinition> findByKey(String key);

    MockDefinition requireByKey(String key);

    Set<String> keys();

    Map<String, MockDefinition> all();
}
