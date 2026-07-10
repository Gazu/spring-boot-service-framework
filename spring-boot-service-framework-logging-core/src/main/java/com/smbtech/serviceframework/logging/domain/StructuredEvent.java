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
 */
public record StructuredEvent(
        EventType type,
        String message,
        List<Object> arguments,
        Map<String, Object> data,
        Set<String> tags,
        Sensitivity sensitivity,
        Throwable throwable
) {

    public StructuredEvent {
        type = type == null ? EventType.APPLICATION : type;
        message = message == null ? "" : message;
        arguments = arguments == null ? List.of() : List.copyOf(arguments);
        data = data == null ? Map.of() : Map.copyOf(data);
        tags = tags == null ? Set.of() : Set.copyOf(tags);
        sensitivity = sensitivity == null ? Sensitivity.PUBLIC : sensitivity;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(EventType type) {
        return new Builder().type(type);
    }

    public Optional<Throwable> failure() {
        return Optional.ofNullable(throwable);
    }

    public boolean isSensitive() {
        return sensitivity == Sensitivity.SENSITIVE;
    }

    /**
     * Mutable construction boundary that produces an immutable event.
     */
    public static final class Builder {
        private EventType type = EventType.APPLICATION;
        private String message = "";
        private final List<Object> arguments = new ArrayList<>();
        private final Map<String, Object> data = new LinkedHashMap<>();
        private final Set<String> tags = new LinkedHashSet<>();
        private Sensitivity sensitivity = Sensitivity.PUBLIC;
        private Throwable throwable;

        public Builder type(String type) {
            return type(EventType.named(type));
        }

        public Builder type(EventType type) {
            this.type = type;
            return this;
        }

        public Builder message(String message, Object... arguments) {
            this.message = message;
            this.arguments.clear();
            if (arguments != null) {
                this.arguments.addAll(Arrays.asList(arguments));
            }
            return this;
        }

        public Builder with(String key, Object value) {
            if (key == null || key.isBlank()) {
                throw new IllegalArgumentException("Data key may not be null or blank");
            }
            this.data.put(key, value);
            return this;
        }

        public Builder with(String key, Consumer<Map<String, Object>> values) {
            if (values == null) {
                throw new IllegalArgumentException("Values consumer may not be null");
            }
            Map<String, Object> nested = new LinkedHashMap<>();
            values.accept(nested);
            return with(key, Map.copyOf(nested));
        }

        public Builder tag(String tag) {
            if (tag == null || tag.isBlank()) {
                throw new IllegalArgumentException("Tag may not be null or blank");
            }
            this.tags.add(tag);
            return this;
        }

        public Builder sensitive() {
            this.sensitivity = Sensitivity.SENSITIVE;
            return this;
        }

        public Builder throwable(Throwable throwable) {
            this.throwable = throwable;
            return this;
        }

        public StructuredEvent build() {
            return new StructuredEvent(
                    type,
                    message,
                    arguments,
                    data,
                    tags,
                    sensitivity,
                    throwable
            );
        }
    }
}
