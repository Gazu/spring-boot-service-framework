package com.smbtech.serviceframework.httpclient.domain;

/** Defines supported grant type values. */
public enum GrantType {
    /** Represents client credentials. */
    CLIENT_CREDENTIALS("client_credentials"),
    /** Represents JWT bearer. */
    JWT_BEARER("urn:ietf:params:oauth:grant-type:jwt-bearer");

    private final String value;

    GrantType(String value) {
        this.value = value;
    }

    /**
     * Performs the value operation.
     *
     * @return value result
     */
    public String value() {
        return value;
    }
}
