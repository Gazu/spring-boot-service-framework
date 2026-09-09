package com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.keystore;

import com.nimbusds.jose.jwk.JWK;
import com.smbtech.serviceframework.httpclient.port.out.KeyStoreDefinitionSource;
import java.util.function.Consumer;
import java.util.function.Function;
import org.springframework.core.io.ResourceLoader;

public final class KeyStoreRuntimeTestFixtures {

    private KeyStoreRuntimeTestFixtures() {}

    public static Capabilities capabilities(
            KeyStoreDefinitionSource definitionSource, ResourceLoader resourceLoader) {
        KeyStoreManager keyStoreManager = new KeyStoreManager(definitionSource, resourceLoader);
        KeyStoreRuntime runtime = new KeyStoreRuntime(keyStoreManager);
        return new Capabilities(
                runtime::validateLoadable,
                runtime::validateMtls,
                new SigningJwkFactory(new PrivateKeyLoader(keyStoreManager))::create);
    }

    public record Capabilities(
            Consumer<String> keyStoreValidator,
            Function<String, String> mtlsKeyStoreValidator,
            Function<String, JWK> signingJwkResolver) {}
}
