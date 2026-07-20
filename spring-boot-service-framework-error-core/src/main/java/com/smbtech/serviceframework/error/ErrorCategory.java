package com.smbtech.serviceframework.error;

/** Framework-neutral classification of an application error. */
public enum ErrorCategory {
    /** Request input or domain validation failure. */
    VALIDATION,
    /** Missing or invalid authentication. */
    AUTHENTICATION,
    /** Insufficient permissions for an authenticated caller. */
    AUTHORIZATION,
    /** Requested resource does not exist. */
    NOT_FOUND,
    /** Request conflicts with the current resource state. */
    CONFLICT,
    /** Failure returned by a downstream dependency. */
    DOWNSTREAM,
    /** Request rejected because a rate limit was exceeded. */
    RATE_LIMIT,
    /** HTTP method is not supported by the requested endpoint. */
    METHOD_NOT_ALLOWED,
    /** Request media type is not supported by the endpoint. */
    UNSUPPORTED_MEDIA_TYPE,
    /** Endpoint cannot produce an acceptable response media type. */
    NOT_ACCEPTABLE,
    /** Unexpected internal application failure. */
    INTERNAL
}
