package com.smbtech.serviceframework.starter.restclient.adapter.out.spring;

import com.smbtech.serviceframework.httpclient.domain.HttpClientDefinition;
import com.smbtech.serviceframework.httpclient.domain.HttpExchangeAuditFailure;
import com.smbtech.serviceframework.httpclient.domain.HttpExchangeAuditRequest;
import com.smbtech.serviceframework.httpclient.domain.HttpExchangeAuditResponse;
import com.smbtech.serviceframework.httpclient.port.out.HttpExchangeAuditSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Slf4jHttpExchangeAuditSink implements HttpExchangeAuditSink {

    private static final Logger log = LoggerFactory.getLogger(Slf4jHttpExchangeAuditSink.class);

    @Override
    public void request(HttpClientDefinition definition, HttpExchangeAuditRequest event) {
        log.info("HTTP client request client={} method={} uri={} headers={} body={}",
                definition.name(), event.method(), event.uri(), event.headers(), event.body());
    }

    @Override
    public void response(HttpClientDefinition definition, HttpExchangeAuditResponse event) {
        log.info("HTTP client response client={} method={} uri={} status={} statusText={} durationMs={} headers={} body={}",
                definition.name(),
                event.method(),
                event.uri(),
                event.statusCode(),
                event.statusText(),
                event.duration().toMillis(),
                event.headers(),
                event.body());
    }

    @Override
    public void failure(HttpClientDefinition definition, HttpExchangeAuditFailure event) {
        log.warn("HTTP client failure client={} method={} uri={} status={} statusText={} durationMs={} headers={} requestBody={} responseBody={} exceptionType={} exceptionMessage={}",
                definition.name(),
                event.method(),
                event.uri(),
                event.hasStatusCode() ? event.statusCode() : "",
                event.statusText(),
                event.duration().toMillis(),
                event.headers(),
                event.requestBody(),
                event.responseBody(),
                event.exceptionType(),
                event.exceptionMessage(),
                event.throwable());
    }
}
