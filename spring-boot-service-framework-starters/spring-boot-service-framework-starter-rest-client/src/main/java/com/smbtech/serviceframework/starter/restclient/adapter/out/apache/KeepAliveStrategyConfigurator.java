package com.smbtech.serviceframework.starter.restclient.adapter.out.apache;

import com.smbtech.serviceframework.httpclient.domain.HttpClientDefinition;
import org.apache.hc.client5.http.ConnectionKeepAliveStrategy;

public final class KeepAliveStrategyConfigurator {

    public ConnectionKeepAliveStrategy build(HttpClientDefinition definition) {
        return (response, context) -> ApacheTime.timeValue(definition.pooling().keepAlive());
    }
}
