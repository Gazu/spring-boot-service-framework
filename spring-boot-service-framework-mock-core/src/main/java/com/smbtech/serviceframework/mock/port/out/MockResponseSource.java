package com.smbtech.serviceframework.mock.port.out;

import com.smbtech.serviceframework.mock.domain.MockDefinition;
import com.smbtech.serviceframework.mock.domain.MockRequest;
import com.smbtech.serviceframework.mock.domain.MockResponse;

/** Defines the mock response source contract. */
public interface MockResponseSource {

    /**
     * Performs the load operation.
     *
     * @param definition definition value
     * @param request request value
     * @return load result
     */
    MockResponse load(MockDefinition definition, MockRequest request);
}
