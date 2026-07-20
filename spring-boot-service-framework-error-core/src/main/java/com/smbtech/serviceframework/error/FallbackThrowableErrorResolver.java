package com.smbtech.serviceframework.error;

import com.smbtech.serviceframework.commons.notification.Notification;
import java.util.Objects;

/** Produces a safe internal error when no specialized resolver supports a failure. */
public final class FallbackThrowableErrorResolver implements ThrowableErrorResolver {

    /** Prevents unresolved failures from exposing implementation-specific codes. */
    public static final String DEFAULT_ERROR_CODE = "E_SERVICE_FRAMEWORK_INTERNAL_0001";

    /** Prevents unresolved failures from exposing exception messages. */
    public static final String DEFAULT_PUBLIC_MESSAGE = "The request could not be completed";

    /** Creates the default fallback resolver. */
    public FallbackThrowableErrorResolver() {}

    @Override
    public boolean supports(Throwable throwable) {
        return throwable != null;
    }

    @Override
    public ResolvedError resolve(Throwable throwable) {
        Throwable safeThrowable = Objects.requireNonNull(throwable, "throwable must not be null");
        return new ResolvedError(
                Notification.error(DEFAULT_ERROR_CODE, DEFAULT_PUBLIC_MESSAGE),
                ErrorCategory.INTERNAL,
                ErrorExposure.INTERNAL,
                diagnosticMessage(safeThrowable));
    }

    @Override
    public int order() {
        return LOWEST_PRECEDENCE;
    }

    private static String diagnosticMessage(Throwable throwable) {
        String message = throwable.getMessage();
        if (message == null || message.isBlank()) {
            return throwable.getClass().getName();
        }
        return throwable.getClass().getName() + ": " + message;
    }
}
