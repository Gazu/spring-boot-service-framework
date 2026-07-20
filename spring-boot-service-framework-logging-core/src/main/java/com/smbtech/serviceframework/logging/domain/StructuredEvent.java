package com.smbtech.serviceframework.logging.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Immutable event passed through the logging ports.
 *
 * @param type event classification
 * @param message message template
 * @param arguments ordered message-template arguments
 * @param data structured event attributes
 * @param tags searchable event tags
 * @param sensitivity data sensitivity classification
 * @param throwable related failure, when present
 */
public record StructuredEvent(
        EventType type,
        String message,
        List<Object> arguments,
        Map<String, Object> data,
        Set<String> tags,
        Sensitivity sensitivity,
        Throwable throwable) {

    /**
     * Creates an immutable event and applies safe defaults to nullable values.
     *
     * @param type event classification
     * @param message message template
     * @param arguments ordered message-template arguments
     * @param data structured event attributes
     * @param tags searchable event tags
     * @param sensitivity data sensitivity classification
     * @param throwable related failure, when present
     */
    public StructuredEvent {
        type = type == null ? EventType.APPLICATION : type;
        message = message == null ? "" : message;
        arguments = arguments == null ? List.of() : List.copyOf(arguments);
        data = data == null ? Map.of() : Map.copyOf(data);
        tags = tags == null ? Set.of() : Set.copyOf(tags);
        sensitivity = sensitivity == null ? Sensitivity.PUBLIC : sensitivity;
    }

    /**
     * Starts a builder for an application event.
     *
     * @return event builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Starts a builder with an explicit event type.
     *
     * @param type event classification
     * @return event builder
     */
    public static Builder builder(EventType type) {
        return new Builder().type(type);
    }

    /**
     * Returns the related failure when one was attached.
     *
     * @return optional failure
     */
    public Optional<Throwable> failure() {
        return Optional.ofNullable(throwable);
    }

    /**
     * Reports whether the event may contain sensitive data.
     *
     * @return {@code true} for sensitive events
     */
    public boolean isSensitive() {
        return sensitivity == Sensitivity.SENSITIVE;
    }

    /** Mutable construction boundary that produces an immutable event. */
    public static final class Builder {
        private EventType type = EventType.APPLICATION;
        private String message = "";
        private final List<Object> arguments = new ArrayList<>();
        private final Map<String, Object> data = new LinkedHashMap<>();
        private final Set<String> tags = new LinkedHashSet<>();
        private Sensitivity sensitivity = Sensitivity.PUBLIC;
        private Throwable throwable;

        /** Creates an empty application event builder. */
        public Builder() {}

        /**
         * Sets the event type from its external name.
         *
         * @param type external event type name
         * @return this builder
         */
        public Builder type(String type) {
            return type(EventType.named(type));
        }

        /**
         * Sets the event classification.
         *
         * @param type event classification
         * @return this builder
         */
        public Builder type(EventType type) {
            this.type = type;
            return this;
        }

        /**
         * Sets the message template and its arguments.
         *
         * @param message message template
         * @param arguments ordered template arguments
         * @return this builder
         */
        public Builder message(String message, Object... arguments) {
            this.message = message;
            this.arguments.clear();
            if (arguments != null) {
                this.arguments.addAll(Arrays.asList(arguments));
            }
            return this;
        }

        /**
         * Adds or replaces one structured event attribute.
         *
         * @param key attribute key
         * @param value attribute value
         * @return this builder
         */
        public Builder with(String key, Object value) {
            if (key == null || key.isBlank()) {
                throw new IllegalArgumentException("Data key may not be null or blank");
            }
            this.data.put(key, value);
            return this;
        }

        /**
         * Builds and adds a nested structured attribute map.
         *
         * @param key attribute key
         * @param values nested-map customizer
         * @return this builder
         */
        public Builder with(String key, Consumer<Map<String, Object>> values) {
            if (values == null) {
                throw new IllegalArgumentException("Values consumer may not be null");
            }
            Map<String, Object> nested = new LinkedHashMap<>();
            values.accept(nested);
            return with(key, Map.copyOf(nested));
        }

        /**
         * Adds a searchable event tag.
         *
         * @param tag tag value
         * @return this builder
         */
        public Builder tag(String tag) {
            if (tag == null || tag.isBlank()) {
                throw new IllegalArgumentException("Tag may not be null or blank");
            }
            this.tags.add(tag);
            return this;
        }

        /**
         * Marks the event as containing sensitive data.
         *
         * @return this builder
         */
        public Builder sensitive() {
            this.sensitivity = Sensitivity.SENSITIVE;
            return this;
        }

        /**
         * Attaches a failure to the event.
         *
         * @param throwable related failure
         * @return this builder
         */
        public Builder throwable(Throwable throwable) {
            this.throwable = throwable;
            return this;
        }

        /**
         * Builds an immutable structured event.
         *
         * @return constructed event
         */
        public StructuredEvent build() {
            return new StructuredEvent(
                    type, message, arguments, data, tags, sensitivity, throwable);
        }
    }
}
