package com.smbtech.serviceframework.logging.domain;

/** Indicates whether an event may contain personally identifiable information. */
public enum Sensitivity {
    /** Event contains no known personally identifiable information. */
    PUBLIC,
    /** Event may contain personally identifiable or otherwise sensitive data. */
    SENSITIVE
}
