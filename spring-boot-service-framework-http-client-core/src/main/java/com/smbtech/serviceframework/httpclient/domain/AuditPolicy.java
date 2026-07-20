package com.smbtech.serviceframework.httpclient.domain;

/**
 * Carries immutable audit policy data.
 *
 * @param enabled enabled value
 * @param includeRequest include request value
 * @param includeResponse include response value
 * @param includeHeaders include headers value
 * @param includeBody include body value
 * @param maxBodySize max body size value
 */
public record AuditPolicy(
        boolean enabled,
        boolean includeRequest,
        boolean includeResponse,
        boolean includeHeaders,
        boolean includeBody,
        int maxBodySize) {
    /**
     * Performs the disabled operation.
     *
     * @return disabled result
     */
    public static AuditPolicy disabled() {
        return new AuditPolicy(false, false, false, false, false, 4096);
    }

    /** Creates and validates the record components. */
    public AuditPolicy {
        maxBodySize = maxBodySize > 0 ? maxBodySize : 4096;
    }
}
