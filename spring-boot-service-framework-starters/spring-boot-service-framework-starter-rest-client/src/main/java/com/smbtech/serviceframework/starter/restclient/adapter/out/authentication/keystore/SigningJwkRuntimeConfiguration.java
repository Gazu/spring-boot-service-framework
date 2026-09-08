package com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.keystore;

import com.nimbusds.jose.jwk.JWK;
import java.util.function.Function;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(name = "com.nimbusds.jose.jwk.JWK")
class SigningJwkRuntimeConfiguration {

    @Bean("restClientSigningJwkResolver")
    Function<String, JWK> restClientSigningJwkResolver(KeyStoreManager keyStoreManager) {
        return new SigningJwkFactory(new PrivateKeyLoader(keyStoreManager))::create;
    }
}
