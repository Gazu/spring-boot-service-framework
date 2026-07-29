package com.smbtech.serviceframework.starter.logging;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.smbtech.serviceframework.starter.logging.adapter.out.logback.PolicyAwareAsyncAppender;
import com.smbtech.serviceframework.starter.logging.autoconfigure.LoggingProperties;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;

class StarterSmokeTest {

    @Test
    void startsWithThePackagedLogbackConfiguration() {
        SpringApplication application = new SpringApplication(TestApplication.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        application.setDefaultProperties(
                java.util.Map.of(
                        "spring.main.banner-mode", "off",
                        "smbtech.logging.async.enabled", "false"));

        try (ConfigurableApplicationContext context = application.run()) {
            assertThat(context.getBean(LoggingProperties.class).getAsync().isEnabled()).isFalse();
        }
    }

    @Test
    void appliesSaturationPolicyThroughThePackagedLogbackConfiguration() {
        SpringApplication application = new SpringApplication(TestApplication.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        application.setDefaultProperties(
                java.util.Map.of(
                        "spring.main.banner-mode",
                        "off",
                        "smbtech.logging.level",
                        "OFF",
                        "smbtech.logging.async.saturation-policy",
                        "DROP_WHEN_FULL"));

        PolicyAwareAsyncAppender async;
        try (ConfigurableApplicationContext context = application.run()) {
            Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

            assertThat(root.getAppender("ASYNC")).isInstanceOf(PolicyAwareAsyncAppender.class);
            async = (PolicyAwareAsyncAppender) root.getAppender("ASYNC");
            assertThat(async.isNeverBlock()).isTrue();
            assertThat(async.getDiscardingThreshold()).isZero();
            assertThat(async.isAcceptingEvents()).isTrue();
            assertThat(context.getBean(LoggingProperties.class).getAsync().getSaturationPolicy())
                    .isEqualTo(LoggingProperties.Async.SaturationPolicy.DROP_WHEN_FULL);
        }

        ((LoggerContext) LoggerFactory.getILoggerFactory()).stop();

        assertThat(async.isStarted()).isFalse();
        assertThat(async.isAcceptingEvents()).isFalse();
        assertThat(async.getShutdownCount()).isEqualTo(1);
        assertThat(async.getShutdownTimeoutCount()).isZero();
    }

    @Test
    void preservesLegacyNeverBlockThroughThePackagedLogbackConfiguration() {
        SpringApplication application = new SpringApplication(TestApplication.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        application.setDefaultProperties(
                java.util.Map.of(
                        "spring.main.banner-mode",
                        "off",
                        "smbtech.logging.level",
                        "OFF",
                        "smbtech.logging.async.never-block",
                        "true"));

        try (ConfigurableApplicationContext context = application.run()) {
            Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
            PolicyAwareAsyncAppender async = (PolicyAwareAsyncAppender) root.getAppender("ASYNC");
            LoggingProperties.Async properties =
                    context.getBean(LoggingProperties.class).getAsync();

            assertThat(async.isNeverBlock()).isTrue();
            assertThat(async.getDiscardingThreshold()).isZero();
            assertThat(properties.isNeverBlock()).isTrue();
            assertThat(properties.getSaturationPolicy())
                    .isEqualTo(LoggingProperties.Async.SaturationPolicy.BLOCK);
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApplication {}
}
