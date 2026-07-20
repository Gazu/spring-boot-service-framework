package com.smbtech.serviceframework.mock.exception;

/** Provides mock exception behavior. */
public class MockException extends RuntimeException {

    /**
     * Creates a mock exception instance.
     *
     * @param message message value
     */
    public MockException(String message) {
        super(message);
    }

    /**
     * Creates a mock exception instance.
     *
     * @param message message value
     * @param cause cause value
     */
    public MockException(String message, Throwable cause) {
        super(message, cause);
    }
}
