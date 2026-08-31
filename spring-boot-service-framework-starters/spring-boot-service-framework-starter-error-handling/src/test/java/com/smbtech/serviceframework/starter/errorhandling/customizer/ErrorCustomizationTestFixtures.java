package com.smbtech.serviceframework.starter.errorhandling.customizer;

import com.smbtech.serviceframework.logging.port.out.CorrelationContext;
import com.smbtech.serviceframework.starter.errorhandling.api.ResolvedErrorCustomizer;

/** Creates package-owned customization defaults for tests outside this package. */
public final class ErrorCustomizationTestFixtures {

    private ErrorCustomizationTestFixtures() {}

    public static ResolvedErrorCustomizer standardMetadataCustomizer() {
        return new StandardErrorMetadataCustomizer();
    }

    public static ResolvedErrorCustomizer standardMetadataCustomizer(
            CorrelationContext correlationContext) {
        return new StandardErrorMetadataCustomizer(correlationContext);
    }
}
