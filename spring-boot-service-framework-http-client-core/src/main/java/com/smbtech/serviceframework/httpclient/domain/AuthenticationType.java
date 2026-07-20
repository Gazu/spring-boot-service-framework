package com.smbtech.serviceframework.httpclient.domain;

/** Defines supported authentication type values. */
public enum AuthenticationType {
    /** Represents no auth. */
    NO_AUTH,
    /** Represents basic auth. */
    BASIC_AUTH,
    /** Represents client credentials. */
    CLIENT_CREDENTIALS,
    /** Represents JWT bearer. */
    JWT_BEARER,
    /** Represents other. */
    OTHER
}
