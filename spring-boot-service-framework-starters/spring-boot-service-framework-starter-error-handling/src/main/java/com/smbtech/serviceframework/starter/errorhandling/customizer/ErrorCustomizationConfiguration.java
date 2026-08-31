package com.smbtech.serviceframework.starter.errorhandling.customizer;

import com.smbtech.serviceframework.logging.port.out.CorrelationContext;
import com.smbtech.serviceframework.starter.errorhandling.api.ResolvedErrorCustomizer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Registers the internal customization pipeline and standard metadata contributor. */
@Configuration(proxyBeanMethods = false)
class ErrorCustomizationConfiguration {

    @Bean("standardErrorMetadataCustomizer")
    @ConditionalOnMissingBean(name = "standardErrorMetadataCustomizer")
    ResolvedErrorCustomizer standardErrorMetadataCustomizer(
            ObjectProvider<CorrelationContext> correlationContext) {
        return correlationContext
                .orderedStream()
                .findFirst()
                .map(StandardErrorMetadataCustomizer::new)
                .orElseGet(StandardErrorMetadataCustomizer::new);
    }
}
