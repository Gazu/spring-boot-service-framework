package com.smbtech.serviceframework.starter.mock.autoconfigure;

import com.smbtech.serviceframework.mock.port.in.MockCatalog;
import com.smbtech.serviceframework.mock.port.in.MockResponder;
import com.smbtech.serviceframework.mock.port.out.MockDefinitionSource;
import com.smbtech.serviceframework.mock.port.out.MockResponseSource;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportRuntimeHints;
import tools.jackson.databind.ObjectMapper;

/** Provides mock auto configuration behavior. */
@AutoConfiguration
@ConditionalOnClass(ObjectMapper.class)
@EnableConfigurationProperties(MockProperties.class)
@ImportRuntimeHints(MockRuntimeHints.class)
@Import(MockConfigurationImportSelector.class)
public class MockAutoConfiguration {
    /** Creates a mock auto configuration instance. */
    public MockAutoConfiguration() {}

    @Bean
    @ConditionalOnMissingBean
    MockCatalog mockCatalog(MockDefinitionSource source) {
        return MockCatalog.from(source);
    }

    @Bean
    @ConditionalOnMissingBean
    MockResponder mockResponder(MockCatalog catalog, MockResponseSource responseSource) {
        return MockResponder.from(catalog, responseSource);
    }
}
