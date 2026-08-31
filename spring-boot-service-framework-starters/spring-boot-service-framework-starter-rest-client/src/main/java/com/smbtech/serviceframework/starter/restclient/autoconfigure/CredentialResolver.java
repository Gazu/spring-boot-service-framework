package com.smbtech.serviceframework.starter.restclient.autoconfigure;

import com.smbtech.serviceframework.httpclient.exception.HttpClientAuthenticationException;
import com.smbtech.serviceframework.httpclient.port.out.CredentialProvider;

final class CredentialResolver {

    private final CredentialProvider credentialProvider;

    /**
     * Creates a credential resolver instance.
     *
     * @param credentialProvider credential provider value
     */
    CredentialResolver(CredentialProvider credentialProvider) {
        this.credentialProvider = credentialProvider;
    }

    /**
     * Performs the resolve operation.
     *
     * @param directValue direct value
     * @param credentialRef credential ref value
     * @param fieldName field name value
     * @return resolve result
     */
    String resolve(String directValue, String credentialRef, String fieldName) {
        if (credentialRef != null && !credentialRef.isBlank()) {
            return credentialProvider
                    .findSecret(credentialRef)
                    .orElseThrow(
                            () ->
                                    new HttpClientAuthenticationException(
                                            "Credential not configured for "
                                                    + fieldName
                                                    + ": "
                                                    + credentialRef));
        }
        return directValue;
    }
}
