package com.smbtech.serviceframework.httpclient.port.out;

import java.util.Map;

/** Defines the correlation headers provider contract. */
public interface CorrelationHeadersProvider {

    /**
     * Performs the current headers operation.
     *
     * @return current headers result
     */
    Map<String, String> currentHeaders();
}
