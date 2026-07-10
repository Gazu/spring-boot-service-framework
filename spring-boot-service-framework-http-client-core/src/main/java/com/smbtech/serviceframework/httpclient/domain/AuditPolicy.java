package com.smbtech.serviceframework.httpclient.domain;

public record AuditPolicy(
        boolean enabled,
        boolean includeRequest,
        boolean includeResponse,
        boolean includeHeaders,
        boolean includeBody,
        int maxBodySize
) {
    public static AuditPolicy disabled() {
        return new AuditPolicy(false, false, false, false, false, 4096);
    }

    public AuditPolicy {
        maxBodySize = maxBodySize > 0 ? maxBodySize : 4096;
    }
}
