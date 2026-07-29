package com.smbtech.serviceframework.starter.logging.autoconfigure;

import com.smbtech.serviceframework.starter.logging.adapter.out.logback.PolicyAwareAsyncAppender;
import com.smbtech.serviceframework.starter.logging.adapter.out.logback.ServiceFrameworkStructuredLogFormatter;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/** Native-image hints for logging components loaded from Logback configuration. */
final class LoggingRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        hints.reflection()
                .registerType(
                        PolicyAwareAsyncAppender.class,
                        MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
                        MemberCategory.INVOKE_PUBLIC_METHODS)
                .registerType(
                        ServiceFrameworkStructuredLogFormatter.class,
                        MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS);
        hints.resources().registerPattern("logback-spring.xml");
        hints.resources()
                .registerPattern("com/smbtech/serviceframework/starter/logging/logback/*.xml");
    }
}
