package com.smbtech.serviceframework.httpclient.port.in;

import com.smbtech.serviceframework.httpclient.domain.HttpClientDefinition;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface HttpClientCatalog {

    Optional<HttpClientDefinition> findByName(String name);

    HttpClientDefinition requireByName(String name);

    Set<String> names();

    Map<String, HttpClientDefinition> all();
}
