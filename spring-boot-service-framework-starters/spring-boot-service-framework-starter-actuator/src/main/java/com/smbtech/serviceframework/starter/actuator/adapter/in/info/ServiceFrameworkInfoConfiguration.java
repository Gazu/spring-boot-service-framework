package com.smbtech.serviceframework.starter.actuator.adapter.in.info;

import com.smbtech.serviceframework.actuator.port.in.FrameworkDiagnostics;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class ServiceFrameworkInfoConfiguration {

    @Bean(name = "serviceFrameworkInfoContributor")
    @ConditionalOnMissingBean(name = "serviceFrameworkInfoContributor")
    InfoContributor serviceFrameworkInfoContributor(FrameworkDiagnostics diagnostics) {
        return new ServiceFrameworkInfoContributor(diagnostics);
    }
}
