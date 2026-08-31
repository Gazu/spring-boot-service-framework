package com.smbtech.serviceframework.httpclient.port.in;

import com.smbtech.serviceframework.httpclient.domain.HttpClientDefinition;
import com.smbtech.serviceframework.httpclient.port.out.HttpClientDefinitionSource;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Defines the http client catalog contract. */
public interface HttpClientCatalog {

    /**
     * Loads and validates a catalog from the supplied definition source.
     *
     * @param source definition source
     * @return immutable HTTP client catalog
     */
    static HttpClientCatalog from(HttpClientDefinitionSource source) {
        return from(source, HttpClientDefinitionValidator.defaultValidator());
    }

    /**
     * Loads a catalog using an application-provided validation policy.
     *
     * @param source definition source
     * @param validator definition validator
     * @return immutable HTTP client catalog
     */
    static HttpClientCatalog from(
            HttpClientDefinitionSource source, HttpClientDefinitionValidator validator) {
        return new DefaultHttpClientCatalog(
                Objects.requireNonNull(source, "source must not be null"),
                Objects.requireNonNull(validator, "validator must not be null"));
    }

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
