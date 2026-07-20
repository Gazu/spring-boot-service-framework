package com.smbtech.serviceframework.starter.restclient.api;

import java.util.Map;
import java.util.Set;
import org.springframework.web.client.RestClient;

/** Defines the rest client registry contract. */
public interface RestClientRegistry {

    /**
     * Performs the get operation.
     *
     * @param name name value
     * @return get result
     */
    RestClient get(String name);

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
    Map<String, RestClient> all();
}
