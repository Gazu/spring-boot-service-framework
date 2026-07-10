package com.smbtech.serviceframework.starter.restclient;

import com.smbtech.serviceframework.httpclient.domain.CredentialDefinition;
import com.smbtech.serviceframework.httpclient.exception.AuthenticationException;
import com.smbtech.serviceframework.starter.restclient.autoconfigure.CredentialPropertiesMapper;
import com.smbtech.serviceframework.starter.restclient.autoconfigure.RestClientProperties;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CredentialPropertiesMapperTest {

    private final CredentialPropertiesMapper mapper = new CredentialPropertiesMapper();

    @Test
    void mapsPlainCredentialValue() {
        RestClientProperties properties = new RestClientProperties();
        RestClientProperties.Credential credential = new RestClientProperties.Credential();
        credential.setValue("plain-secret");
        properties.getAuthentication().getCredentials().put("plain", credential);

        Map<String, CredentialDefinition> definitions = mapper.map(properties);

        assertThat(definitions).containsKey("plain");
        assertThat(definitions.get("plain").value()).isEqualTo("plain-secret");
    }

    @Test
    void decodesBase64CredentialValue() {
        RestClientProperties properties = new RestClientProperties();
        RestClientProperties.Credential credential = new RestClientProperties.Credential();
        credential.setBase64("Y2hhbmdlaXQ=");
        properties.getAuthentication().getCredentials().put("password", credential);

        Map<String, CredentialDefinition> definitions = mapper.map(properties);

        assertThat(definitions.get("password").value()).isEqualTo("changeit");
    }

    @Test
    void base64CredentialValueHasPriorityOverPlainValue() {
        RestClientProperties properties = new RestClientProperties();
        RestClientProperties.Credential credential = new RestClientProperties.Credential();
        credential.setValue("plain-secret");
        credential.setBase64("YmFzZTY0LXNlY3JldA==");
        properties.getAuthentication().getCredentials().put("password", credential);

        Map<String, CredentialDefinition> definitions = mapper.map(properties);

        assertThat(definitions.get("password").value()).isEqualTo("base64-secret");
    }

    @Test
    void base64CredentialValueMayContainWhitespace() {
        RestClientProperties properties = new RestClientProperties();
        RestClientProperties.Credential credential = new RestClientProperties.Credential();
        credential.setBase64("Y2hh\nbmdl\taXQ=");
        properties.getAuthentication().getCredentials().put("password", credential);

        Map<String, CredentialDefinition> definitions = mapper.map(properties);

        assertThat(definitions.get("password").value()).isEqualTo("changeit");
    }

    @Test
    void rejectsInvalidBase64CredentialValue() {
        RestClientProperties properties = new RestClientProperties();
        RestClientProperties.Credential credential = new RestClientProperties.Credential();
        credential.setBase64("not-valid-base64");
        properties.getAuthentication().getCredentials().put("broken", credential);

        assertThatThrownBy(() -> mapper.map(properties))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Invalid base64 credential value: broken");
    }
}
