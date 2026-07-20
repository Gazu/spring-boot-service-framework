package com.smbtech.serviceframework.mock.port.in;

import com.smbtech.serviceframework.mock.domain.MockDefinition;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Defines the mock catalog contract. */
public interface MockCatalog {

    /**
     * Finds by key.
     *
     * @param key key value
     * @return find by key result
     */
    Optional<MockDefinition> findByKey(String key);

    /**
     * Performs the require by key operation.
     *
     * @param key key value
     * @return require by key result
     */
    MockDefinition requireByKey(String key);

    /**
     * Performs the keys operation.
     *
     * @return keys result
     */
    Set<String> keys();

    /**
     * Performs the all operation.
     *
     * @return all result
     */
    Map<String, MockDefinition> all();
}
