package com.smbtech.serviceframework.starter.restclient.autoconfigure;

import com.smbtech.serviceframework.httpclient.domain.CredentialDefinition;
import com.smbtech.serviceframework.httpclient.exception.AuthenticationException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class CredentialPropertiesMapper {

    public Map<String, CredentialDefinition> map(RestClientProperties properties) {
        Map<String, CredentialDefinition> definitions = new LinkedHashMap<>();
        RestClientProperties.Authentication authentication =
                Objects.requireNonNullElseGet(properties.getAuthentication(), RestClientProperties.Authentication::new);

        authentication.getCredentials().forEach((id, credential) ->
                definitions.put(id, new CredentialDefinition(id, value(id, credential)))
        );

        return definitions;
    }

    private String value(String id, RestClientProperties.Credential credential) {
        if (credential == null) {
            return "";
        }
        if (credential.getBase64() != null && !credential.getBase64().isBlank()) {
            try {
                byte[] decoded = Base64.getDecoder().decode(normalizedBase64(credential.getBase64()));
                return new String(decoded, StandardCharsets.UTF_8);
            } catch (IllegalArgumentException exception) {
                throw new AuthenticationException("Invalid base64 credential value: " + id, exception);
            }
        }
        return credential.getValue();
    }

    private String normalizedBase64(String value) {
        return value.replaceAll("\\s", "");
    }
}
