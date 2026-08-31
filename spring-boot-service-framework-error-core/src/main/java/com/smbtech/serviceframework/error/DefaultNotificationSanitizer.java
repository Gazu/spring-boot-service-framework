package com.smbtech.serviceframework.error;

import com.smbtech.serviceframework.commons.notification.Notification;
import java.lang.reflect.Array;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Applies a metadata allowlist and recursively redacts sensitive values from response
 * notifications.
 */
final class DefaultNotificationSanitizer implements NotificationSanitizer {

    /** Value used in place of sensitive content. */
    static final String REDACTED_VALUE = NotificationSanitizer.REDACTED_VALUE;

    /** Default metadata keys that may be included in responses. */
    static final Set<String> DEFAULT_METADATA_ALLOWLIST =
            NotificationSanitizer.DEFAULT_METADATA_ALLOWLIST;

    private static final int MAX_DEPTH = 10;
    private static final Pattern AUTHORIZATION_PATTERN =
            Pattern.compile("(?i)\\b(Bearer|Basic)\\s+[A-Za-z0-9._~+/=-]+");
    private static final Pattern JWT_PATTERN =
            Pattern.compile("\\b[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{10,}\\b");
    private static final Pattern LONG_SECRET_PATTERN =
            Pattern.compile("(?<![A-Za-z0-9+/=_-])[A-Za-z0-9+/=_-]{40,}(?![A-Za-z0-9+/=_-])");
    private static final Pattern SENSITIVE_ASSIGNMENT_PATTERN =
            Pattern.compile(
                    "(?i)(authorization|assertion|credential|password|passwd|secret|token|api[_-]?key)"
                            + "(\\s*[=:]\\s*)[^\\s,;&}]+");
    private static final List<String> SENSITIVE_KEY_PARTS =
            List.of(
                    "authorization",
                    "assertion",
                    "credential",
                    "password",
                    "passwd",
                    "secret",
                    "token",
                    "apikey",
                    "privatekey",
                    "clientsecret",
                    "cookie",
                    "session",
                    "keystore",
                    "keypassword");
    private static final List<String> RESTRICTED_CONTENT_KEY_PARTS =
            List.of("header", "body", "claim", "cause", "exception", "stacktrace");

    private final Set<String> metadataAllowlist;

    /** Normalized metadata allowlist used for case-insensitive matching. */
    private final Set<String> normalizedMetadataAllowlist;

    /** Creates a sanitizer with the default response metadata allowlist. */
    DefaultNotificationSanitizer() {
        this(DEFAULT_METADATA_ALLOWLIST);
    }

    /**
     * Creates a sanitizer with a custom top-level metadata allowlist.
     *
     * @param metadataAllowlist metadata keys allowed in response notifications
     */
    DefaultNotificationSanitizer(Set<String> metadataAllowlist) {
        this.metadataAllowlist = immutableAllowlist(metadataAllowlist);
        this.normalizedMetadataAllowlist =
                this.metadataAllowlist.stream()
                        .map(DefaultNotificationSanitizer::normalizeKey)
                        .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    @Override
    public Notification sanitize(Notification notification) {
        Notification source = Objects.requireNonNull(notification, "notification must not be null");
        return new Notification(
                source.code(),
                sanitizeText(source.message()),
                source.severity(),
                sanitizeText(source.fieldName()),
                sanitizeMetadata(source.metadata()),
                source.id(),
                source.timestamp());
    }

    @Override
    public ResolvedError sanitize(ResolvedError resolvedError) {
        ResolvedError source =
                Objects.requireNonNull(resolvedError, "resolvedError must not be null");
        List<FieldViolation> fieldViolations =
                source.fieldViolations().stream()
                        .map(
                                violation ->
                                        new FieldViolation(
                                                violation.fieldName(),
                                                violation.code(),
                                                sanitizeText(violation.message())))
                        .toList();
        return source.withNotification(sanitize(source.notification()))
                .withFieldViolations(fieldViolations);
    }

    /**
     * Returns the immutable configured metadata allowlist.
     *
     * @return metadata allowlist
     */
    public Set<String> metadataAllowlist() {
        return metadataAllowlist;
    }

    /**
     * Redacts sensitive material embedded in text.
     *
     * @param value source text
     * @return sanitized text
     */
    static String sanitizeText(String value) {
        if (value == null || value.isBlank()) {
            return Objects.requireNonNullElse(value, "");
        }
        String sanitized = AUTHORIZATION_PATTERN.matcher(value).replaceAll("$1 " + REDACTED_VALUE);
        sanitized = JWT_PATTERN.matcher(sanitized).replaceAll(REDACTED_VALUE);
        sanitized = LONG_SECRET_PATTERN.matcher(sanitized).replaceAll(REDACTED_VALUE);
        return SENSITIVE_ASSIGNMENT_PATTERN.matcher(sanitized).replaceAll("$1$2" + REDACTED_VALUE);
    }

    private Map<String, Object> sanitizeMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty() || metadataAllowlist.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> sanitized = new LinkedHashMap<>();
        IdentityHashMap<Object, Boolean> visited = new IdentityHashMap<>();
        metadata.forEach(
                (key, value) -> {
                    if (isAllowed(key)) {
                        sanitized.put(key, sanitizeValue(key, value, 0, visited));
                    }
                });
        return Collections.unmodifiableMap(sanitized);
    }

    private Object sanitizeValue(
            String key, Object value, int depth, IdentityHashMap<Object, Boolean> visited) {
        if (value == null) {
            return null;
        }
        if (isSensitiveKey(key) || isRestrictedContentKey(key) || value instanceof Throwable) {
            return REDACTED_VALUE;
        }
        if (value instanceof CharSequence sequence) {
            return sanitizeText(sequence.toString());
        }
        if (isSafeScalar(value)) {
            return value;
        }
        if (depth >= MAX_DEPTH || visited.put(value, Boolean.TRUE) != null) {
            return REDACTED_VALUE;
        }
        try {
            if (value instanceof Map<?, ?> map) {
                return sanitizeMap(map, depth + 1, visited);
            }
            if (value instanceof Collection<?> collection) {
                return sanitizeCollection(collection, depth + 1, visited);
            }
            if (value.getClass().isArray()) {
                return sanitizeArray(value, depth + 1, visited);
            }
            return REDACTED_VALUE;
        } finally {
            visited.remove(value);
        }
    }

    private Map<String, Object> sanitizeMap(
            Map<?, ?> source, int depth, IdentityHashMap<Object, Boolean> visited) {
        Map<String, Object> sanitized = new LinkedHashMap<>();
        source.forEach(
                (key, value) -> {
                    if (key != null) {
                        String name = key.toString();
                        sanitized.put(name, sanitizeValue(name, value, depth, visited));
                    }
                });
        return Collections.unmodifiableMap(sanitized);
    }

    private List<Object> sanitizeCollection(
            Collection<?> source, int depth, IdentityHashMap<Object, Boolean> visited) {
        List<Object> sanitized = new ArrayList<>(source.size());
        for (Object value : source) {
            sanitized.add(sanitizeValue("", value, depth, visited));
        }
        return Collections.unmodifiableList(sanitized);
    }

    private List<Object> sanitizeArray(
            Object source, int depth, IdentityHashMap<Object, Boolean> visited) {
        int length = Array.getLength(source);
        List<Object> sanitized = new ArrayList<>(length);
        for (int index = 0; index < length; index++) {
            sanitized.add(sanitizeValue("", Array.get(source, index), depth, visited));
        }
        return Collections.unmodifiableList(sanitized);
    }

    private boolean isAllowed(String key) {
        return key != null && normalizedMetadataAllowlist.contains(normalizeKey(key));
    }

    private static boolean isSensitiveKey(String key) {
        String normalized = normalizeKey(key);
        return SENSITIVE_KEY_PARTS.stream().anyMatch(normalized::contains);
    }

    private static boolean isRestrictedContentKey(String key) {
        String normalized = normalizeKey(key);
        return RESTRICTED_CONTENT_KEY_PARTS.stream().anyMatch(normalized::contains);
    }

    private static String normalizeKey(String key) {
        return Objects.requireNonNullElse(key, "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]", "");
    }

    private static boolean isSafeScalar(Object value) {
        return value instanceof Number
                || value instanceof Boolean
                || value instanceof Character
                || value instanceof Enum<?>
                || value instanceof UUID
                || value instanceof TemporalAccessor;
    }

    private static Set<String> immutableAllowlist(Set<String> allowlist) {
        Objects.requireNonNull(allowlist, "metadataAllowlist must not be null");
        Set<String> copy = new LinkedHashSet<>();
        for (String key : allowlist) {
            if (key == null || key.isBlank()) {
                throw new IllegalArgumentException("metadata allowlist keys must not be blank");
            }
            copy.add(key.trim());
        }
        return Collections.unmodifiableSet(copy);
    }
}
