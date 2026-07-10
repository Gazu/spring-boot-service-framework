package com.smbtech.serviceframework.starter.restclient.adapter.out.apache;

import com.smbtech.serviceframework.httpclient.domain.HttpClientDefinition;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;

public final class RegistryConfigurator {

    private final SslConnectionSocketFactoryConfigurator sslConnectionSocketFactoryConfigurator;

    public RegistryConfigurator(SslConnectionSocketFactoryConfigurator sslConnectionSocketFactoryConfigurator) {
        this.sslConnectionSocketFactoryConfigurator = sslConnectionSocketFactoryConfigurator;
    }

    public Registry<ConnectionSocketFactory> build(HttpClientDefinition definition) {
        return RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.getSocketFactory())
                .register("https", sslConnectionSocketFactoryConfigurator.build(definition))
                .build();
    }
}
