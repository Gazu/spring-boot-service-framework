package com.smbtech.serviceframework.starter.errorhandling.customizer;

import com.smbtech.serviceframework.commons.notification.Notification;
import com.smbtech.serviceframework.error.ErrorCategory;
import com.smbtech.serviceframework.error.ResolvedError;
import com.smbtech.serviceframework.error.metadata.ConflictErrorMetadata;
import com.smbtech.serviceframework.error.metadata.HttpErrorMetadata;
import com.smbtech.serviceframework.error.metadata.RequestErrorMetadata;
import com.smbtech.serviceframework.error.metadata.ResourceErrorMetadata;
import com.smbtech.serviceframework.error.metadata.StandardErrorMetadata;
import com.smbtech.serviceframework.error.metadata.StandardErrorMetadataBuilder;
import com.smbtech.serviceframework.error.metadata.StandardErrorMetadataKeys;
import com.smbtech.serviceframework.error.metadata.ValidationErrorMetadata;
import com.smbtech.serviceframework.logging.port.out.CorrelationContext;
import com.smbtech.serviceframework.starter.errorhandling.api.ResolvedErrorCustomizer;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.core.Ordered;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.method.MethodValidationException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** Applies safe, request-aware standard metadata to every resolved MVC error. */
public final class StandardErrorMetadataCustomizer implements ResolvedErrorCustomizer {

    /** Correlation key shared with the logging starter. */
    public static final String TRANSACTION_ID_KEY = "transactionId";

    /** Runs after regular application customizers so category metadata remains consistent. */
    public static final int DEFAULT_ORDER = Ordered.LOWEST_PRECEDENCE - 100;

    private final CorrelationContext correlationContext;

    /** Creates the customizer without a correlation context. */
    public StandardErrorMetadataCustomizer() {
        this.correlationContext = null;
    }

    /**
     * Creates the customizer with correlation context lookup.
     *
     * @param correlationContext correlation context used to resolve identifiers
     */
    public StandardErrorMetadataCustomizer(CorrelationContext correlationContext) {
        this.correlationContext =
                Objects.requireNonNull(correlationContext, "correlationContext must not be null");
    }

    @Override
    public ResolvedError customize(
            Throwable cause, ResolvedError resolvedError, HttpServletRequest request) {
        Throwable failure = Objects.requireNonNull(cause, "cause must not be null");
        ResolvedError source =
                Objects.requireNonNull(resolvedError, "resolvedError must not be null");
        HttpServletRequest httpRequest =
                Objects.requireNonNull(request, "request must not be null");

        StandardErrorMetadataBuilder metadata =
                StandardErrorMetadata.builder(source.category())
                        .correlationId(correlationId())
                        .retryable(retryable(source.category()))
                        .request(requestMetadata(httpRequest));
        addCategoryMetadata(metadata, failure, source, httpRequest);
        return merge(source, metadata.buildMap());
    }

    @Override
    public int order() {
        return DEFAULT_ORDER;
    }

    private void addCategoryMetadata(
            StandardErrorMetadataBuilder metadata,
            Throwable cause,
            ResolvedError resolvedError,
            HttpServletRequest request) {
        switch (resolvedError.category()) {
            case VALIDATION ->
                    metadata.validation(new ValidationErrorMetadata(validationType(cause)));
            case NOT_FOUND -> metadata.resource(new ResourceErrorMetadata(resourceType(cause)));
            case CONFLICT -> metadata.conflict(new ConflictErrorMetadata("conflict", ""));
            case METHOD_NOT_ALLOWED, UNSUPPORTED_MEDIA_TYPE, NOT_ACCEPTABLE ->
                    metadata.http(httpMetadata(cause, request));
            default -> {}
        }
    }

    private String correlationId() {
        if (correlationContext == null) {
            return "";
        }
        try {
            return correlationContext.find(TRANSACTION_ID_KEY).orElse("");
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    private static Boolean retryable(ErrorCategory category) {
        return switch (category) {
            case RATE_LIMIT -> true;
            case DOWNSTREAM, INTERNAL -> null;
            default -> false;
        };
    }

    private static RequestErrorMetadata requestMetadata(HttpServletRequest request) {
        Object route = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        String method = Objects.requireNonNullElse(request.getMethod(), "");
        String routeTemplate = route == null ? "" : route.toString();
        if (method.isBlank() && routeTemplate.isBlank()) {
            return null;
        }
        return new RequestErrorMetadata(method, routeTemplate, "");
    }

    private static String validationType(Throwable cause) {
        if (cause instanceof HttpMessageNotReadableException) {
            return "malformed_json";
        }
        if (cause instanceof MethodArgumentTypeMismatchException) {
            return "type_mismatch";
        }
        if (cause instanceof MissingRequestHeaderException) {
            return "missing_header";
        }
        if (cause instanceof MissingServletRequestParameterException) {
            return "missing_parameter";
        }
        if (cause instanceof HandlerMethodValidationException
                || cause instanceof MethodValidationException
                || cause instanceof BindException) {
            return "bean_validation";
        }
        if (cause instanceof ServletRequestBindingException) {
            return "binding";
        }
        return "validation";
    }

    private static String resourceType(Throwable cause) {
        return cause instanceof NoResourceFoundException || cause instanceof NoHandlerFoundException
                ? "route"
                : "resource";
    }

    private static HttpErrorMetadata httpMetadata(Throwable cause, HttpServletRequest request) {
        if (cause instanceof HttpRequestMethodNotSupportedException exception) {
            return new HttpErrorMetadata(
                    request.getMethod(),
                    stringValues(exception.getSupportedMethods()),
                    "",
                    List.of(),
                    List.of());
        }
        if (cause instanceof HttpMediaTypeNotSupportedException exception) {
            return new HttpErrorMetadata(
                    request.getMethod(),
                    List.of(),
                    mediaTypeValue(exception.getContentType()),
                    mediaTypeValues(exception.getSupportedMediaTypes()),
                    List.of());
        }
        if (cause instanceof HttpMediaTypeNotAcceptableException exception) {
            return new HttpErrorMetadata(
                    request.getMethod(),
                    List.of(),
                    "",
                    List.of(),
                    mediaTypeValues(exception.getSupportedMediaTypes()));
        }
        return new HttpErrorMetadata(request.getMethod(), List.of(), "", List.of(), List.of());
    }

    private static List<String> stringValues(String[] values) {
        return values == null ? List.of() : Arrays.asList(values);
    }

    private static List<String> mediaTypeValues(List<MediaType> values) {
        return values == null ? List.of() : values.stream().map(MediaType::toString).toList();
    }

    private static String mediaTypeValue(MediaType value) {
        return value == null ? "" : value.toString();
    }

    private static ResolvedError merge(
            ResolvedError resolvedError, Map<String, Object> standardMetadata) {
        Notification source = resolvedError.notification();
        Map<String, Object> metadata = new LinkedHashMap<>(source.metadata());
        standardMetadata.forEach(
                (key, value) -> {
                    if (isFrameworkIdentityKey(key) || !metadata.containsKey(key)) {
                        metadata.put(key, value);
                    }
                });
        Notification notification =
                new Notification(
                        source.code(),
                        source.message(),
                        source.severity(),
                        source.fieldName(),
                        metadata,
                        source.id(),
                        source.timestamp());
        return new ResolvedError(
                notification,
                resolvedError.category(),
                resolvedError.exposure(),
                resolvedError.diagnosticMessage(),
                resolvedError.fieldViolations());
    }

    private static boolean isFrameworkIdentityKey(String key) {
        return StandardErrorMetadataKeys.SCHEMA_VERSION.equals(key)
                || StandardErrorMetadataKeys.CATEGORY.equals(key)
                || StandardErrorMetadataKeys.CORRELATION_ID.equals(key)
                || StandardErrorMetadataKeys.RETRYABLE.equals(key)
                || StandardErrorMetadataKeys.REQUEST.equals(key);
    }
}
