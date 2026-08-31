package com.smbtech.serviceframework.starter.restclient.adapter.out.apache;

import com.smbtech.serviceframework.httpclient.domain.HttpClientDefinition;
import java.util.function.BiFunction;
import javax.net.ssl.SSLContext;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;

/** Provides ssl connection socket factory configurator behavior. */
final class SslConnectionSocketFactoryConfigurator {

    private final HostnameVerifierConfigurator hostnameVerifierConfigurator;
    private final BiFunction<HttpClientDefinition, SSLContext, SSLContext> sslContextBuilder;
    private final SSLContext fallbackSslContext;

    /**
     * Creates an SSL connection socket factory configurator instance.
     *
     * @param hostnameVerifierConfigurator hostname verifier configurator value
     * @param sslContextBuilder SSL context builder
     * @param fallbackSslContext fallback ssl context value
     */
    public SslConnectionSocketFactoryConfigurator(
            HostnameVerifierConfigurator hostnameVerifierConfigurator,
            BiFunction<HttpClientDefinition, SSLContext, SSLContext> sslContextBuilder,
            SSLContext fallbackSslContext) {
        this.hostnameVerifierConfigurator = hostnameVerifierConfigurator;
        this.sslContextBuilder = sslContextBuilder;
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

        SSLContext sslContext = sslContextBuilder.apply(definition, fallbackSslContext);
        if (sslContext != null) {
            builder.setSslContext(sslContext);
        }

        return builder.build();
    }
}
