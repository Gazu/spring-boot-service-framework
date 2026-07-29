package com.smbtech.serviceframework.starter.errorhandling.customizer;

import com.smbtech.serviceframework.commons.notification.Notification;
import com.smbtech.serviceframework.error.ErrorExposure;
import com.smbtech.serviceframework.error.ResolvedError;
import com.smbtech.serviceframework.starter.errorhandling.api.ErrorExposurePolicy;
import com.smbtech.serviceframework.starter.errorhandling.api.NotificationResponseCustomizer;
import com.smbtech.serviceframework.starter.errorhandling.api.ResolvedErrorCustomizer;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.ToIntFunction;
import org.springframework.http.ResponseEntity;

/** Executes resolved-error and response customizers in deterministic order. */
public final class ErrorCustomizationPipeline {

    private final List<ResolvedErrorCustomizer> resolvedErrorCustomizers;
    private final List<NotificationResponseCustomizer> responseCustomizers;
    private final ErrorExposurePolicy errorExposurePolicy;

    /**
     * Creates an ordered customization pipeline.
     *
     * @param resolvedErrorCustomizers resolved error customizers
     * @param responseCustomizers response customizers
     */
    public ErrorCustomizationPipeline(
            List<? extends ResolvedErrorCustomizer> resolvedErrorCustomizers,
            List<? extends NotificationResponseCustomizer> responseCustomizers) {
        this(
                resolvedErrorCustomizers,
                responseCustomizers,
                resolvedError -> resolvedError.exposure());
    }

    /**
     * Creates an ordered customization pipeline with a final exposure policy.
     *
     * @param resolvedErrorCustomizers resolved error customizers
     * @param responseCustomizers response customizers
     * @param errorExposurePolicy final error exposure policy
     */
    public ErrorCustomizationPipeline(
            List<? extends ResolvedErrorCustomizer> resolvedErrorCustomizers,
            List<? extends NotificationResponseCustomizer> responseCustomizers,
            ErrorExposurePolicy errorExposurePolicy) {
        this.resolvedErrorCustomizers =
                orderedCopy(resolvedErrorCustomizers, ResolvedErrorCustomizer::order);
        this.responseCustomizers =
                orderedCopy(responseCustomizers, NotificationResponseCustomizer::order);
        this.errorExposurePolicy =
                Objects.requireNonNull(errorExposurePolicy, "errorExposurePolicy must not be null");
    }

    /**
     * Applies all resolved-error customizers.
     *
     * @param cause original failure
     * @param resolvedError initial resolved error
     * @param request current request
     * @return customized error
     */
    public ResolvedError customize(
            Throwable cause, ResolvedError resolvedError, HttpServletRequest request) {
        Throwable safeCause = Objects.requireNonNull(cause, "cause must not be null");
        HttpServletRequest safeRequest =
                Objects.requireNonNull(request, "request must not be null");
        ResolvedError current =
                Objects.requireNonNull(resolvedError, "resolvedError must not be null");
        for (ResolvedErrorCustomizer customizer : resolvedErrorCustomizers) {
            current =
                    Objects.requireNonNull(
                            customizer.customize(safeCause, current, safeRequest),
                            () ->
                                    "ResolvedErrorCustomizer "
                                            + customizer.getClass().getName()
                                            + " returned null");
        }
        return applyExposure(current);
    }

    /**
     * Applies all response customizers.
     *
     * @param response initial response
     * @param resolvedError resolved error
     * @param request current request
     * @return customized response
     */
    public ResponseEntity<Notification> customize(
            ResponseEntity<Notification> response,
            ResolvedError resolvedError,
            HttpServletRequest request) {
        ResolvedError safeError =
                Objects.requireNonNull(resolvedError, "resolvedError must not be null");
        HttpServletRequest safeRequest =
                Objects.requireNonNull(request, "request must not be null");
        ResponseEntity<Notification> current =
                Objects.requireNonNull(response, "response must not be null");
        for (NotificationResponseCustomizer customizer : responseCustomizers) {
            current =
                    Objects.requireNonNull(
                            customizer.customize(current, safeError, safeRequest),
                            () ->
                                    "NotificationResponseCustomizer "
                                            + customizer.getClass().getName()
                                            + " returned null");
        }
        return current;
    }

    /**
     * Returns resolved-error customizers in effective execution order.
     *
     * @return ordered resolved-error customizers
     */
    public List<ResolvedErrorCustomizer> resolvedErrorCustomizers() {
        return resolvedErrorCustomizers;
    }

    /**
     * Returns response customizers in effective execution order.
     *
     * @return ordered response customizers
     */
    public List<NotificationResponseCustomizer> responseCustomizers() {
        return responseCustomizers;
    }

    private ResolvedError applyExposure(ResolvedError resolvedError) {
        ErrorExposure exposure =
                Objects.requireNonNull(
                        errorExposurePolicy.resolve(resolvedError),
                        "ErrorExposurePolicy returned null");
        if (exposure == resolvedError.exposure()) {
            return resolvedError;
        }
        return resolvedError.withExposure(exposure);
    }

    private static <T> List<T> orderedCopy(List<? extends T> values, ToIntFunction<T> order) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        List<T> ordered = new ArrayList<>(values.size());
        for (T value : values) {
            ordered.add(Objects.requireNonNull(value, "customizer must not be null"));
        }
        ordered.sort(Comparator.comparingInt(order));
        return List.copyOf(ordered);
    }
}
