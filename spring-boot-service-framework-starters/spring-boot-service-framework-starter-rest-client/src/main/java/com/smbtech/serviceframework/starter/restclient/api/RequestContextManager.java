package com.smbtech.serviceframework.starter.restclient.api;

import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/** Defines the request context manager contract. */
public interface RequestContextManager {

    /**
     * Performs the current operation.
     *
     * @return current result
     */
    RequestContext current();

    /**
     * Performs the open operation.
     *
     * @param context context value
     * @return open result
     */
    RequestContextScope open(RequestContext context);

    /**
     * Performs the current headers operation.
     *
     * @return current headers result
     */
    default Map<String, String> currentHeaders() {
        return current().headers();
    }

    /**
     * Performs the current JWT bearer claims operation.
     *
     * @return current JWT bearer claims result
     */
    default Map<String, Object> currentJwtBearerClaims() {
        return current().jwtBearerClaims();
    }

    /**
     * Performs the open operation.
     *
     * @param headers headers value
     * @param jwtBearerClaims JWT bearer claims value
     * @return open result
     */
    default RequestContextScope open(
            Map<String, String> headers, Map<String, Object> jwtBearerClaims) {
        return open(RequestContext.of(headers, jwtBearerClaims));
    }

    /**
     * Performs the open headers operation.
     *
     * @param headers headers value
     * @return open headers result
     */
    default RequestContextScope openHeaders(Map<String, String> headers) {
        return open(RequestContext.ofHeaders(headers));
    }

    /**
     * Performs the open header operation.
     *
     * @param name name value
     * @param value header value
     * @return open header result
     */
    default RequestContextScope openHeader(String name, String value) {
        return open(RequestContext.builder().header(name, value).build());
    }

    /**
     * Performs the open JWT bearer claims operation.
     *
     * @param jwtBearerClaims JWT bearer claims value
     * @return open JWT bearer claims result
     */
    default RequestContextScope openJwtBearerClaims(Map<String, Object> jwtBearerClaims) {
        return open(RequestContext.ofJwtBearerClaims(jwtBearerClaims));
    }

    /**
     * Performs the open JWT bearer claim operation.
     *
     * @param name name value
     * @param value claim value
     * @return open JWT bearer claim result
     */
    default RequestContextScope openJwtBearerClaim(String name, Object value) {
        return open(RequestContext.builder().jwtBearerClaim(name, value).build());
    }

    /**
     * Performs the open operation.
     *
     * @param customizer customizer value
     * @return open result
     */
    default RequestContextScope open(Consumer<RequestContext.Builder> customizer) {
        Objects.requireNonNull(customizer, "customizer must not be null");
        RequestContext.Builder builder = RequestContext.builder();
        customizer.accept(builder);
        return open(builder.build());
    }
}
