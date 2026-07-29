package com.smbtech.serviceframework.starter.restclient.api;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Carries immutable request context data.
 *
 * @param headers headers value
 * @param jwtBearerClaims JWT bearer claims value
 */
public record RequestContext(Map<String, String> headers, Map<String, Object> jwtBearerClaims) {

    private static final RequestContext EMPTY = new RequestContext(Map.of(), Map.of());

    /** Creates and validates the record components. */
    public RequestContext {
        headers = immutableHeaders(headers);
        jwtBearerClaims = immutableClaims(jwtBearerClaims);
    }

    /**
     * Performs the empty operation.
     *
     * @return empty result
     */
    public static RequestContext empty() {
        return EMPTY;
    }

    /**
     * Creates the result.
     *
     * @param headers headers value
     * @param jwtBearerClaims JWT bearer claims value
     * @return of result
     */
    public static RequestContext of(
            Map<String, String> headers, Map<String, Object> jwtBearerClaims) {
        return new RequestContext(headers, jwtBearerClaims);
    }

    /**
     * Creates headers.
     *
     * @param headers headers value
     * @return of headers result
     */
    public static RequestContext ofHeaders(Map<String, String> headers) {
        return new RequestContext(headers, Map.of());
    }

    /**
     * Creates JWT bearer claims.
     *
     * @param jwtBearerClaims JWT bearer claims value
     * @return of JWT bearer claims result
     */
    public static RequestContext ofJwtBearerClaims(Map<String, Object> jwtBearerClaims) {
        return new RequestContext(Map.of(), jwtBearerClaims);
    }

    /**
     * Creates er.
     *
     * @return builder result
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates er.
     *
     * @param context context value
     * @return builder result
     */
    public static Builder builder(RequestContext context) {
        RequestContext safeContext = Objects.requireNonNull(context, "context must not be null");
        return new Builder()
                .headers(safeContext.headers)
                .jwtBearerClaims(safeContext.jwtBearerClaims);
    }

    /**
     * Reports whether empty.
     *
     * @return is empty result
     */
    public boolean isEmpty() {
        return headers.isEmpty() && jwtBearerClaims.isEmpty();
    }

    /**
     * Performs the to builder operation.
     *
     * @return to builder result
     */
    public Builder toBuilder() {
        return builder(this);
    }

    /**
     * Performs the with header operation.
     *
     * @param name name value
     * @param value header value
     * @return with header result
     */
    public RequestContext withHeader(String name, String value) {
        return builder(this).header(name, value).build();
    }

    /**
     * Performs the with headers operation.
     *
     * @param values values value
     * @return with headers result
     */
    public RequestContext withHeaders(Map<String, String> values) {
        return builder(this).headers(values).build();
    }

    /**
     * Performs the with JWT bearer claim operation.
     *
     * @param name name value
     * @param value claim value
     * @return with JWT bearer claim result
     */
    public RequestContext withJwtBearerClaim(String name, Object value) {
        return builder(this).jwtBearerClaim(name, value).build();
    }

    /**
     * Performs the with JWT bearer claims operation.
     *
     * @param values values value
     * @return with JWT bearer claims result
     */
    public RequestContext withJwtBearerClaims(Map<String, Object> values) {
        return builder(this).jwtBearerClaims(values).build();
    }

    private static Map<String, String> immutableHeaders(Map<String, String> values) {
        LinkedHashMap<String, String> copy = new LinkedHashMap<>();
        Objects.requireNonNullElse(values, Map.<String, String>of())
                .forEach(
                        (name, value) ->
                                copy.put(
                                        normalizeName(name, "header name"),
                                        requireValue(value, name)));
        return Collections.unmodifiableMap(copy);
    }

    private static Map<String, Object> immutableClaims(Map<String, Object> values) {
        LinkedHashMap<String, Object> copy = new LinkedHashMap<>();
        Objects.requireNonNullElse(values, Map.<String, Object>of())
                .forEach(
                        (name, value) ->
                                copy.put(
                                        normalizeName(name, "claim name"),
                                        requireValue(value, name)));
        return ImmutableRequestValues.structuredMap(copy);
    }

    private static String normalizeName(String value, String name) {
        String normalized = Objects.requireNonNull(value, name + " must not be null").trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return normalized;
    }

    private static <T> T requireValue(T value, String name) {
        return Objects.requireNonNull(value, "value must not be null for " + name);
    }

    /** Provides builder behavior. */
    public static final class Builder {
        private final Map<String, String> headers = new LinkedHashMap<>();
        private final Map<String, Object> jwtBearerClaims = new LinkedHashMap<>();

        private Builder() {}

        /**
         * Performs the header operation.
         *
         * @param name name value
         * @param value header value
         * @return header result
         */
        public Builder header(String name, String value) {
            headers.put(normalizeName(name, "header name"), requireValue(value, name));
            return this;
        }

        /**
         * Performs the headers operation.
         *
         * @param values values value
         * @return headers result
         */
        public Builder headers(Map<String, String> values) {
            immutableHeaders(values).forEach(headers::put);
            return this;
        }

        /**
         * Performs the JWT bearer claim operation.
         *
         * @param name name value
         * @param value claim value
         * @return JWT bearer claim result
         */
        public Builder jwtBearerClaim(String name, Object value) {
            jwtBearerClaims.put(normalizeName(name, "claim name"), requireValue(value, name));
            return this;
        }

        /**
         * Performs the JWT bearer claims operation.
         *
         * @param values values value
         * @return JWT bearer claims result
         */
        public Builder jwtBearerClaims(Map<String, Object> values) {
            immutableClaims(values).forEach(jwtBearerClaims::put);
            return this;
        }

        /**
         * Creates the result.
         *
         * @return build result
         */
        public RequestContext build() {
            return new RequestContext(headers, jwtBearerClaims);
        }
    }
}
