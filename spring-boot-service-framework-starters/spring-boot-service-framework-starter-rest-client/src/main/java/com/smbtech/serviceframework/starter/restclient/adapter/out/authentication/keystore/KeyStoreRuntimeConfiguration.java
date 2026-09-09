package com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.keystore;

import com.smbtech.serviceframework.httpclient.domain.HttpClientDefinition;
import com.smbtech.serviceframework.httpclient.port.out.KeyStoreDefinitionSource;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.net.ssl.SSLContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;

@Configuration(proxyBeanMethods = false)
class KeyStoreRuntimeConfiguration {

    @Bean
    KeyStoreManager restClientKeyStoreManager(
            KeyStoreDefinitionSource definitionSource, ResourceLoader resourceLoader) {
        return new KeyStoreManager(definitionSource, resourceLoader);
    }

    @Bean
    KeyStoreRuntime restClientKeyStoreRuntime(KeyStoreManager keyStoreManager) {
        return new KeyStoreRuntime(keyStoreManager);
    }

    @Bean("restClientSslContextBuilder")
    BiFunction<HttpClientDefinition, SSLContext, SSLContext> restClientSslContextBuilder(
            KeyStoreRuntime runtime) {
        return runtime::buildSslContext;
    }

    @Bean("restClientKeyStoreContentValidator")
    Consumer<String> restClientKeyStoreContentValidator(KeyStoreRuntime runtime) {
        return runtime::validateLoadable;
    }

    @Bean("restClientMtlsKeyStoreContentValidator")
    Function<String, String> restClientMtlsKeyStoreContentValidator(KeyStoreRuntime runtime) {
        return runtime::validateMtls;
    }
}
