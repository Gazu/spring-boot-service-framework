package com.smbtech.serviceframework.error;

import com.smbtech.serviceframework.commons.notification.Notification;
import com.smbtech.serviceframework.commons.notification.NotifyingException;
import java.io.Serial;
import java.util.List;
import java.util.Objects;

/**
 * Application exception carrying one or more public notifications while retaining a separate
 * diagnostic message and cause.
 */
public class ServiceException extends NotifyingException {

    @Serial private static final long serialVersionUID = 7706153756870154286L;

    /** Category associated with the primary notification. */
    private final ErrorCategory category;

    /**
     * Creates an exception from one notification.
     *
     * @param notification public notification
     */
    public ServiceException(Notification notification) {
        this(List.of(requireNotification(notification)), ErrorCategory.INTERNAL, null, null);
    }

    /**
     * Creates an exception from one notification and a cause.
     *
     * @param notification public notification
     * @param cause underlying cause
     */
    public ServiceException(Notification notification, Throwable cause) {
        this(List.of(requireNotification(notification)), ErrorCategory.INTERNAL, null, cause);
    }

    /**
     * Creates an exception from one notification and a diagnostic message.
     *
     * @param notification public notification
     * @param diagnosticMessage internal diagnostic message
     */
    public ServiceException(Notification notification, String diagnosticMessage) {
        this(
                List.of(requireNotification(notification)),
                ErrorCategory.INTERNAL,
                diagnosticMessage,
                null);
    }

    /**
     * Creates an exception from one notification, a diagnostic message, and a cause.
     *
     * @param notification public notification
     * @param diagnosticMessage internal diagnostic message
     * @param cause underlying cause
     */
    public ServiceException(Notification notification, String diagnosticMessage, Throwable cause) {
        this(
                List.of(requireNotification(notification)),
                ErrorCategory.INTERNAL,
                diagnosticMessage,
                cause);
    }

    /**
     * Creates an exception from multiple notifications.
     *
     * @param notifications public notifications, ordered by priority
     */
    public ServiceException(List<Notification> notifications) {
        this(notifications, ErrorCategory.INTERNAL, null, null);
    }

    /**
     * Creates an exception from multiple notifications and a cause.
     *
     * @param notifications public notifications, ordered by priority
     * @param cause underlying cause
     */
    public ServiceException(List<Notification> notifications, Throwable cause) {
        this(notifications, ErrorCategory.INTERNAL, null, cause);
    }

    /**
     * Creates an exception from multiple notifications and a diagnostic message.
     *
     * @param notifications public notifications, ordered by priority
     * @param diagnosticMessage internal diagnostic message
     */
    public ServiceException(List<Notification> notifications, String diagnosticMessage) {
        this(notifications, ErrorCategory.INTERNAL, diagnosticMessage, null);
    }

    /**
     * Creates an exception from multiple notifications, a diagnostic message, and a cause.
     *
     * @param notifications public notifications, ordered by priority
     * @param diagnosticMessage internal diagnostic message
     * @param cause underlying cause
     */
    public ServiceException(
            List<Notification> notifications, String diagnosticMessage, Throwable cause) {
        this(notifications, ErrorCategory.INTERNAL, diagnosticMessage, cause);
    }

    private ServiceException(
            List<Notification> notifications,
            ErrorCategory category,
            String diagnosticMessage,
            Throwable cause) {
        super(
                requireNotifications(notifications),
                resolveDiagnosticMessage(notifications, diagnosticMessage),
                cause);
        this.category = Objects.requireNonNull(category, "category must not be null");
    }

    /**
     * Creates an exception from an application error definition.
     *
     * @param definition application error definition
     * @return service exception
     */
    public static ServiceException from(ErrorDefinition definition) {
        return from(definition, null, null);
    }

    /**
     * Creates an exception from an application error definition and a cause.
     *
     * @param definition application error definition
     * @param cause underlying cause
     * @return service exception
     */
    public static ServiceException from(ErrorDefinition definition, Throwable cause) {
        return from(definition, null, cause);
    }

    /**
     * Creates an exception from an application error definition and a diagnostic message.
     *
     * @param definition application error definition
     * @param diagnosticMessage internal diagnostic message
     * @return service exception
     */
    public static ServiceException from(ErrorDefinition definition, String diagnosticMessage) {
        return from(definition, diagnosticMessage, null);
    }

    /**
     * Creates an exception from an application error definition, a diagnostic message, and a cause.
     *
     * @param definition application error definition
     * @param diagnosticMessage internal diagnostic message
     * @param cause underlying cause
     * @return service exception
     */
    public static ServiceException from(
            ErrorDefinition definition, String diagnosticMessage, Throwable cause) {
        ErrorDefinition safeDefinition = requireDefinition(definition);
        return new ServiceException(
                List.of(toNotification(safeDefinition)),
                safeDefinition.category(),
                diagnosticMessage,
                cause);
    }

    /**
     * Creates an exception from multiple application error definitions.
     *
     * @param definitions application error definitions, ordered by priority
     * @return service exception
     */
    public static ServiceException from(List<? extends ErrorDefinition> definitions) {
        return from(definitions, null, null);
    }

    /**
     * Creates an exception from multiple definitions and a cause.
     *
     * @param definitions application error definitions, ordered by priority
     * @param cause underlying cause
     * @return service exception
     */
    public static ServiceException from(
            List<? extends ErrorDefinition> definitions, Throwable cause) {
        return from(definitions, null, cause);
    }

    /**
     * Creates an exception from multiple definitions and a diagnostic message.
     *
     * @param definitions application error definitions, ordered by priority
     * @param diagnosticMessage internal diagnostic message
     * @return service exception
     */
    public static ServiceException from(
            List<? extends ErrorDefinition> definitions, String diagnosticMessage) {
        return from(definitions, diagnosticMessage, null);
    }

    /**
     * Creates an exception from multiple definitions, a diagnostic message, and a cause.
     *
     * @param definitions application error definitions, ordered by priority
     * @param diagnosticMessage internal diagnostic message
     * @param cause underlying cause
     * @return service exception
     */
    public static ServiceException from(
            List<? extends ErrorDefinition> definitions,
            String diagnosticMessage,
            Throwable cause) {
        List<ErrorDefinition> safeDefinitions = requireDefinitions(definitions);
        List<Notification> notifications =
                safeDefinitions.stream().map(ServiceException::toNotification).toList();
        return new ServiceException(
                notifications, safeDefinitions.getFirst().category(), diagnosticMessage, cause);
    }

    /**
     * Returns the category associated with the primary notification.
     *
     * @return primary error category
     */
    public ErrorCategory category() {
        return category;
    }

    /**
     * Returns the internal diagnostic message. It must not be exposed to clients automatically.
     *
     * @return diagnostic message
     */
    public String diagnosticMessage() {
        return getMessage();
    }

    private static Notification requireNotification(Notification notification) {
        return Objects.requireNonNull(notification, "notification must not be null");
    }

    private static List<Notification> requireNotifications(List<Notification> notifications) {
        if (notifications == null || notifications.isEmpty()) {
            throw new IllegalArgumentException("notifications must not be empty");
        }
        return List.copyOf(notifications);
    }

    private static ErrorDefinition requireDefinition(ErrorDefinition definition) {
        ErrorDefinition safeDefinition =
                Objects.requireNonNull(definition, "definition must not be null");
        Objects.requireNonNull(safeDefinition.category(), "definition category must not be null");
        Objects.requireNonNull(safeDefinition.severity(), "definition severity must not be null");
        return safeDefinition;
    }

    private static List<ErrorDefinition> requireDefinitions(
            List<? extends ErrorDefinition> definitions) {
        if (definitions == null || definitions.isEmpty()) {
            throw new IllegalArgumentException("definitions must not be empty");
        }
        return definitions.stream().map(ServiceException::requireDefinition).toList();
    }

    private static Notification toNotification(ErrorDefinition definition) {
        return Notification.builder()
                .code(definition.code())
                .message(definition.publicMessage())
                .severity(definition.severity())
                .build();
    }

    private static String resolveDiagnosticMessage(
            List<Notification> notifications, String diagnosticMessage) {
        if (diagnosticMessage != null && !diagnosticMessage.isBlank()) {
            return diagnosticMessage;
        }
        Notification primary = requireNotifications(notifications).getFirst();
        return primary.message().isBlank() ? primary.code() : primary.message();
    }
}
