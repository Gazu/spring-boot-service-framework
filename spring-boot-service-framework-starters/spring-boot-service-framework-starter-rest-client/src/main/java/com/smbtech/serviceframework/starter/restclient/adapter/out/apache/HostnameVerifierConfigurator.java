package com.smbtech.serviceframework.starter.restclient.adapter.out.apache;

import com.smbtech.serviceframework.httpclient.domain.HttpClientDefinition;
import org.apache.hc.client5.http.ssl.HttpsSupport;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;

import javax.net.ssl.HostnameVerifier;

public final class HostnameVerifierConfigurator {

    public HostnameVerifier build(HttpClientDefinition definition) {
        if (definition.apache().hostnameVerificationEnabled()) {
            return HttpsSupport.getDefaultHostnameVerifier();
        }
        return NoopHostnameVerifier.INSTANCE;
    }
}
