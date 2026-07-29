package com.smbtech.serviceframework.error;

/**
 * Selects the intended HTTP response audience and its level of detail.
 *
 * <p>Both modes require sanitization and never expose internal diagnostics, exception causes, stack
 * traces, credentials, or raw downstream content.
 */
public enum ErrorExposure {
    /** Produces a minimal response suitable for untrusted or external consumers. */
    PUBLIC,
    /** Produces a detailed, sanitized response intended for trusted internal consumers. */
    INTERNAL
}
