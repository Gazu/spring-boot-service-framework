package com.smbtech.serviceframework.httpclient.port.out;

import java.util.Optional;

/** Defines the credential provider contract. */
public interface CredentialProvider {

    /**
     * Finds secret.
     *
     * @param key key value
     * @return find secret result
     */
    Optional<String> findSecret(String key);
}
