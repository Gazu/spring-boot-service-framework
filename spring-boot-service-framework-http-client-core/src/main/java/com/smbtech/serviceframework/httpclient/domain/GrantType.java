package com.smbtech.serviceframework.httpclient.domain;

public enum GrantType {
    CLIENT_CREDENTIALS("client_credentials"),
    JWT_BEARER("urn:ietf:params:oauth:grant-type:jwt-bearer");

    private final String value;

    GrantType(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
