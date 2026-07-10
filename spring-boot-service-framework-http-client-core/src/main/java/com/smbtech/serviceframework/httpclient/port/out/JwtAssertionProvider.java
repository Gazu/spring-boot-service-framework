package com.smbtech.serviceframework.httpclient.port.out;

import com.smbtech.serviceframework.httpclient.domain.TokenRequestDefinition;

public interface JwtAssertionProvider {

    String createAssertion(TokenRequestDefinition definition);
}
