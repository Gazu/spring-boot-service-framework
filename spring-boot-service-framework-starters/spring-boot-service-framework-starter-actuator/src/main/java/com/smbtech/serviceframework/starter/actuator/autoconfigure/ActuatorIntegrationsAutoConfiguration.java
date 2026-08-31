package com.smbtech.serviceframework.starter.actuator.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Import;

/** Provides passive integrations with optional Service Framework starters. */
@AutoConfiguration(
        after = ActuatorAutoConfiguration.class,
        afterName = {
            "com.smbtech.serviceframework.starter.logging.autoconfigure.LoggingAutoConfiguration",
            "com.smbtech.serviceframework.starter.restclient.autoconfigure.RestClientAutoConfiguration",
            "com.smbtech.serviceframework.starter.mock.autoconfigure.MockAutoConfiguration",
            "com.smbtech.serviceframework.starter.errorhandling.autoconfigure.ErrorHandlingAutoConfiguration"
        })
@ConditionalOnProperty(
        prefix = "smbtech.actuator",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
@Import(ActuatorConfigurationImportSelector.class)
public class ActuatorIntegrationsAutoConfiguration {

    /** Creates an Actuator integrations auto-configuration instance. */
    public ActuatorIntegrationsAutoConfiguration() {}
}
