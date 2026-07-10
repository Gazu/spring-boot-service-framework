package com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.jwt;

import com.smbtech.serviceframework.httpclient.domain.GrantType;
import com.smbtech.serviceframework.httpclient.domain.TokenRequestDefinition;
import com.smbtech.serviceframework.httpclient.exception.AuthenticationException;
import com.smbtech.serviceframework.httpclient.port.out.JwtAssertionProvider;

import java.time.Clock;

public final class JwtBearerAccessTokenProvider implements JwtAssertionProvider {

    private final JwtAssertionFactory jwtAssertionFactory;
    private final Clock clock;

    public JwtBearerAccessTokenProvider(
            JwtAssertionFactory jwtAssertionFactory,
            Clock clock
    ) {
        this.jwtAssertionFactory = jwtAssertionFactory;
        this.clock = clock;
    }

    @Override
    public String createAssertion(TokenRequestDefinition definition) {
        if (definition.grantType() != GrantType.JWT_BEARER) {
            throw new AuthenticationException("Token request is not JWT_BEARER: " + definition.id());
        }
        if (!definition.jwtBearer().isConfigured()) {
            throw new AuthenticationException("jwtBearer.keyStoreId is required for token request: " + definition.id());
        }
        return jwtAssertionFactory.create(JwtBearerGrantRequest.from(definition, clock.instant()));
    }
}
