package com.smbtech.serviceframework.starter.logging.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import com.smbtech.serviceframework.starter.logging.adapter.out.logback.PolicyAwareAsyncAppender;
import com.smbtech.serviceframework.starter.logging.adapter.out.logback.ServiceFrameworkStructuredLogFormatter;
import org.junit.jupiter.api.Test;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;

class LoggingRuntimeHintsTest {

    @Test
    void registersFormatterAndLogbackConfiguration() throws Exception {
        RuntimeHints hints = new RuntimeHints();

        new LoggingRuntimeHints().registerHints(hints, getClass().getClassLoader());

        assertThat(
                        RuntimeHintsPredicates.reflection()
                                .onConstructorInvocation(
                                        PolicyAwareAsyncAppender.class.getConstructor()))
                .accepts(hints);
        assertThat(
                        RuntimeHintsPredicates.reflection()
                                .onMethodInvocation(
                                        PolicyAwareAsyncAppender.class.getMethod(
                                                "setSaturationPolicy", String.class)))
                .accepts(hints);
        assertThat(
                        RuntimeHintsPredicates.reflection()
                                .onConstructorInvocation(
                                        ServiceFrameworkStructuredLogFormatter.class
                                                .getConstructor()))
                .accepts(hints);
        assertThat(RuntimeHintsPredicates.resource().forResource("logback-spring.xml"))
                .accepts(hints);
        assertThat(
                        RuntimeHintsPredicates.resource()
                                .forResource(
                                        "com/smbtech/serviceframework/starter/logging/logback/"
                                                + "properties.xml"))
                .accepts(hints);
        assertThat(
                        RuntimeHintsPredicates.resource()
                                .forResource(
                                        "com/smbtech/serviceframework/starter/logging/logback/"
                                                + "structured-console-appender.xml"))
                .accepts(hints);
        assertThat(
                        RuntimeHintsPredicates.resource()
                                .forResource(
                                        "com/smbtech/serviceframework/starter/logging/logback/"
                                                + "async-appender.xml"))
                .accepts(hints);
        assertThat(
                        RuntimeHintsPredicates.resource()
                                .forResource(
                                        "com/smbtech/serviceframework/starter/logging/logback/"
                                                + "root.xml"))
                .accepts(hints);
    }
}
