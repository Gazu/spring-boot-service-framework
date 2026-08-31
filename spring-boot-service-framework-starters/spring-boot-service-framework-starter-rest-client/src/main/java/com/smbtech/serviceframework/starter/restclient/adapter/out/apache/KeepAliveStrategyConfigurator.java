package com.smbtech.serviceframework.starter.restclient.adapter.out.apache;

import com.smbtech.serviceframework.httpclient.domain.HttpClientDefinition;
import org.apache.hc.client5.http.ConnectionKeepAliveStrategy;

/** Provides keep alive strategy configurator behavior. */
final class KeepAliveStrategyConfigurator {
    /** Creates a keep alive strategy configurator instance. */
    public KeepAliveStrategyConfigurator() {}

    /**
     * Creates the result.
     *
     * @param definition definition value
     * @return build result
     */
    public ConnectionKeepAliveStrategy build(HttpClientDefinition definition) {
        return (response, context) -> ApacheTime.timeValue(definition.pooling().keepAlive());
    }
}
