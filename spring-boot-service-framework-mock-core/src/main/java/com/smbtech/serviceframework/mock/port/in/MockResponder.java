package com.smbtech.serviceframework.mock.port.in;

import com.smbtech.serviceframework.mock.domain.MockRequest;
import com.smbtech.serviceframework.mock.domain.MockResponse;
import java.util.Optional;

/** Defines the mock responder contract. */
@FunctionalInterface
public interface MockResponder {

    /**
     * Performs the respond operation.
     *
     * @param request request value
     * @return respond result
     */
    Optional<MockResponse> respond(MockRequest request);
}
