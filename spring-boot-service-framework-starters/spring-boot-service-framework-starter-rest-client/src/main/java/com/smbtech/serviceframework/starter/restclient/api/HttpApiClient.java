package com.smbtech.serviceframework.starter.restclient.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Defines the HTTP API client contract. */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface HttpApiClient {

    /**
     * Performs the value operation.
     *
     * @return value result
     */
    String value();
}
