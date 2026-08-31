package com.smbtech.serviceframework.starter.errorhandling.internal;

import com.smbtech.serviceframework.commons.notification.Notification;
import com.smbtech.serviceframework.error.ErrorCategory;
import com.smbtech.serviceframework.error.ErrorExposure;
import com.smbtech.serviceframework.error.FieldViolation;
import com.smbtech.serviceframework.error.ResolvedError;
import com.smbtech.serviceframework.error.ThrowableErrorResolver;
import java.util.ArrayList;
import java.util.List;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.validation.method.MethodValidationException;
import org.springframework.validation.method.MethodValidationResult;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Resolves Spring MVC validation and binding failures. */
final class ValidationExceptionResolver implements ThrowableErrorResolver {

    /** Identifies constraint violations in the stable public error catalog. */
    public static final String VALIDATION_ERROR_CODE = "E_SERVICE_FRAMEWORK_VALIDATION_0001";

    /** Distinguishes request binding failures from constraint violations. */
    public static final String BINDING_ERROR_CODE = "E_SERVICE_FRAMEWORK_BINDING_0001";

    /** Runs validation mapping before broader Spring MVC resolvers. */
    public static final int DEFAULT_ORDER = -900;

    /** Creates the Spring MVC validation resolver. */
    public ValidationExceptionResolver() {}

    @Override
    public boolean supports(Throwable throwable) {
        return throwable instanceof BindException
                || throwable instanceof HandlerMethodValidationException
                || throwable instanceof MethodValidationException
                || throwable instanceof MethodArgumentTypeMismatchException;
    }

    @Override
    public ResolvedError resolve(Throwable throwable) {
        if (throwable instanceof BindException bindException) {
            return validationError(bindException, bindingViolations(bindException));
        }
        if (throwable instanceof HandlerMethodValidationException validationException) {
            return methodValidationError(validationException, validationException);
        }
        if (throwable instanceof MethodValidationException validationException) {
            return internalMethodValidationError(validationException);
        }
        if (throwable instanceof MethodArgumentTypeMismatchException mismatchException) {
            return bindingError(
                    mismatchException,
                    List.of(
                            new FieldViolation(
                                    mismatchException.getName(),
                                    "type_mismatch",
                                    "The value has an invalid type")));
        }
        throw new IllegalArgumentException(
                "Unsupported validation exception: " + throwable.getClass().getName());
    }

    @Override
    public int order() {
        return DEFAULT_ORDER;
    }

    private static ResolvedError validationError(
            Throwable throwable, List<FieldViolation> violations) {
        return publicError(
                throwable, VALIDATION_ERROR_CODE, "Request validation failed", violations);
    }

    private static ResolvedError bindingError(
            Throwable throwable, List<FieldViolation> violations) {
        return publicError(throwable, BINDING_ERROR_CODE, "Request binding failed", violations);
    }

    private static ResolvedError publicError(
            Throwable throwable, String code, String message, List<FieldViolation> violations) {
        return new ResolvedError(
                Notification.error(code, message),
                ErrorCategory.VALIDATION,
                ErrorExposure.PUBLIC,
                diagnosticMessage(throwable),
                violations);
    }

    private static ResolvedError methodValidationError(
            HandlerMethodValidationException throwable, MethodValidationResult validationResult) {
        if (throwable.isForReturnValue()) {
            return internalMethodValidationError(throwable);
        }
        return validationError(throwable, methodViolations(validationResult));
    }

    private static ResolvedError internalMethodValidationError(Throwable throwable) {
        return new ResolvedError(
                Notification.error(
                        "E_SERVICE_FRAMEWORK_INTERNAL_VALIDATION_0001", "Validation failed"),
                ErrorCategory.INTERNAL,
                ErrorExposure.INTERNAL,
                diagnosticMessage(throwable));
    }

    private static List<FieldViolation> bindingViolations(BindException exception) {
        List<FieldViolation> violations = new ArrayList<>();
        for (FieldError error : exception.getFieldErrors()) {
            violations.add(
                    new FieldViolation(error.getField(), errorCode(error), errorMessage(error)));
        }
        for (ObjectError error : exception.getGlobalErrors()) {
            violations.add(new FieldViolation("", errorCode(error), errorMessage(error)));
        }
        return List.copyOf(violations);
    }

    private static List<FieldViolation> methodViolations(MethodValidationResult validationResult) {
        List<FieldViolation> violations = new ArrayList<>();
        for (ParameterValidationResult parameterResult :
                validationResult.getParameterValidationResults()) {
            String fieldName = parameterName(parameterResult);
            for (MessageSourceResolvable error : parameterResult.getResolvableErrors()) {
                violations.add(
                        new FieldViolation(fieldName, errorCode(error), errorMessage(error)));
            }
        }
        for (MessageSourceResolvable error :
                validationResult.getCrossParameterValidationResults()) {
            violations.add(new FieldViolation("", errorCode(error), errorMessage(error)));
        }
        return List.copyOf(violations);
    }

    private static String parameterName(ParameterValidationResult result) {
        String name = result.getMethodParameter().getParameterName();
        return name == null || name.isBlank()
                ? "arg" + result.getMethodParameter().getParameterIndex()
                : name;
    }

    private static String errorCode(MessageSourceResolvable error) {
        String[] codes = error.getCodes();
        if (codes == null || codes.length == 0 || codes[0] == null || codes[0].isBlank()) {
            return "invalid";
        }
        return codes[0];
    }

    private static String errorMessage(MessageSourceResolvable error) {
        String message = error.getDefaultMessage();
        return message == null || message.isBlank() ? "The value is invalid" : message;
    }

    private static String diagnosticMessage(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null || message.isBlank()
                ? throwable.getClass().getName()
                : throwable.getClass().getName() + ": " + message;
    }
}
