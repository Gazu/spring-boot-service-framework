package com.smbtech.serviceframework.starter.restclient.adapter.out.apache;

import com.smbtech.serviceframework.httpclient.domain.HttpClientDefinition;
import org.apache.hc.client5.http.config.RequestConfig;

public final class RequestConfigConfigurator {

    public RequestConfig build(HttpClientDefinition definition) {
        return RequestConfig.custom()
                .setConnectTimeout(ApacheTime.timeout(definition.timeout().connectTimeout()))
                .setConnectionRequestTimeout(ApacheTime.timeout(definition.timeout().connectionRequestTimeout()))
                .setResponseTimeout(ApacheTime.timeout(definition.timeout().responseTimeout()))
                .setConnectionKeepAlive(ApacheTime.timeValue(definition.pooling().keepAlive()))
                .build();
    }
}
