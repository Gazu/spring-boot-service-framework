package com.smbtech.serviceframework.logging.domain;

/** Logging levels understood by the application core. */
public enum LogLevel {
    /** Finest-grained diagnostic output. */
    TRACE,
    /** Diagnostic output intended for development and troubleshooting. */
    DEBUG,
    /** Normal operational information. */
    INFO,
    /** Unexpected condition that does not stop the operation. */
    WARN,
    /** Failed operation. */
    ERROR
}
