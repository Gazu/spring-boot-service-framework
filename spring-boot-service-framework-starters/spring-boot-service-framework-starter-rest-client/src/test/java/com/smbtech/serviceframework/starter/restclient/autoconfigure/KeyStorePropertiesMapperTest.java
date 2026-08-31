package com.smbtech.serviceframework.starter.restclient.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import com.smbtech.serviceframework.httpclient.domain.KeyStoreDefinition;
import com.smbtech.serviceframework.httpclient.port.out.CredentialProvider;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class KeyStorePropertiesMapperTest {

    @Test
    void decodesBase64CredentialRefsForStorePasswordAndKeyPassword() {
        RestClientProperties properties = new RestClientProperties();

        RestClientProperties.KeyStore keyStore = new RestClientProperties.KeyStore();
        keyStore.setBase64("ZmFrZS1qa3M=");
        keyStore.setType("JKS");
        keyStore.setPasswordRef("store-password");
        keyStore.setKeyAlias("client");
        keyStore.setKeyPasswordRef("key-password");
        properties.getAuthentication().getKeyStores().put("client-cert", keyStore);

        RestClientProperties.Credential storePassword = new RestClientProperties.Credential();
        storePassword.setBase64(encoded("store-secret"));
        properties.getAuthentication().getCredentials().put("store-password", storePassword);

        RestClientProperties.Credential keyPassword = new RestClientProperties.Credential();
        keyPassword.setBase64(encoded("key-secret"));
        properties.getAuthentication().getCredentials().put("key-password", keyPassword);

        Map<String, String> decodedCredentials =
                new CredentialPropertiesMapper()
                        .map(properties).values().stream()
                                .collect(
                                        java.util.stream.Collectors.toMap(
                                                com.smbtech.serviceframework.httpclient.domain
                                                                .CredentialDefinition
                                                        ::id,
                                                com.smbtech.serviceframework.httpclient.domain
                                                                .CredentialDefinition
                                                        ::value));
        CredentialProvider credentialProvider =
                key -> Optional.ofNullable(decodedCredentials.get(key));
        KeyStorePropertiesMapper mapper =
                new KeyStorePropertiesMapper(new CredentialResolver(credentialProvider));

        Map<String, KeyStoreDefinition> definitions = mapper.map(properties);

        KeyStoreDefinition definition = definitions.get("client-cert");
        assertThat(definition.password()).isEqualTo("store-secret");
        assertThat(definition.keyPassword()).isEqualTo("key-secret");
    }

    private String encoded(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
