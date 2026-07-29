package com.smbtech.examples.actuator;

import com.smbtech.serviceframework.actuator.domain.ComponentHealth;
import com.smbtech.serviceframework.actuator.domain.FrameworkModuleInfo;
import com.smbtech.serviceframework.actuator.port.out.DiagnosticProbe;
import com.smbtech.serviceframework.actuator.port.out.FrameworkModuleInfoProvider;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ActuatorConsumerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ActuatorConsumerApplication.class, args);
    }

    @Bean
    DiagnosticProbe exampleApplicationDiagnosticProbe() {
        return new DiagnosticProbe() {
            @Override
            public String componentName() {
                return "example-application";
            }

            @Override
            public ComponentHealth check() {
                return ComponentHealth.up(
                        componentName(), Map.of("ready", true, "clientSecret", "must-not-leak"));
            }
        };
    }

    @Bean
    FrameworkModuleInfoProvider exampleModuleInfoProvider() {
        return new FrameworkModuleInfoProvider() {
            @Override
            public String moduleName() {
                return "actuator-consumer";
            }

            @Override
            public FrameworkModuleInfo provide() {
                return new FrameworkModuleInfo(moduleName(), "example", Map.of("sample", true));
            }
        };
    }
}
