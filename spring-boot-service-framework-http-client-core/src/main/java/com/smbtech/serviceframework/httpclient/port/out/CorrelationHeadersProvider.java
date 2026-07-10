package com.smbtech.serviceframework.httpclient.port.out;

import java.util.Map;

public interface CorrelationHeadersProvider {

    Map<String, String> currentHeaders();
}
