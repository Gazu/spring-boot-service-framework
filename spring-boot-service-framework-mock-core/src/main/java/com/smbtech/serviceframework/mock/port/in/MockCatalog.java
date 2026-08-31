package com.smbtech.serviceframework.mock.port.in;

import com.smbtech.serviceframework.mock.domain.MockDefinition;
import com.smbtech.serviceframework.mock.port.out.MockDefinitionSource;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Defines the mock catalog contract. */
public interface MockCatalog {

    /**
     * Creates the default immutable catalog from a definition source.
     *
     * @param source definition source
     * @return default mock catalog
     */
    static MockCatalog from(MockDefinitionSource source) {
        return new DefaultMockCatalog(source);
    }

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
