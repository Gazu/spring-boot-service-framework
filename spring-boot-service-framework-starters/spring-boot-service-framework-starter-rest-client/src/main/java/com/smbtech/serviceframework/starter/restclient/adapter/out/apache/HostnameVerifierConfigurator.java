package com.smbtech.serviceframework.starter.restclient.adapter.out.apache;

import com.smbtech.serviceframework.httpclient.domain.HttpClientDefinition;
import javax.net.ssl.HostnameVerifier;
import org.apache.hc.client5.http.ssl.HttpsSupport;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;

/** Provides hostname verifier configurator behavior. */
final class HostnameVerifierConfigurator {
    /** Creates a hostname verifier configurator instance. */
    public HostnameVerifierConfigurator() {}

    /**
     * Creates the result.
     *
     * @param definition definition value
     * @return build result
     */
    public HostnameVerifier build(HttpClientDefinition definition) {
        if (definition.apache().hostnameVerificationEnabled()) {
            return HttpsSupport.getDefaultHostnameVerifier();
        }
        return NoopHostnameVerifier.INSTANCE;
    }
}
