package com.smbtech.serviceframework.starter.logging.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("smbtech.logging")
public class LoggingProperties {
    private boolean production;
    private String level = "INFO";
    private final Async async = new Async();
    private final Transaction transaction = new Transaction();

    public boolean isProduction() {
        return production;
    }

    public void setProduction(boolean production) {
        this.production = production;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public Async getAsync() {
        return async;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public static class Async {
        private boolean enabled = true;
        private int queueSize = 2048;
        private int discardingThreshold;
        private boolean neverBlock;
        private int maxFlushTimeMs = 1000;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getQueueSize() {
            return queueSize;
        }

        public void setQueueSize(int queueSize) {
            this.queueSize = queueSize;
        }

        public int getDiscardingThreshold() {
            return discardingThreshold;
        }

        public void setDiscardingThreshold(int discardingThreshold) {
            this.discardingThreshold = discardingThreshold;
        }

        public boolean isNeverBlock() {
            return neverBlock;
        }

        public void setNeverBlock(boolean neverBlock) {
            this.neverBlock = neverBlock;
        }

        public int getMaxFlushTimeMs() {
            return maxFlushTimeMs;
        }

        public void setMaxFlushTimeMs(int maxFlushTimeMs) {
            this.maxFlushTimeMs = maxFlushTimeMs;
        }
    }

    public static class Transaction {
        private boolean enabled = true;
        private String headerName = "X-Transaction-Id";
        private boolean acceptIncoming = true;
        private int maxLength = 128;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getHeaderName() {
            return headerName;
        }

        public void setHeaderName(String headerName) {
            this.headerName = headerName;
        }

        public boolean isAcceptIncoming() {
            return acceptIncoming;
        }

        public void setAcceptIncoming(boolean acceptIncoming) {
            this.acceptIncoming = acceptIncoming;
        }

        public int getMaxLength() {
            return maxLength;
        }

        public void setMaxLength(int maxLength) {
            this.maxLength = maxLength;
        }
    }
}
