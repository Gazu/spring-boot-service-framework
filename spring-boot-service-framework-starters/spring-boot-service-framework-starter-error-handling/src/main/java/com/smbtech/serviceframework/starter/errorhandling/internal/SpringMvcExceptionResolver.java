package com.smbtech.serviceframework.starter.errorhandling.internal;

import com.smbtech.serviceframework.commons.notification.Notification;
import com.smbtech.serviceframework.error.ErrorCategory;
import com.smbtech.serviceframework.error.ErrorExposure;
import com.smbtech.serviceframework.error.FieldViolation;
import com.smbtech.serviceframework.error.ResolvedError;
import com.smbtech.serviceframework.error.ThrowableErrorResolver;
import java.util.List;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** Resolves standard Spring MVC request failures into public notifications. */
final class SpringMvcExceptionResolver implements ThrowableErrorResolver {

    /** Runs after validation mapping and before downstream HTTP mapping. */
    public static final int DEFAULT_ORDER = -800;

    /** Creates the standard Spring MVC exception resolver. */
    public SpringMvcExceptionResolver() {}

    @Override
    public boolean supports(Throwable throwable) {
        return throwable instanceof HttpMessageNotReadableException
                || throwable instanceof NoResourceFoundException
                || throwable instanceof NoHandlerFoundException
                || throwable instanceof MissingRequestHeaderException
                || throwable instanceof MissingServletRequestParameterException
                || throwable instanceof ServletRequestBindingException
                || throwable instanceof HttpRequestMethodNotSupportedException
                || throwable instanceof HttpMediaTypeNotSupportedException
                || throwable instanceof HttpMediaTypeNotAcceptableException;
    }

    @Override
    public ResolvedError resolve(Throwable throwable) {
        if (throwable instanceof HttpMessageNotReadableException) {
            return error(
                    throwable,
                    "E_SERVICE_FRAMEWORK_JSON_0001",
                    "Request body is invalid",
                    ErrorCategory.VALIDATION);
        }
        if (throwable instanceof NoResourceFoundException
                || throwable instanceof NoHandlerFoundException) {
            return error(
                    throwable,
                    "E_SERVICE_FRAMEWORK_ROUTE_0001",
                    "Requested resource was not found",
                    ErrorCategory.NOT_FOUND);
        }
        if (throwable instanceof MissingRequestHeaderException missingHeader) {
            return error(
                    throwable,
                    "E_SERVICE_FRAMEWORK_HEADER_0001",
                    "Required request header is missing",
                    ErrorCategory.VALIDATION,
                    List.of(
                            new FieldViolation(
                                    missingHeader.getHeaderName(),
                                    "required",
                                    "Header is required")));
        }
        if (throwable instanceof MissingServletRequestParameterException missingParameter) {
            return error(
                    throwable,
                    "E_SERVICE_FRAMEWORK_PARAMETER_0001",
                    "Required request parameter is missing",
                    ErrorCategory.VALIDATION,
                    List.of(
                            new FieldViolation(
                                    missingParameter.getParameterName(),
                                    "required",
                                    "Parameter is required")));
        }
        if (throwable instanceof ServletRequestBindingException) {
            return error(
                    throwable,
                    "E_SERVICE_FRAMEWORK_BINDING_0002",
                    "Request binding failed",
                    ErrorCategory.VALIDATION);
        }
        if (throwable instanceof HttpRequestMethodNotSupportedException) {
            return error(
                    throwable,
                    "E_SERVICE_FRAMEWORK_METHOD_0001",
                    "HTTP method is not supported",
                    ErrorCategory.METHOD_NOT_ALLOWED);
        }
        if (throwable instanceof HttpMediaTypeNotSupportedException) {
            return error(
                    throwable,
                    "E_SERVICE_FRAMEWORK_MEDIA_TYPE_0001",
                    "Request media type is not supported",
                    ErrorCategory.UNSUPPORTED_MEDIA_TYPE);
        }
        if (throwable instanceof HttpMediaTypeNotAcceptableException) {
            return error(
                    throwable,
                    "E_SERVICE_FRAMEWORK_MEDIA_TYPE_0002",
                    "Requested response media type is not acceptable",
                    ErrorCategory.NOT_ACCEPTABLE);
        }
        throw new IllegalArgumentException(
                "Unsupported Spring MVC exception: " + throwable.getClass().getName());
    }

    @Override
    public int order() {
        return DEFAULT_ORDER;
    }

    private static ResolvedError error(
            Throwable throwable, String code, String message, ErrorCategory category) {
        return error(throwable, code, message, category, List.of());
    }

    private static ResolvedError error(
            Throwable throwable,
            String code,
            String message,
            ErrorCategory category,
            List<FieldViolation> violations) {
        return new ResolvedError(
                Notification.error(code, message),
                category,
                ErrorExposure.PUBLIC,
                diagnosticMessage(throwable),
                violations);
    }

    private static String diagnosticMessage(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null || message.isBlank()
                ? throwable.getClass().getName()
                : throwable.getClass().getName() + ": " + message;
    }
}
