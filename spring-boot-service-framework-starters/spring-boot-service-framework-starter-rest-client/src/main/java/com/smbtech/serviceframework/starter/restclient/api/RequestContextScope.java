package com.smbtech.serviceframework.starter.restclient.api;

/** Defines the request context scope contract. */
public interface RequestContextScope extends AutoCloseable {

    /**
     * Performs the context operation.
     *
     * @return context result
     */
    RequestContext context();

    /**
     * Reports whether closed.
     *
     * @return is closed result
     */
    boolean isClosed();

    @Override
    void close();
}
