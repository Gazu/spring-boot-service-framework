package com.smbtech.serviceframework.mock.port.out;

import com.smbtech.serviceframework.mock.domain.MockDefinition;
import com.smbtech.serviceframework.mock.domain.MockRequest;
import com.smbtech.serviceframework.mock.domain.MockResponse;

public interface MockResponseSource {

    MockResponse load(MockDefinition definition, MockRequest request);
}
