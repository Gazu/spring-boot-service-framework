package com.smbtech.serviceframework.starter.errorhandling.internal;

import com.smbtech.serviceframework.error.NotificationSanitizer;
import com.smbtech.serviceframework.starter.errorhandling.api.NotificationHttpStatusResolver;
import com.smbtech.serviceframework.starter.errorhandling.api.NotificationResponseFactory;

/** Creates package-owned response defaults for tests outside the web adapter package. */
public final class ErrorHandlingWebTestFixtures {

    private ErrorHandlingWebTestFixtures() {}

    public static NotificationResponseFactory responseFactory() {
        return new DefaultNotificationResponseFactory();
    }

    public static NotificationResponseFactory responseFactory(
            NotificationHttpStatusResolver statusResolver,
            NotificationSanitizer notificationSanitizer,
            boolean includeFieldViolations) {
        return new DefaultNotificationResponseFactory(
                statusResolver, notificationSanitizer, includeFieldViolations);
    }
}
