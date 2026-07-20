package com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring;

import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

final class JwtBearerAuthorizedClientCacheKey {

    private JwtBearerAuthorizedClientCacheKey() {}

    static String principalName(String principalName, Map<String, Object> customClaims) {
        if (customClaims == null || customClaims.isEmpty()) {
            return principalName;
        }
        return principalName + ":jwt-bearer:" + sha256(canonicalize(customClaims));
    }

    static String canonicalize(Map<?, ?> claims) {
        return claims.entrySet().stream()
                .sorted(Comparator.comparing(entry -> String.valueOf(entry.getKey())))
                .map(
                        entry ->
                                text(String.valueOf(entry.getKey()))
                                        + "="
                                        + canonicalizeValue(entry.getValue()))
                .collect(Collectors.joining(";", "m{", "}"));
    }

    private static String canonicalizeValue(Object value) {
        if (value == null) {
            return "n";
        }
        if (value instanceof Map<?, ?> map) {
            return canonicalize(map);
        }
        if (value instanceof Iterable<?> iterable) {
            return StreamSupport.stream(iterable.spliterator(), false)
                    .map(JwtBearerAuthorizedClientCacheKey::canonicalizeValue)
                    .collect(Collectors.joining(",", "l[", "]"));
        }
        if (value.getClass().isArray()) {
            return IntStream.range(0, Array.getLength(value))
                    .mapToObj(index -> canonicalizeValue(Array.get(value, index)))
                    .collect(Collectors.joining(",", "a[", "]"));
        }
        if (value instanceof Number number) {
            return "number:" + text(number.toString());
        }
        if (value instanceof Boolean bool) {
            return "boolean:" + bool;
        }
        return "string:" + text(String.valueOf(value));
    }

    private static String text(String value) {
        return value.length() + ":" + value;
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
