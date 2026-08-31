package com.smbtech.serviceframework.starter.restclient.adapter.out.apache;

import com.smbtech.serviceframework.httpclient.domain.HttpClientDefinition;
import org.apache.hc.core5.http.io.SocketConfig;

/** Provides socket config configurator behavior. */
final class SocketConfigConfigurator {
    /** Creates a socket config configurator instance. */
    public SocketConfigConfigurator() {}

    /**
     * Creates the result.
     *
     * @param definition definition value
     * @return build result
     */
    public SocketConfig build(HttpClientDefinition definition) {
        return SocketConfig.custom()
                .setSoTimeout(ApacheTime.timeout(definition.timeout().responseTimeout()))
                .setSoKeepAlive(definition.pooling().tcpKeepAlive())
                .setTcpNoDelay(true)
                .build();
    }
}
