package com.smbtech.serviceframework.openapi.generator;

/** Defines supported open api change code values. */
public enum OpenApiChangeCode {
    /** Represents operation removed. */
    OPERATION_REMOVED,
    /** Represents operation added. */
    OPERATION_ADDED,
    /** Represents operation id changed. */
    OPERATION_ID_CHANGED,
    /** Represents parameter removed. */
    PARAMETER_REMOVED,
    /** Represents parameter added. */
    PARAMETER_ADDED,
    /** Represents parameter became required. */
    PARAMETER_BECAME_REQUIRED,
    /** Represents parameter became optional. */
    PARAMETER_BECAME_OPTIONAL,
    /** Represents request body removed. */
    REQUEST_BODY_REMOVED,
    /** Represents request body added. */
    REQUEST_BODY_ADDED,
    /** Represents request body became required. */
    REQUEST_BODY_BECAME_REQUIRED,
    /** Represents request body became optional. */
    REQUEST_BODY_BECAME_OPTIONAL,
    /** Represents response removed. */
    RESPONSE_REMOVED,
    /** Represents response added. */
    RESPONSE_ADDED,
    /** Represents media type removed. */
    MEDIA_TYPE_REMOVED,
    /** Represents media type added. */
    MEDIA_TYPE_ADDED,
    /** Represents schema removed. */
    SCHEMA_REMOVED,
    /** Represents schema added. */
    SCHEMA_ADDED,
    /** Represents property removed. */
    PROPERTY_REMOVED,
    /** Represents property added. */
    PROPERTY_ADDED,
    /** Represents property became required. */
    PROPERTY_BECAME_REQUIRED,
    /** Represents property became optional. */
    PROPERTY_BECAME_OPTIONAL,
    /** Represents type changed. */
    TYPE_CHANGED,
    /** Represents format changed. */
    FORMAT_CHANGED,
    /** Represents reference changed. */
    REFERENCE_CHANGED,
    /** Represents enum value removed. */
    ENUM_VALUE_REMOVED,
    /** Represents enum value added. */
    ENUM_VALUE_ADDED,
    /** Represents constraint tightened. */
    CONSTRAINT_TIGHTENED,
    /** Represents constraint relaxed. */
    CONSTRAINT_RELAXED,
    /** Represents composition changed. */
    COMPOSITION_CHANGED
}
