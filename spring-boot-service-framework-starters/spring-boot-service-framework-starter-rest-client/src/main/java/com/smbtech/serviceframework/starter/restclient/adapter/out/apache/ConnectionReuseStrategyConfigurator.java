package com.smbtech.serviceframework.starter.restclient.adapter.out.apache;

import com.smbtech.serviceframework.httpclient.domain.ConnectionReusePolicy;
import com.smbtech.serviceframework.httpclient.domain.HttpClientDefinition;
import org.apache.hc.core5.http.ConnectionReuseStrategy;
import org.apache.hc.core5.http.impl.DefaultConnectionReuseStrategy;

/** Provides connection reuse strategy configurator behavior. */
final class ConnectionReuseStrategyConfigurator {
    /** Creates a connection reuse strategy configurator instance. */
    public ConnectionReuseStrategyConfigurator() {}

    /**
     * Creates the result.
     *
     * @param definition definition value
     * @return build result
     */
    public ConnectionReuseStrategy build(HttpClientDefinition definition) {
        ConnectionReusePolicy policy = definition.pooling().connectionReusePolicy();
        return switch (policy) {
            case ALWAYS -> (request, response, context) -> true;
            case NEVER -> (request, response, context) -> false;
            case DEFAULT, OTHER -> DefaultConnectionReuseStrategy.INSTANCE;
        };
    }
}
