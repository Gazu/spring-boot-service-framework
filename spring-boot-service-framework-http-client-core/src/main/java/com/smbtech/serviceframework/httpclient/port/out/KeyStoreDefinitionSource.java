package com.smbtech.serviceframework.httpclient.port.out;

import com.smbtech.serviceframework.httpclient.domain.KeyStoreDefinition;

import java.util.Map;
import java.util.Optional;

public interface KeyStoreDefinitionSource {

    Optional<KeyStoreDefinition> findById(String id);

    Map<String, KeyStoreDefinition> all();
}
