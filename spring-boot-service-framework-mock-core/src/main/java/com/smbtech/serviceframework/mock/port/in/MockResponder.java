package com.smbtech.serviceframework.mock.port.in;

import com.smbtech.serviceframework.mock.domain.MockRequest;
import com.smbtech.serviceframework.mock.domain.MockResponse;

import java.util.Optional;

@FunctionalInterface
public interface MockResponder {

    Optional<MockResponse> respond(MockRequest request);
}
