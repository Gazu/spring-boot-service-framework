package com.smbtech.serviceframework.starter.actuator.autoconfigure;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Provides base Service Framework Actuator configuration properties. */
@ConfigurationProperties(prefix = "smbtech.actuator")
public class ActuatorProperties {

    private boolean enabled = true;
    private final Diagnostics diagnostics = new Diagnostics();
    private final Metrics metrics = new Metrics();

    /** Creates an Actuator properties instance. */
    public ActuatorProperties() {}

    /**
     * Reports whether Service Framework Actuator auto-configuration is enabled.
     *
     * @return whether auto-configuration is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Sets whether Service Framework Actuator auto-configuration is enabled.
     *
     * @param enabled whether auto-configuration is enabled
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Returns the diagnostics safety and performance settings.
     *
     * @return diagnostics settings
     */
    public Diagnostics getDiagnostics() {
        return diagnostics;
    }

    /**
     * Returns the metrics settings.
     *
     * @return metrics settings
     */
    public Metrics getMetrics() {
        return metrics;
    }

    /** Micrometer metrics settings. */
    public static class Metrics {

        private boolean enabled = true;
        private Duration cacheTtl = Duration.ofSeconds(10);

        /** Creates a metrics settings instance. */
        public Metrics() {}

        /**
         * Reports whether framework metrics are enabled.
         *
         * @return whether metrics are enabled
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * Sets whether framework metrics are enabled.
         *
         * @param enabled whether metrics are enabled
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * Returns the duration for which one diagnostics sample is reused.
         *
         * @return diagnostics sample cache duration
         */
        public Duration getCacheTtl() {
            return cacheTtl;
        }

        /**
         * Sets the duration for which one diagnostics sample is reused.
         *
         * @param cacheTtl diagnostics sample cache duration
         */
        public void setCacheTtl(Duration cacheTtl) {
            this.cacheTtl = cacheTtl;
        }
    }

    /** Diagnostics safety and performance settings. */
    public static class Diagnostics {

        private Duration cacheTtl = Duration.ofSeconds(5);
        private Duration operationTimeout = Duration.ofSeconds(2);
        private int maxComponents = 64;
        private int maxModules = 64;

        /** Creates a diagnostics settings instance. */
        public Diagnostics() {}

        /**
         * Returns the duration for which diagnostics results are reused.
         *
         * @return diagnostics cache duration
         */
        public Duration getCacheTtl() {
            return cacheTtl;
        }

        /**
         * Sets the duration for which diagnostics results are reused.
         *
         * @param cacheTtl diagnostics cache duration
         */
        public void setCacheTtl(Duration cacheTtl) {
            this.cacheTtl = cacheTtl;
        }

        /**
         * Returns the maximum duration of one diagnostics operation.
         *
         * @return diagnostics operation timeout
         */
        public Duration getOperationTimeout() {
            return operationTimeout;
        }

        /**
         * Sets the maximum duration of one diagnostics operation.
         *
         * @param operationTimeout diagnostics operation timeout
         */
        public void setOperationTimeout(Duration operationTimeout) {
            this.operationTimeout = operationTimeout;
        }

        /**
         * Returns the maximum number of retained component results.
         *
         * @return maximum component results
         */
        public int getMaxComponents() {
            return maxComponents;
        }

        /**
         * Sets the maximum number of retained component results.
         *
         * @param maxComponents maximum component results
         */
        public void setMaxComponents(int maxComponents) {
            this.maxComponents = maxComponents;
        }

        /**
         * Returns the maximum number of retained module results.
         *
         * @return maximum module results
         */
        public int getMaxModules() {
            return maxModules;
        }

        /**
         * Sets the maximum number of retained module results.
         *
         * @param maxModules maximum module results
         */
        public void setMaxModules(int maxModules) {
            this.maxModules = maxModules;
        }
    }
}
