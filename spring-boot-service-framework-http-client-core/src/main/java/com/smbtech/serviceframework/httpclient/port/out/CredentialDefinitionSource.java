package com.smbtech.serviceframework.httpclient.port.out;

import com.smbtech.serviceframework.httpclient.domain.CredentialDefinition;

import java.util.Map;
import java.util.Optional;

public interface CredentialDefinitionSource {

    Optional<CredentialDefinition> findById(String id);

    Map<String, CredentialDefinition> all();
}
