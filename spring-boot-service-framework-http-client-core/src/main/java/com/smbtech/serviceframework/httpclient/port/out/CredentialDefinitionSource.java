package com.smbtech.serviceframework.httpclient.port.out;

import com.smbtech.serviceframework.httpclient.domain.CredentialDefinition;
import java.util.Map;
import java.util.Optional;

/** Defines the credential definition source contract. */
public interface CredentialDefinitionSource {

    /**
     * Finds by id.
     *
     * @param id id value
     * @return find by id result
     */
    Optional<CredentialDefinition> findById(String id);

    /**
     * Performs the all operation.
     *
     * @return all result
     */
    Map<String, CredentialDefinition> all();
}
