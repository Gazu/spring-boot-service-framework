package com.smbtech.serviceframework.starter.logging.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Provides logging properties behavior. */
@ConfigurationProperties("smbtech.logging")
public class LoggingProperties {
    /** Creates a logging properties instance. */
    public LoggingProperties() {}

    private boolean production;
    private String level = "INFO";
    private final Async async = new Async();
    private final Transaction transaction = new Transaction();

    /**
     * Reports whether production.
     *
     * @return is production result
     */
    public boolean isProduction() {
        return production;
    }

    /**
     * Sets the configured production.
     *
     * @param production production value
     */
    public void setProduction(boolean production) {
        this.production = production;
    }

    /**
     * Returns the configured level.
     *
     * @return get level result
     */
    public String getLevel() {
        return level;
    }

    /**
     * Sets the configured level.
     *
     * @param level level value
     */
    public void setLevel(String level) {
        this.level = level;
    }

    /**
     * Returns the configured async.
     *
     * @return get async result
     */
    public Async getAsync() {
        return async;
    }

    /**
     * Returns the configured transaction.
     *
     * @return get transaction result
     */
    public Transaction getTransaction() {
        return transaction;
    }

    /** Provides async behavior. */
    public static class Async {
        /** Creates a async instance. */
        public Async() {}

        private boolean enabled = true;
        private int queueSize = 2048;
        private int discardingThreshold;
        private boolean neverBlock;
        private int maxFlushTimeMs = 1000;

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
         * Returns the configured queue size.
         *
         * @return get queue size result
         */
        public int getQueueSize() {
            return queueSize;
        }

        /**
         * Sets the configured queue size.
         *
         * @param queueSize queue size value
         */
        public void setQueueSize(int queueSize) {
            this.queueSize = queueSize;
        }

        /**
         * Returns the configured discarding threshold.
         *
         * @return get discarding threshold result
         */
        public int getDiscardingThreshold() {
            return discardingThreshold;
        }

        /**
         * Sets the configured discarding threshold.
         *
         * @param discardingThreshold discarding threshold value
         */
        public void setDiscardingThreshold(int discardingThreshold) {
            this.discardingThreshold = discardingThreshold;
        }

        /**
         * Reports whether never block.
         *
         * @return is never block result
         */
        public boolean isNeverBlock() {
            return neverBlock;
        }

        /**
         * Sets the configured never block.
         *
         * @param neverBlock never block value
         */
        public void setNeverBlock(boolean neverBlock) {
            this.neverBlock = neverBlock;
        }

        /**
         * Returns the configured max flush time ms.
         *
         * @return get max flush time ms result
         */
        public int getMaxFlushTimeMs() {
            return maxFlushTimeMs;
        }

        /**
         * Sets the configured max flush time ms.
         *
         * @param maxFlushTimeMs max flush time ms value
         */
        public void setMaxFlushTimeMs(int maxFlushTimeMs) {
            this.maxFlushTimeMs = maxFlushTimeMs;
        }
    }

    /** Provides transaction behavior. */
    public static class Transaction {
        /** Creates a transaction instance. */
        public Transaction() {}

        private boolean enabled = true;
        private String headerName = "X-Transaction-Id";
        private boolean acceptIncoming = true;
        private int maxLength = 128;

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
         * Returns the configured header name.
         *
         * @return get header name result
         */
        public String getHeaderName() {
            return headerName;
        }

        /**
         * Sets the configured header name.
         *
         * @param headerName header name value
         */
        public void setHeaderName(String headerName) {
            this.headerName = headerName;
        }

        /**
         * Reports whether accept incoming.
         *
         * @return is accept incoming result
         */
        public boolean isAcceptIncoming() {
            return acceptIncoming;
        }

        /**
         * Sets the configured accept incoming.
         *
         * @param acceptIncoming accept incoming value
         */
        public void setAcceptIncoming(boolean acceptIncoming) {
            this.acceptIncoming = acceptIncoming;
        }

        /**
         * Returns the configured max length.
         *
         * @return get max length result
         */
        public int getMaxLength() {
            return maxLength;
        }

        /**
         * Sets the configured max length.
         *
         * @param maxLength max length value
         */
        public void setMaxLength(int maxLength) {
            this.maxLength = maxLength;
        }
    }
}
