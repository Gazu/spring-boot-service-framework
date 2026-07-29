package com.smbtech.serviceframework.starter.restclient.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.smbtech.serviceframework.starter.restclient.api.AccessTokenClient;
import com.smbtech.serviceframework.starter.restclient.api.RestClientRegistry;
import com.smbtech.serviceframework.starter.restclient.api.customizer.RestClientAuthenticationConfigurer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class OAuth2ClasspathIsolationTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withClassLoader(new FilteredClassLoader("org.springframework.security"))
                    .withConfiguration(
                            AutoConfigurations.of(
                                    RestClientAutoConfiguration.class,
                                    OAuth2RestClientAutoConfiguration.class));

    @Test
    void startsWithoutSpringSecurityAndKeepsBaseRestClientInfrastructure() {
        contextRunner.run(
                context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(RestClientRegistry.class);
                    assertThat(context).doesNotHaveBean(AccessTokenClient.class);
                    assertThat(context).doesNotHaveBean(RestClientAuthenticationConfigurer.class);
                });
    }

    @Test
    void reportsMissingAuthenticationProviderWhenOAuth2IsConfiguredWithoutSpringSecurity() {
        contextRunner
                .withPropertyValues(
                        "smbtech.rest-clients.clients.payments.base-url=https://payments.example",
                        "smbtech.rest-clients.clients.payments.authentication-type=CLIENT_CREDENTIALS",
                        "smbtech.rest-clients.clients.payments.token-request-id=payments-token")
                .run(
                        context -> {
                            assertThat(context).hasNotFailed();
                            assertThatThrownBy(() -> context.getBean("paymentsRestClient"))
                                    .hasRootCauseMessage(
                                            "No authentication provider is configured for "
                                                    + "CLIENT_CREDENTIALS HTTP client: payments");
                        });
    }
}
