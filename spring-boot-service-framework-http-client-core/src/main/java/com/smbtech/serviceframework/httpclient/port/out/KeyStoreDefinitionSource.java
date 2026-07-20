package com.smbtech.serviceframework.httpclient.port.out;

import com.smbtech.serviceframework.httpclient.domain.KeyStoreDefinition;
import java.util.Map;
import java.util.Optional;

/** Defines the key store definition source contract. */
public interface KeyStoreDefinitionSource {

    /**
     * Finds by id.
     *
     * @param id id value
     * @return find by id result
     */
    Optional<KeyStoreDefinition> findById(String id);

    /**
     * Performs the all operation.
     *
     * @return all result
     */
    Map<String, KeyStoreDefinition> all();
}
