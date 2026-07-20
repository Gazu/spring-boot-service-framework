package com.smbtech.serviceframework.error.metadata;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TreeSet;

final class MetadataValues {

    private MetadataValues() {}

    static String requireText(String value, String field) {
        String normalized = optionalText(value);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return normalized;
    }

    static String optionalText(String value) {
        return Objects.requireNonNullElse(value, "").trim();
    }

    static String optionalUppercase(String value) {
        String normalized = optionalText(value);
        return normalized.isEmpty() ? normalized : normalized.toUpperCase(Locale.ROOT);
    }

    static List<String> sortedTexts(Collection<String> values, String field) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        TreeSet<String> normalized = new TreeSet<>();
        for (String value : values) {
            normalized.add(requireText(value, field));
        }
        return List.copyOf(normalized);
    }

    static List<String> sortedUppercaseTexts(Collection<String> values, String field) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        TreeSet<String> normalized = new TreeSet<>();
        for (String value : values) {
            normalized.add(requireText(value, field).toUpperCase(Locale.ROOT));
        }
        return List.copyOf(normalized);
    }

    static String optionalAbsoluteUri(String value, String field) {
        String normalized = optionalText(value);
        if (normalized.isEmpty()) {
            return normalized;
        }
        URI uri;
        try {
            uri = URI.create(normalized);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(field + " must be a valid absolute URI", exception);
        }
        if (!uri.isAbsolute()) {
            throw new IllegalArgumentException(field + " must be an absolute URI");
        }
        return uri.toASCIIString();
    }
}
