package com.smbtech.serviceframework.commons.notification;

import java.io.Serial;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Base runtime exception for failures that can expose one or more structured notifications in
 * addition to the normal exception message.
 *
 * @serial exclude
 */
public class NotifyingException extends RuntimeException {

    @Serial private static final long serialVersionUID = 4373313436990236182L;

    private static final String DEFAULT_MESSAGE = "Notification exception";

    private final List<Notification> notifications;

    /**
     * Creates an exception carrying one notification.
     *
     * @param notification notification to expose
     */
    public NotifyingException(Notification notification) {
        this(List.of(Objects.requireNonNull(notification, "notification must not be null")));
    }

    /**
     * Creates an exception carrying one notification and a cause.
     *
     * @param notification notification to expose
     * @param cause underlying failure
     */
    public NotifyingException(Notification notification, Throwable cause) {
        this(
                List.of(Objects.requireNonNull(notification, "notification must not be null")),
                defaultMessage(List.of(notification)),
                cause);
    }

    /**
     * Creates an exception carrying ordered notifications.
     *
     * @param notifications notifications to expose
     */
    public NotifyingException(List<Notification> notifications) {
        this(notifications, defaultMessage(notifications));
    }

    /**
     * Creates an exception carrying ordered notifications and a cause.
     *
     * @param notifications notifications to expose
     * @param cause underlying failure
     */
    public NotifyingException(List<Notification> notifications, Throwable cause) {
        this(notifications, defaultMessage(notifications), cause);
    }

    /**
     * Creates an exception without structured notifications.
     *
     * @param message diagnostic exception message
     */
    public NotifyingException(String message) {
        this(List.of(), message);
    }

    /**
     * Creates an exception without structured notifications.
     *
     * @param message diagnostic exception message
     * @param cause underlying failure
     */
    public NotifyingException(String message, Throwable cause) {
        this(List.of(), message, cause);
    }

    /**
     * Creates an exception with explicit notifications and diagnostic message.
     *
     * @param notifications notifications to expose
     * @param message diagnostic exception message
     */
    public NotifyingException(List<Notification> notifications, String message) {
        super(message);
        this.notifications = immutableNotifications(notifications);
    }

    /**
     * Creates an exception with explicit notifications, diagnostic message, and cause.
     *
     * @param notifications notifications to expose
     * @param message diagnostic exception message
     * @param cause underlying failure
     */
    public NotifyingException(List<Notification> notifications, String message, Throwable cause) {
        super(message, cause);
        this.notifications = immutableNotifications(notifications);
    }

    /**
     * Returns an immutable list of structured notifications associated with this exception.
     *
     * @return notifications
     */
    public List<Notification> notifications() {
        return notifications;
    }

    /**
     * Returns the first notification when this exception carries at least one.
     *
     * @return primary notification
     */
    public Optional<Notification> primaryNotification() {
        return notifications.stream().findFirst();
    }

    private static List<Notification> immutableNotifications(List<Notification> notifications) {
        if (notifications == null || notifications.isEmpty()) {
            return List.of();
        }
        return List.copyOf(notifications);
    }

    private static String defaultMessage(List<Notification> notifications) {
        if (notifications == null || notifications.isEmpty()) {
            return DEFAULT_MESSAGE;
        }

        Notification notification = notifications.getFirst();
        if (notification.message() != null && !notification.message().isBlank()) {
            return notification.message();
        }
        return notification.code();
    }
}
