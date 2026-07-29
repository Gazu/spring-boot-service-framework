package com.smbtech.serviceframework.starter.restclient.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import com.smbtech.serviceframework.httpclient.domain.AuthenticationType;
import com.smbtech.serviceframework.httpclient.domain.HttpClientDefinition;
import com.smbtech.serviceframework.starter.restclient.api.customizer.RestClientAuthenticationConfigurer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

class RestClientAuthenticationConfigurerIntegrationTest {

    @Test
    void delegatesCustomAuthenticationWithoutChangingTheRestClientFactory() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(RestClientAutoConfiguration.class))
                .withUserConfiguration(CustomAuthenticationConfiguration.class)
                .withPropertyValues(
                        "smbtech.rest-clients.clients.partner.base-url=https://partner.example",
                        "smbtech.rest-clients.clients.partner.authentication-type=OTHER")
                .run(
                        context -> {
                            assertThat(context).hasNotFailed();
                            assertThat(context).hasBean("partnerRestClient");
                            assertThat(context.getBean(TestAuthenticationConfigurer.class).applied)
                                    .isTrue();
                        });
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomAuthenticationConfiguration {

        @Bean
        TestAuthenticationConfigurer testAuthenticationConfigurer() {
            return new TestAuthenticationConfigurer();
        }
    }

    static final class TestAuthenticationConfigurer implements RestClientAuthenticationConfigurer {

        private boolean applied;

        @Override
        public boolean supports(AuthenticationType authenticationType) {
            return authenticationType == AuthenticationType.OTHER;
        }

        @Override
        public void configure(HttpClientDefinition definition, RestClient.Builder builder) {
            applied = true;
            builder.defaultHeader("X-Partner-Authentication", definition.name());
        }
    }
}
