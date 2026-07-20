package com.smbtech.serviceframework.starter.restclient.adapter.out.apache;

import com.smbtech.serviceframework.httpclient.domain.HttpClientDefinition;
import javax.net.ssl.SSLContext;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;

/** Provides ssl connection socket factory configurator behavior. */
public final class SslConnectionSocketFactoryConfigurator {

    private final HostnameVerifierConfigurator hostnameVerifierConfigurator;
    private final SslContextFactory sslContextFactory;
    private final SSLContext fallbackSslContext;

    /**
     * Creates an SSL connection socket factory configurator instance.
     *
     * @param hostnameVerifierConfigurator hostname verifier configurator value
     * @param sslContextFactory ssl context factory value
     * @param fallbackSslContext fallback ssl context value
     */
    public SslConnectionSocketFactoryConfigurator(
            HostnameVerifierConfigurator hostnameVerifierConfigurator,
            SslContextFactory sslContextFactory,
            SSLContext fallbackSslContext) {
        this.hostnameVerifierConfigurator = hostnameVerifierConfigurator;
        this.sslContextFactory = sslContextFactory;
        this.fallbackSslContext = fallbackSslContext;
    }

    /**
     * Creates the result.
     *
     * @param definition definition value
     * @return build result
     */
    public SSLConnectionSocketFactory build(HttpClientDefinition definition) {
        SSLConnectionSocketFactoryBuilder builder =
                SSLConnectionSocketFactoryBuilder.create()
                        .setHostnameVerifier(hostnameVerifierConfigurator.build(definition))
                        .useSystemProperties();

        SSLContext sslContext = sslContextFactory.build(definition, fallbackSslContext);
        if (sslContext != null) {
            builder.setSslContext(sslContext);
        }

        return builder.build();
    }
}
