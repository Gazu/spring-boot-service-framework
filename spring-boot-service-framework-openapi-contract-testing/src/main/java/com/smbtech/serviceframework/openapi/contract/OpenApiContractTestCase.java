package com.smbtech.serviceframework.openapi.contract;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Provides open api contract test case behavior. */
public final class OpenApiContractTestCase {

    private final String operationId;
    private final Map<String, String> pathParameters;
    private final Map<String, String> queryParameters;
    private final Map<String, String> headers;
    private final Map<String, String> cookies;
    private final String requestBody;
    private final String requestContentType;
    private final Integer expectedStatus;

    private OpenApiContractTestCase(Builder builder) {
        this.operationId = builder.operationId;
        this.pathParameters = Map.copyOf(builder.pathParameters);
        this.queryParameters = Map.copyOf(builder.queryParameters);
        this.headers = Map.copyOf(builder.headers);
        this.cookies = Map.copyOf(builder.cookies);
        this.requestBody = builder.requestBody;
        this.requestContentType = builder.requestContentType;
        this.expectedStatus = builder.expectedStatus;
    }

    /**
     * Performs the for operation operation.
     *
     * @param operationId operation id value
     * @return for result
     */
    public static Builder forOperation(String operationId) {
        return new Builder(operationId);
    }

    /**
     * Performs the operation id operation.
     *
     * @return operation id result
     */
    public String operationId() {
        return operationId;
    }

    /**
     * Performs the path parameters operation.
     *
     * @return path parameters result
     */
    public Map<String, String> pathParameters() {
        return pathParameters;
    }

    /**
     * Performs the query parameters operation.
     *
     * @return query parameters result
     */
    public Map<String, String> queryParameters() {
        return queryParameters;
    }

    /**
     * Performs the headers operation.
     *
     * @return headers result
     */
    public Map<String, String> headers() {
        return headers;
    }

    /**
     * Performs the cookies operation.
     *
     * @return cookies result
     */
    public Map<String, String> cookies() {
        return cookies;
    }

    /**
     * Performs the request body operation.
     *
     * @return request body result
     */
    public String requestBody() {
        return requestBody;
    }

    /**
     * Performs the request content type operation.
     *
     * @return request content type result
     */
    public String requestContentType() {
        return requestContentType;
    }

    /**
     * Performs the expected status operation.
     *
     * @return expected status result
     */
    public Integer expectedStatus() {
        return expectedStatus;
    }

    /** Provides builder behavior. */
    public static final class Builder {

        private final String operationId;
        private final Map<String, String> pathParameters = new LinkedHashMap<>();
        private final Map<String, String> queryParameters = new LinkedHashMap<>();
        private final Map<String, String> headers = new LinkedHashMap<>();
        private final Map<String, String> cookies = new LinkedHashMap<>();
        private String requestBody;
        private String requestContentType;
        private Integer expectedStatus;

        private Builder(String operationId) {
            this.operationId = requireText(operationId, "operationId");
        }

        /**
         * Performs the path parameter operation.
         *
         * @param name name value
         * @param value path parameter value
         * @return path parameter result
         */
        public Builder pathParameter(String name, String value) {
            pathParameters.put(
                    requireText(name, "path parameter name"),
                    requireText(value, "path parameter value"));
            return this;
        }

        /**
         * Performs the query parameter operation.
         *
         * @param name name value
         * @param value query parameter value
         * @return query parameter result
         */
        public Builder queryParameter(String name, String value) {
            queryParameters.put(
                    requireText(name, "query parameter name"),
                    Objects.requireNonNull(value, "query parameter value"));
            return this;
        }

        /**
         * Performs the header operation.
         *
         * @param name name value
         * @param value header value
         * @return header result
         */
        public Builder header(String name, String value) {
            headers.put(
                    requireText(name, "header name"),
                    Objects.requireNonNull(value, "header value"));
            return this;
        }

        /**
         * Performs the cookie operation.
         *
         * @param name name value
         * @param value cookie value
         * @return cookie result
         */
        public Builder cookie(String name, String value) {
            cookies.put(
                    requireText(name, "cookie name"),
                    Objects.requireNonNull(value, "cookie value"));
            return this;
        }

        /**
         * Performs the json body operation.
         *
         * @param requestBody request body value
         * @return json body result
         */
        public Builder jsonBody(String requestBody) {
            return body(requestBody, "application/json");
        }

        /**
         * Performs the body operation.
         *
         * @param requestBody request body value
         * @param contentType content type value
         * @return body result
         */
        public Builder body(String requestBody, String contentType) {
            this.requestBody = Objects.requireNonNull(requestBody, "requestBody");
            this.requestContentType = requireText(contentType, "contentType");
            return this;
        }

        /**
         * Performs the expected status operation.
         *
         * @param expectedStatus expected status value
         * @return expected status result
         */
        public Builder expectedStatus(int expectedStatus) {
            if (expectedStatus < 100 || expectedStatus > 599) {
                throw new IllegalArgumentException("expectedStatus must be between 100 and 599");
            }
            this.expectedStatus = expectedStatus;
            return this;
        }

        /**
         * Creates the result.
         *
         * @return build result
         */
        public OpenApiContractTestCase build() {
            return new OpenApiContractTestCase(this);
        }

        private static String requireText(String value, String name) {
            String safeValue = Objects.requireNonNull(value, name).trim();
            if (safeValue.isEmpty()) {
                throw new IllegalArgumentException(name + " must not be blank");
            }
            return safeValue;
        }
    }
}
