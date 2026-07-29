package com.smbtech.serviceframework.starter.mock.autoconfigure;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Provides mock properties behavior. */
@ConfigurationProperties(prefix = "smbtech.mocks")
public class MockProperties {
    /** Creates a mock properties instance. */
    public MockProperties() {}

    private Map<String, Endpoint> endpoints = new LinkedHashMap<>();
    private OpenApi openapi = new OpenApi();

    /**
     * Returns the configured endpoints.
     *
     * @return get endpoints result
     */
    public Map<String, Endpoint> getEndpoints() {
        return endpoints;
    }

    /**
     * Sets the configured endpoints.
     *
     * @param endpoints endpoints value
     */
    public void setEndpoints(Map<String, Endpoint> endpoints) {
        this.endpoints = endpoints;
    }

    /**
     * Returns the configured openapi.
     *
     * @return get openapi result
     */
    public OpenApi getOpenapi() {
        return openapi;
    }

    /**
     * Sets the configured openapi.
     *
     * @param openapi openapi value
     */
    public void setOpenapi(OpenApi openapi) {
        this.openapi = openapi;
    }

    /** Provides endpoint behavior. */
    public static class Endpoint {
        /** Creates an endpoint instance. */
        public Endpoint() {}

        private boolean enabled;
        private String file;
        private Duration delay = Duration.ZERO;

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
         * Returns the configured file.
         *
         * @return get file result
         */
        public String getFile() {
            return file;
        }

        /**
         * Sets the configured file.
         *
         * @param file file value
         */
        public void setFile(String file) {
            this.file = file;
        }

        /**
         * Returns the configured delay.
         *
         * @return get delay result
         */
        public Duration getDelay() {
            return delay;
        }

        /**
         * Sets the configured delay.
         *
         * @param delay delay value
         */
        public void setDelay(Duration delay) {
            this.delay = delay;
        }
    }

    /** Provides open api behavior. */
    public static class OpenApi {
        /** Creates an OpenAPI instance. */
        public OpenApi() {}

        private boolean enabled;
        private boolean failFast = true;
        private boolean includeOptionalProperties = true;
        private boolean allowInProduction;
        private boolean statusOverrideEnabled;
        private String statusHeader = "X-Mock-Status";
        private Set<String> productionProfiles = new LinkedHashSet<>(Set.of("prod", "production"));
        private Map<String, Contract> contracts = new LinkedHashMap<>();

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
         * Reports whether fail fast.
         *
         * @return is fail fast result
         */
        public boolean isFailFast() {
            return failFast;
        }

        /**
         * Sets the configured fail fast.
         *
         * @param failFast fail fast value
         */
        public void setFailFast(boolean failFast) {
            this.failFast = failFast;
        }

        /**
         * Reports whether include optional properties.
         *
         * @return is include optional properties result
         */
        public boolean isIncludeOptionalProperties() {
            return includeOptionalProperties;
        }

        /**
         * Sets the configured include optional properties.
         *
         * @param includeOptionalProperties include optional properties value
         */
        public void setIncludeOptionalProperties(boolean includeOptionalProperties) {
            this.includeOptionalProperties = includeOptionalProperties;
        }

        /**
         * Reports whether OpenAPI mock routes may run under a production profile.
         *
         * @return whether production activation is explicitly allowed
         */
        public boolean isAllowInProduction() {
            return allowInProduction;
        }

        /**
         * Sets whether OpenAPI mock routes may run under a production profile.
         *
         * @param allowInProduction whether production activation is explicitly allowed
         */
        public void setAllowInProduction(boolean allowInProduction) {
            this.allowInProduction = allowInProduction;
        }

        /**
         * Reports whether requests may select a declared response status through a header.
         *
         * @return whether status override is enabled
         */
        public boolean isStatusOverrideEnabled() {
            return statusOverrideEnabled;
        }

        /**
         * Sets whether requests may select a declared response status through a header.
         *
         * @param statusOverrideEnabled whether status override is enabled
         */
        public void setStatusOverrideEnabled(boolean statusOverrideEnabled) {
            this.statusOverrideEnabled = statusOverrideEnabled;
        }

        /**
         * Returns the configured status header.
         *
         * @return get status header result
         */
        public String getStatusHeader() {
            return statusHeader;
        }

        /**
         * Sets the configured status header.
         *
         * @param statusHeader status header value
         */
        public void setStatusHeader(String statusHeader) {
            this.statusHeader = statusHeader;
        }

        /**
         * Returns profiles treated as production environments.
         *
         * @return configured production profiles
         */
        public Set<String> getProductionProfiles() {
            return productionProfiles;
        }

        /**
         * Sets profiles treated as production environments.
         *
         * @param productionProfiles configured production profiles
         */
        public void setProductionProfiles(Set<String> productionProfiles) {
            this.productionProfiles =
                    productionProfiles == null
                            ? new LinkedHashSet<>()
                            : new LinkedHashSet<>(productionProfiles);
        }

        /**
         * Returns the configured contracts.
         *
         * @return get contracts result
         */
        public Map<String, Contract> getContracts() {
            return contracts;
        }

        /**
         * Sets the configured contracts.
         *
         * @param contracts contracts value
         */
        public void setContracts(Map<String, Contract> contracts) {
            this.contracts = contracts;
        }
    }

    /** Provides contract behavior. */
    public static class Contract {
        /** Creates a contract instance. */
        public Contract() {}

        private boolean enabled = true;
        private String location;
        private String basePath = "";
        private Duration delay = Duration.ZERO;

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
         * Returns the configured location.
         *
         * @return get location result
         */
        public String getLocation() {
            return location;
        }

        /**
         * Sets the configured location.
         *
         * @param location location value
         */
        public void setLocation(String location) {
            this.location = location;
        }

        /**
         * Returns the configured base path.
         *
         * @return get base path result
         */
        public String getBasePath() {
            return basePath;
        }

        /**
         * Sets the configured base path.
         *
         * @param basePath base path value
         */
        public void setBasePath(String basePath) {
            this.basePath = basePath;
        }

        /**
         * Returns the configured delay.
         *
         * @return get delay result
         */
        public Duration getDelay() {
            return delay;
        }

        /**
         * Sets the configured delay.
         *
         * @param delay delay value
         */
        public void setDelay(Duration delay) {
            this.delay = delay;
        }
    }
}
