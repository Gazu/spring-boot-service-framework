package com.smbtech.serviceframework.httpclient.port.out;

import com.smbtech.serviceframework.httpclient.domain.HttpErrorResponse;

import java.lang.reflect.Type;

/**
 * Reads downstream HTTP error response bodies into application-specific objects.
 */
public interface HttpErrorResponseBodyReader {

    /**
     * Converts the captured HTTP error response body into the requested type.
     *
     * @param error captured downstream HTTP error response
     * @param type target type
     * @return decoded error response body
     * @param <T> target type
     */
    <T> T read(HttpErrorResponse error, Class<T> type);

    /**
     * Converts the captured HTTP error response body into the requested generic type.
     *
     * @param error captured downstream HTTP error response
     * @param type target generic type
     * @return decoded error response body
     * @param <T> target type
     */
    <T> T read(HttpErrorResponse error, Type type);
}
