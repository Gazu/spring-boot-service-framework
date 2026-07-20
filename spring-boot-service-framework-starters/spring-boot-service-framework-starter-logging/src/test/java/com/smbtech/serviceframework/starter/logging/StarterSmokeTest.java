package com.smbtech.serviceframework.starter.logging;

import static org.assertj.core.api.Assertions.assertThat;

import com.smbtech.serviceframework.starter.logging.autoconfigure.LoggingProperties;
import org.junit.jupiter.api.Test;
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

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApplication {}
}
