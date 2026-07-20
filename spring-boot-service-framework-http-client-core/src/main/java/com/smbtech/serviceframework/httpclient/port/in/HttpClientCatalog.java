package com.smbtech.serviceframework.httpclient.port.in;

import com.smbtech.serviceframework.httpclient.domain.HttpClientDefinition;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Defines the http client catalog contract. */
public interface HttpClientCatalog {

    /**
     * Finds by name.
     *
     * @param name name value
     * @return find by name result
     */
    Optional<HttpClientDefinition> findByName(String name);

    /**
     * Performs the require by name operation.
     *
     * @param name name value
     * @return require by name result
     */
    HttpClientDefinition requireByName(String name);

    /**
     * Performs the names operation.
     *
     * @return names result
     */
    Set<String> names();

    /**
     * Performs the all operation.
     *
     * @return all result
     */
    Map<String, HttpClientDefinition> all();
}
