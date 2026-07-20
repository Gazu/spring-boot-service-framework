package com.smbtech.serviceframework.starter.errorhandling.adapter.in.web;

import com.smbtech.serviceframework.error.ErrorCategory;
import com.smbtech.serviceframework.error.ResolvedError;
import com.smbtech.serviceframework.starter.errorhandling.api.NotificationHttpStatusResolver;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

/** Maps error categories to their default HTTP status. */
public final class DefaultNotificationHttpStatusResolver implements NotificationHttpStatusResolver {

    /** Creates the default HTTP status resolver. */
    public DefaultNotificationHttpStatusResolver() {}

    @Override
    public HttpStatusCode resolve(ResolvedError resolvedError) {
        ErrorCategory category =
                Objects.requireNonNull(resolvedError, "resolvedError must not be null").category();
        return switch (category) {
            case VALIDATION -> HttpStatus.BAD_REQUEST;
            case AUTHENTICATION -> HttpStatus.UNAUTHORIZED;
            case AUTHORIZATION -> HttpStatus.FORBIDDEN;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CONFLICT -> HttpStatus.CONFLICT;
            case DOWNSTREAM -> HttpStatus.BAD_GATEWAY;
            case RATE_LIMIT -> HttpStatus.TOO_MANY_REQUESTS;
            case METHOD_NOT_ALLOWED -> HttpStatus.METHOD_NOT_ALLOWED;
            case UNSUPPORTED_MEDIA_TYPE -> HttpStatus.UNSUPPORTED_MEDIA_TYPE;
            case NOT_ACCEPTABLE -> HttpStatus.NOT_ACCEPTABLE;
            case INTERNAL -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
