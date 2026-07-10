package com.smbtech.serviceframework.starter.restclient.adapter.out.apache;

import com.smbtech.serviceframework.httpclient.domain.HttpClientDefinition;
import org.apache.hc.core5.http.io.SocketConfig;

public final class SocketConfigConfigurator {

    public SocketConfig build(HttpClientDefinition definition) {
        return SocketConfig.custom()
                .setSoTimeout(ApacheTime.timeout(definition.timeout().responseTimeout()))
                .setSoKeepAlive(definition.pooling().tcpKeepAlive())
                .setTcpNoDelay(true)
                .build();
    }
}
