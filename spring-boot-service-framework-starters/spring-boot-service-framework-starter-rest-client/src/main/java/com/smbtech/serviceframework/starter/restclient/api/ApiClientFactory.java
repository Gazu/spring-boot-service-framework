package com.smbtech.serviceframework.starter.restclient.api;

/** Defines the api client factory contract. */
public interface ApiClientFactory {

    /**
     * Creates the result.
     *
     * @param clientName client name value
     * @param apiType api type value
     * @return create result
     * @param <T> generic value type
     */
    <T> T create(String clientName, Class<T> apiType);

    /**
     * Creates the result.
     *
     * @param apiType api type value
     * @return create result
     * @param <T> generic value type
     */
    <T> T create(Class<T> apiType);
}
