package com.smbtech.serviceframework.httpclient.port.out;

import com.smbtech.serviceframework.httpclient.domain.HttpClientDefinition;
import com.smbtech.serviceframework.httpclient.domain.HttpExchangeAuditFailure;
import com.smbtech.serviceframework.httpclient.domain.HttpExchangeAuditRequest;
import com.smbtech.serviceframework.httpclient.domain.HttpExchangeAuditResponse;
import java.util.Map;

/** Defines the http exchange audit sink contract. */
public interface HttpExchangeAuditSink {

    /**
     * Performs the request operation.
     *
     * @param definition definition value
     * @param event event value
     */
    default void request(HttpClientDefinition definition, HttpExchangeAuditRequest event) {
        request(definition, event.method(), event.uri(), event.headers());
    }

    /**
     * Performs the response operation.
     *
     * @param definition definition value
     * @param event event value
     */
    default void response(HttpClientDefinition definition, HttpExchangeAuditResponse event) {
        response(definition, event.statusCode(), event.headers());
    }

    /**
     * Performs the failure operation.
     *
     * @param definition definition value
     * @param event event value
     */
    default void failure(HttpClientDefinition definition, HttpExchangeAuditFailure event) {
        failure(definition, event.method(), event.uri(), event.throwable());
    }

    /**
     * Performs the request operation.
     *
     * @param definition definition value
     * @param method method value
     * @param uri uri value
     * @param headers headers value
     */
    default void request(
            HttpClientDefinition definition,
            String method,
            String uri,
            Map<String, String> headers) {
        // Compatibility hook for simple sinks.
    }

    /**
     * Performs the response operation.
     *
     * @param definition definition value
     * @param statusCode status code value
     * @param headers headers value
     */
    default void response(
            HttpClientDefinition definition, int statusCode, Map<String, String> headers) {
        // Compatibility hook for simple sinks.
    }

    /**
     * Performs the failure operation.
     *
     * @param definition definition value
     * @param method method value
     * @param uri uri value
     * @param throwable throwable value
     */
    default void failure(
            HttpClientDefinition definition, String method, String uri, Throwable throwable) {
        // Compatibility hook for simple sinks.
    }
}
