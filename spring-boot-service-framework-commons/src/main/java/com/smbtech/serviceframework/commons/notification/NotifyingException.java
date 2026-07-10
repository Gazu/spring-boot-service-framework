package com.smbtech.serviceframework.commons.notification;

import java.io.Serial;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Base runtime exception for failures that can expose one or more structured
 * notifications in addition to the normal exception message.
 */
public class NotifyingException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 4373313436990236182L;

    private static final String DEFAULT_MESSAGE = "Notification exception";

    private final List<Notification> notifications;

    public NotifyingException(Notification notification) {
        this(List.of(Objects.requireNonNull(notification, "notification must not be null")));
    }

    public NotifyingException(Notification notification, Throwable cause) {
        this(
                List.of(Objects.requireNonNull(notification, "notification must not be null")),
                defaultMessage(List.of(notification)),
                cause
        );
    }

    public NotifyingException(List<Notification> notifications) {
        this(notifications, defaultMessage(notifications));
    }

    public NotifyingException(List<Notification> notifications, Throwable cause) {
        this(notifications, defaultMessage(notifications), cause);
    }

    public NotifyingException(String message) {
        this(List.of(), message);
    }

    public NotifyingException(String message, Throwable cause) {
        this(List.of(), message, cause);
    }

    public NotifyingException(List<Notification> notifications, String message) {
        super(message);
        this.notifications = immutableNotifications(notifications);
    }

    public NotifyingException(List<Notification> notifications, String message, Throwable cause) {
        super(message, cause);
        this.notifications = immutableNotifications(notifications);
    }

    /**
     * Returns an immutable list of structured notifications associated with
     * this exception.
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
