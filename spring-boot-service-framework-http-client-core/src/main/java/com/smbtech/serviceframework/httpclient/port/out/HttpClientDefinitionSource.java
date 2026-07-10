package com.smbtech.serviceframework.httpclient.port.out;

import com.smbtech.serviceframework.httpclient.domain.HttpClientDefinition;

import java.util.Map;

public interface HttpClientDefinitionSource {

    Map<String, HttpClientDefinition> loadDefinitions();
}
