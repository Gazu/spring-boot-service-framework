package com.smbtech.serviceframework.mock.exception;

public class MockException extends RuntimeException {

    public MockException(String message) {
        super(message);
    }

    public MockException(String message, Throwable cause) {
        super(message, cause);
    }
}
