package com.smbtech.serviceframework.starter.errorhandling.autoconfigure;

import com.smbtech.serviceframework.error.ErrorExposure;
import com.smbtech.serviceframework.error.NotificationSanitizer;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuration properties for reusable HTTP error handling. */
@ConfigurationProperties("smbtech.error-handling")
public class ErrorHandlingProperties {

    private static final String DEFAULT_METRIC_NAME = "smbtech.error.handling.errors";

    /** Creates a error handling properties instance. */
    public ErrorHandlingProperties() {}

    private boolean enabled = true;
    private final Response response = new Response();
    private final Logging logging = new Logging();
    private final Metrics metrics = new Metrics();
    private final Security security = new Security();

    /**
     * Reports whether enabled.
     *
     * @return is enabled result
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Sets the configured enabled.
     *
     * @param enabled enabled value
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Returns the configured response.
     *
     * @return get response result
     */
    public Response getResponse() {
        return response;
    }

    /**
     * Returns the configured logging.
     *
     * @return get logging result
     */
    public Logging getLogging() {
        return logging;
    }

    /**
     * Returns the configured metrics.
     *
     * @return get metrics result
     */
    public Metrics getMetrics() {
        return metrics;
    }

    /**
     * Returns the configured security.
     *
     * @return get security result
     */
    public Security getSecurity() {
        return security;
    }

    /** HTTP notification response settings. */
    public static class Response {
        /** Creates a response instance. */
        public Response() {}

        private ErrorExposure exposure = ErrorExposure.PUBLIC;
        private boolean includeFieldViolations = true;
        private Set<String> metadataAllowlist =
                new LinkedHashSet<>(NotificationSanitizer.DEFAULT_METADATA_ALLOWLIST);

        /**
         * Returns the configured response audience and detail level.
         *
         * @return response audience and detail level
         */
        public ErrorExposure getExposure() {
            return exposure;
        }

        /**
         * Sets the configured response audience and detail level.
         *
         * @param exposure response audience and detail level
         */
        public void setExposure(ErrorExposure exposure) {
            this.exposure = exposure;
        }

        /**
         * Reports whether detailed internal responses include field violations.
         *
         * @return whether internal responses include field violations
         */
        public boolean isIncludeFieldViolations() {
            return includeFieldViolations;
        }

        /**
         * Sets whether detailed internal responses include field violations.
         *
         * @param includeFieldViolations whether internal responses include field violations
         */
        public void setIncludeFieldViolations(boolean includeFieldViolations) {
            this.includeFieldViolations = includeFieldViolations;
        }

        /**
         * Returns the metadata allowlist applied to detailed internal responses.
         *
         * @return internal response metadata allowlist
         */
        public Set<String> getMetadataAllowlist() {
            return metadataAllowlist;
        }

        /**
         * Sets the metadata allowlist applied to detailed internal responses.
         *
         * @param metadataAllowlist internal response metadata allowlist
         */
        public void setMetadataAllowlist(Set<String> metadataAllowlist) {
            this.metadataAllowlist =
                    metadataAllowlist == null
                            ? new LinkedHashSet<>()
                            : new LinkedHashSet<>(metadataAllowlist);
        }
    }

    /** Structured error logging settings. */
    public static class Logging {
        /** Creates a logging instance. */
        public Logging() {}

        private boolean enabled = true;
        private boolean includeDiagnostics = true;

        /**
         * Reports whether enabled.
         *
         * @return is enabled result
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * Sets the configured enabled.
         *
         * @param enabled enabled value
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * Reports whether include diagnostics.
         *
         * @return is include diagnostics result
         */
        public boolean isIncludeDiagnostics() {
            return includeDiagnostics;
        }

        /**
         * Sets the configured include diagnostics.
         *
         * @param includeDiagnostics include diagnostics value
         */
        public void setIncludeDiagnostics(boolean includeDiagnostics) {
            this.includeDiagnostics = includeDiagnostics;
        }
    }

    /** Micrometer error counter settings. */
    public static class Metrics {
        /** Creates a metrics instance. */
        public Metrics() {}

        private boolean enabled = true;
        private String metricName = DEFAULT_METRIC_NAME;

        /**
         * Reports whether enabled.
         *
         * @return is enabled result
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * Sets the configured enabled.
         *
         * @param enabled enabled value
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * Returns the configured metric name.
         *
         * @return get metric name result
         */
        public String getMetricName() {
            return metricName;
        }

        /**
         * Sets the configured metric name.
         *
         * @param metricName metric name value
         */
        public void setMetricName(String metricName) {
            this.metricName = metricName;
        }
    }

    /** Spring Security adapter settings. */
    public static class Security {
        /** Creates a security instance. */
        public Security() {}

        private boolean enabled = true;
        private final OAuth2Metadata oauth2Metadata = new OAuth2Metadata();

        /**
         * Reports whether enabled.
         *
         * @return is enabled result
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * Sets the configured enabled.
         *
         * @param enabled enabled value
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * Returns the configured OAuth2 metadata.
         *
         * @return get OAuth2 metadata result
         */
        public OAuth2Metadata getOauth2Metadata() {
            return oauth2Metadata;
        }
    }

    /** Public OAuth2 metadata exposure settings. */
    public static class OAuth2Metadata {
        /** Creates a OAuth2 metadata instance. */
        public OAuth2Metadata() {}

        private boolean enabled = true;
        private boolean includeErrorDescription = true;
        private boolean includeErrorUri = true;
        private boolean includeRequiredScope = false;

        /**
         * Reports whether enabled.
         *
         * @return is enabled result
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * Sets the configured enabled.
         *
         * @param enabled enabled value
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * Reports whether include error description.
         *
         * @return is include error description result
         */
        public boolean isIncludeErrorDescription() {
            return includeErrorDescription;
        }

        /**
         * Sets the configured include error description.
         *
         * @param includeErrorDescription include error description value
         */
        public void setIncludeErrorDescription(boolean includeErrorDescription) {
            this.includeErrorDescription = includeErrorDescription;
        }

        /**
         * Reports whether include error uri.
         *
         * @return is include error uri result
         */
        public boolean isIncludeErrorUri() {
            return includeErrorUri;
        }

        /**
         * Sets the configured include error uri.
         *
         * @param includeErrorUri include error uri value
         */
        public void setIncludeErrorUri(boolean includeErrorUri) {
            this.includeErrorUri = includeErrorUri;
        }

        /**
         * Reports whether include required scope.
         *
         * @return is include required scope result
         */
        public boolean isIncludeRequiredScope() {
            return includeRequiredScope;
        }

        /**
         * Sets the configured include required scope.
         *
         * @param includeRequiredScope include required scope value
         */
        public void setIncludeRequiredScope(boolean includeRequiredScope) {
            this.includeRequiredScope = includeRequiredScope;
        }
    }
}
