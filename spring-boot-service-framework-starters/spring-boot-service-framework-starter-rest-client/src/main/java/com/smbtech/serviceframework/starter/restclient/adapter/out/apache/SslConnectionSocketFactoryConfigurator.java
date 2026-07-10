package com.smbtech.serviceframework.starter.restclient.adapter.out.apache;

import com.smbtech.serviceframework.httpclient.domain.HttpClientDefinition;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;

import javax.net.ssl.SSLContext;

public final class SslConnectionSocketFactoryConfigurator {

    private final HostnameVerifierConfigurator hostnameVerifierConfigurator;
    private final SslContextFactory sslContextFactory;
    private final SSLContext fallbackSslContext;

    public SslConnectionSocketFactoryConfigurator(
            HostnameVerifierConfigurator hostnameVerifierConfigurator,
            SslContextFactory sslContextFactory,
            SSLContext fallbackSslContext
    ) {
        this.hostnameVerifierConfigurator = hostnameVerifierConfigurator;
        this.sslContextFactory = sslContextFactory;
        this.fallbackSslContext = fallbackSslContext;
    }

    public SSLConnectionSocketFactory build(HttpClientDefinition definition) {
        SSLConnectionSocketFactoryBuilder builder = SSLConnectionSocketFactoryBuilder.create()
                .setHostnameVerifier(hostnameVerifierConfigurator.build(definition))
                .useSystemProperties();

        SSLContext sslContext = sslContextFactory.build(definition, fallbackSslContext);
        if (sslContext != null) {
            builder.setSslContext(sslContext);
        }

        return builder.build();
    }
}
