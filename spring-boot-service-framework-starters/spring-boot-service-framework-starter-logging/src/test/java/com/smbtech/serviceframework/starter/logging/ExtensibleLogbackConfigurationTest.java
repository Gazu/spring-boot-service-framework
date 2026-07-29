package com.smbtech.serviceframework.starter.logging;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.smbtech.serviceframework.starter.logging.adapter.out.logback.PolicyAwareAsyncAppender;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;

class ExtensibleLogbackConfigurationTest {

    @Test
    void replacesTheDefaultDelegateUsingPackagedFragments() throws InterruptedException {
        SpringApplication application = new SpringApplication(TestApplication.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        application.setDefaultProperties(
                Map.of(
                        "logging.config",
                        "classpath:logback-extension-test.xml",
                        "spring.main.banner-mode",
                        "off",
                        "smbtech.logging.level",
                        "INFO"));

        try (ConfigurableApplicationContext ignored = application.run()) {
            Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
            PolicyAwareAsyncAppender async = (PolicyAwareAsyncAppender) root.getAppender("ASYNC");
            @SuppressWarnings("unchecked")
            ListAppender<ILoggingEvent> custom =
                    (ListAppender<ILoggingEvent>) async.getAppender("CUSTOM");

            Logger logger = (Logger) LoggerFactory.getLogger("extension-test");
            logger.info("custom delegate event");

            awaitEvent(custom, "custom delegate event");

            assertThat(root.getAppender("STDOUT")).isNull();
            assertThat(custom.list)
                    .extracting(ILoggingEvent::getFormattedMessage)
                    .contains("custom delegate event");
        }
    }

    private static void awaitEvent(ListAppender<ILoggingEvent> appender, String message)
            throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        while (appender.list.stream()
                        .noneMatch(event -> message.equals(event.getFormattedMessage()))
                && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApplication {}
}
