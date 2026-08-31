package com.smbtech.serviceframework.mock.port.in;

import com.smbtech.serviceframework.mock.domain.MockRequest;
import com.smbtech.serviceframework.mock.domain.MockResponse;
import com.smbtech.serviceframework.mock.port.out.MockResponseSource;
import java.util.Optional;

/** Defines the mock responder contract. */
@FunctionalInterface
public interface MockResponder {

    /**
     * Creates the default responder from the catalog and response source ports.
     *
     * @param catalog mock catalog
     * @param responseSource response source
     * @return default mock responder
     */
    static MockResponder from(MockCatalog catalog, MockResponseSource responseSource) {
        return new DefaultMockResponder(catalog, responseSource);
    }

    /**
     * Performs the respond operation.
     *
     * @param request request value
     * @return respond result
     */
    Optional<MockResponse> respond(MockRequest request);
}
