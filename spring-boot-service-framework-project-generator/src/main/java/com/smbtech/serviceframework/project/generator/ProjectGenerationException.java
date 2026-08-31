package com.smbtech.serviceframework.project.generator;

/** Raised when a contract cannot produce a valid project scaffold. */
public final class ProjectGenerationException extends RuntimeException {

    /**
     * Creates an exception with a diagnostic message.
     *
     * @param message diagnostic message
     */
    public ProjectGenerationException(String message) {
        super(message);
    }

    /**
     * Creates an exception with a diagnostic message and cause.
     *
     * @param message diagnostic message
     * @param cause underlying failure
     */
    public ProjectGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
