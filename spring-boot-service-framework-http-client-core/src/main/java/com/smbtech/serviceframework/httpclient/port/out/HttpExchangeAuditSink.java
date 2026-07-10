package com.smbtech.serviceframework.httpclient.port.out;

import com.smbtech.serviceframework.httpclient.domain.HttpClientDefinition;
import com.smbtech.serviceframework.httpclient.domain.HttpExchangeAuditFailure;
import com.smbtech.serviceframework.httpclient.domain.HttpExchangeAuditRequest;
import com.smbtech.serviceframework.httpclient.domain.HttpExchangeAuditResponse;

import java.util.Map;

public interface HttpExchangeAuditSink {

    default void request(HttpClientDefinition definition, HttpExchangeAuditRequest event) {
        request(definition, event.method(), event.uri(), event.headers());
    }

    default void response(HttpClientDefinition definition, HttpExchangeAuditResponse event) {
        response(definition, event.statusCode(), event.headers());
    }

    default void failure(HttpClientDefinition definition, HttpExchangeAuditFailure event) {
        failure(definition, event.method(), event.uri(), event.throwable());
    }

    default void request(HttpClientDefinition definition, String method, String uri, Map<String, String> headers) {
        // Compatibility hook for simple sinks.
    }

    default void response(HttpClientDefinition definition, int statusCode, Map<String, String> headers) {
        // Compatibility hook for simple sinks.
    }

    default void failure(HttpClientDefinition definition, String method, String uri, Throwable throwable) {
        // Compatibility hook for simple sinks.
    }
}
