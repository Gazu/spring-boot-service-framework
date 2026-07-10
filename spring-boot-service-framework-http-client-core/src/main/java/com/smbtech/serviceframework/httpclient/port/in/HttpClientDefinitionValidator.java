package com.smbtech.serviceframework.httpclient.port.in;

import com.smbtech.serviceframework.httpclient.domain.HttpClientDefinition;

public interface HttpClientDefinitionValidator {

    void validate(HttpClientDefinition definition);
}
