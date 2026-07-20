package com.smbtech.serviceframework.error.metadata;

/**
 * Canonical Java keys used by the standard error metadata contract. HTTP serializers may normalize
 * these keys to snake case.
 */
public final class StandardErrorMetadataKeys {

    /** Identifies the metadata contract revision used by a response. */
    public static final String SCHEMA_VERSION = "schemaVersion";

    /** Classifies the resolved failure. */
    public static final String CATEGORY = "category";

    /** Links a public response to logs and traces. */
    public static final String CORRELATION_ID = "correlationId";

    /** Indicates whether retrying the operation can be appropriate. */
    public static final String RETRYABLE = "retryable";

    /** Contains safe request context. */
    public static final String REQUEST = "request";

    /** Describes the validation failure group. */
    public static final String VALIDATION = "validation";

    /** Contains individual input violations. */
    public static final String VIOLATIONS = "violations";

    /** Describes an authentication or authorization failure. */
    public static final String SECURITY = "security";

    /** Contains RFC-compatible OAuth2 failure details. */
    public static final String OAUTH2 = "oauth2";

    /** Identifies the affected resource without exposing its contents. */
    public static final String RESOURCE = "resource";

    /** Describes the operation that conflicted with current state. */
    public static final String CONFLICT = "conflict";

    /** Identifies a failed downstream dependency safely. */
    public static final String DEPENDENCY = "dependency";

    /** Describes retry timing for throttled requests. */
    public static final String RATE_LIMIT = "rateLimit";

    /** Contains protocol-level HTTP failure details. */
    public static final String HTTP = "http";

    private StandardErrorMetadataKeys() {}

    /** Request namespace keys. */
    public static final class Request {
        /** Records the incoming HTTP verb. */
        public static final String METHOD = "method";

        /** Records a normalized route rather than a raw sensitive URI. */
        public static final String ROUTE = "route";

        /** Correlates the failure with a documented API operation. */
        public static final String OPERATION_ID = "operationId";

        private Request() {}
    }

    /** Validation namespace keys. */
    public static final class Validation {
        /** Distinguishes validation failure families. */
        public static final String TYPE = "type";

        private Validation() {}
    }

    /** Field violation keys. */
    public static final class Violation {
        /** Identifies the rejected input member. */
        public static final String FIELD_NAME = "fieldName";

        /** Locates the rejected value in the request. */
        public static final String LOCATION = "location";

        /** Provides a stable machine-readable constraint identifier. */
        public static final String CODE = "code";

        /** Provides a safe human-readable constraint description. */
        public static final String MESSAGE = "message";

        private Violation() {}
    }

    /** Security namespace keys. */
    public static final class Security {
        /** Provides a bounded machine-readable security outcome. */
        public static final String REASON = "reason";

        /** Identifies the authentication protocol without exposing credentials. */
        public static final String AUTHENTICATION_SCHEME = "authenticationScheme";

        private Security() {}
    }

    /** OAuth2 namespace keys. */
    public static final class OAuth2 {
        /** Carries the RFC-defined Bearer error identifier. */
        public static final String ERROR = "error";

        /** Carries a framework-controlled public failure description. */
        public static final String ERROR_DESCRIPTION = "errorDescription";

        /** Points consumers to the applicable protocol specification. */
        public static final String ERROR_URI = "errorUri";

        /** Exposes only the scope required by the authorization policy. */
        public static final String SCOPE = "scope";

        private OAuth2() {}
    }

    /** Resource namespace keys. */
    public static final class Resource {
        /** Names the resource kind without exposing an identifier or payload. */
        public static final String TYPE = "type";

        private Resource() {}
    }

    /** Conflict namespace keys. */
    public static final class Conflict {
        /** Classifies the state conflict. */
        public static final String TYPE = "type";

        /** Identifies the operation rejected by current state. */
        public static final String OPERATION = "operation";

        private Conflict() {}
    }

    /** Dependency namespace keys. */
    public static final class Dependency {
        /** Identifies a dependency using its configured safe name. */
        public static final String NAME = "name";

        /** Identifies the failed downstream operation. */
        public static final String OPERATION = "operation";

        /** Classifies the downstream failure without exposing its response. */
        public static final String FAILURE_TYPE = "failureType";

        private Dependency() {}
    }

    /** Rate-limit namespace keys. */
    public static final class RateLimit {
        /** Communicates a bounded client retry delay. */
        public static final String RETRY_AFTER_SECONDS = "retryAfterSeconds";

        private RateLimit() {}
    }

    /** HTTP namespace keys. */
    public static final class Http {
        /** Identifies the rejected request method. */
        public static final String METHOD = "method";

        /** Lists methods accepted by the matched endpoint. */
        public static final String ALLOWED_METHODS = "allowedMethods";

        /** Identifies the unsupported request representation. */
        public static final String CONTENT_TYPE = "contentType";

        /** Lists representations accepted by the endpoint. */
        public static final String SUPPORTED_MEDIA_TYPES = "supportedMediaTypes";

        /** Lists response representations acceptable to the client. */
        public static final String ACCEPTABLE_MEDIA_TYPES = "acceptableMediaTypes";

        private Http() {}
    }
}
