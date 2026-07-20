package com.smbtech.serviceframework.error;

/** Controls whether resolved error information is safe to expose to consumers. */
public enum ErrorExposure {
    /** The notification is safe to include in a public response. */
    PUBLIC,
    /** The notification requires replacement or sanitization before exposure. */
    INTERNAL
}
