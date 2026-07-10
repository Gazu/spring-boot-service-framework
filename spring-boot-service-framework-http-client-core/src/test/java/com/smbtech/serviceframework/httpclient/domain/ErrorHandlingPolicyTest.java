package com.smbtech.serviceframework.httpclient.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErrorHandlingPolicyTest {

    @Test
    void defaultsCaptureCompleteBodyHeadersAndNotificationMetadata() {
        ErrorHandlingPolicy policy = ErrorHandlingPolicy.defaults();

        assertTrue(policy.enabled());
        assertTrue(policy.includeBody());
        assertTrue(policy.includeHeaders());
        assertTrue(policy.includeNotificationMetadata());
        assertEquals(ErrorHandlingPolicy.DEFAULT_NOTIFICATION_CODE_PREFIX, policy.notificationCodePrefix());
    }

    @Test
    void keepsBackwardCompatibleConstructorDefaults() {
        ErrorHandlingPolicy policy = new ErrorHandlingPolicy(true, true, -1);

        assertTrue(policy.includeHeaders());
        assertTrue(policy.includeNotificationMetadata());
        assertEquals(4096, policy.maxBodySize());
        assertEquals(ErrorHandlingPolicy.DEFAULT_NOTIFICATION_CODE_PREFIX, policy.notificationCodePrefix());
    }

    @Test
    void normalizesCustomNotificationPrefix() {
        ErrorHandlingPolicy policy = new ErrorHandlingPolicy(
                true,
                true,
                4096,
                true,
                true,
                "E_PAYMENTS_HTTP_CLIENT"
        );

        assertEquals("E_PAYMENTS_HTTP_CLIENT_", policy.notificationCodePrefix());
    }
}
