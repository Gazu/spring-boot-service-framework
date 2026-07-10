package com.smbtech.serviceframework.starter.restclient.autoconfigure;

import com.smbtech.serviceframework.httpclient.exception.AuthenticationException;
import com.smbtech.serviceframework.httpclient.port.out.CredentialProvider;

public final class CredentialResolver {

    private final CredentialProvider credentialProvider;

    public CredentialResolver(CredentialProvider credentialProvider) {
        this.credentialProvider = credentialProvider;
    }

    public String resolve(String directValue, String credentialRef, String fieldName) {
        if (credentialRef != null && !credentialRef.isBlank()) {
            return credentialProvider.findSecret(credentialRef)
                    .orElseThrow(() -> new AuthenticationException(
                            "Credential not configured for " + fieldName + ": " + credentialRef
                    ));
        }
        return directValue;
    }
}
