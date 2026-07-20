package com.smbtech.serviceframework.openapi.contract;

/** Defines supported open api contract violation code values. */
public enum OpenApiContractViolationCode {
    /** Represents unknown operation. */
    UNKNOWN_OPERATION,
    /** Represents missing test case. */
    MISSING_TEST_CASE,
    /** Represents missing path parameter. */
    MISSING_PATH_PARAMETER,
    /** Represents missing request parameter. */
    MISSING_REQUEST_PARAMETER,
    /** Represents missing request body. */
    MISSING_REQUEST_BODY,
    /** Represents undeclared request body. */
    UNDECLARED_REQUEST_BODY,
    /** Represents undeclared request content type. */
    UNDECLARED_REQUEST_CONTENT_TYPE,
    /** Represents invalid json request. */
    INVALID_JSON_REQUEST,
    /** Represents request schema mismatch. */
    REQUEST_SCHEMA_MISMATCH,
    /** Represents request execution failed. */
    REQUEST_EXECUTION_FAILED,
    /** Represents unexpected status. */
    UNEXPECTED_STATUS,
    /** Represents undeclared status. */
    UNDECLARED_STATUS,
    /** Represents undeclared content type. */
    UNDECLARED_CONTENT_TYPE,
    /** Represents invalid json response. */
    INVALID_JSON_RESPONSE,
    /** Represents response schema mismatch. */
    RESPONSE_SCHEMA_MISMATCH
}
