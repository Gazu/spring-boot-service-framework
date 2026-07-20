package com.smbtech.serviceframework.httpclient.domain;

/** Defines supported client authentication method values. */
public enum ClientAuthenticationMethod {
    /** Represents client secret basic. */
    CLIENT_SECRET_BASIC,
    /** Represents client secret post. */
    CLIENT_SECRET_POST,
    /** Represents private key JWT. */
    PRIVATE_KEY_JWT,
    /** Represents none. */
    NONE
}
