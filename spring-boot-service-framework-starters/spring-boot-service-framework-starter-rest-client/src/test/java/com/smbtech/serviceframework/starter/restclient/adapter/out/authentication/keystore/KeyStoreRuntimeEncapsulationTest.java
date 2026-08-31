package com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.keystore;

import static org.assertj.core.api.Assertions.assertThat;

import com.smbtech.serviceframework.starter.restclient.autoconfigure.RestClientAutoConfiguration;
import java.security.KeyStore;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class KeyStoreRuntimeEncapsulationTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(RestClientAutoConfiguration.class));

    @Test
    void exposesOnlyHighLevelKeyStoreCapabilities() {
        contextRunner.run(
                context -> {
                    assertThat(context).doesNotHaveBean("restClientKeyStoreLoader");
                    assertThat(context).doesNotHaveBean(KeyStore.class);
                    assertThat(context).hasBean("restClientSslContextBuilder");
                    assertThat(context).hasBean("restClientSigningJwkResolver");
                    assertThat(context).hasBean("restClientKeyStoreContentValidator");
                    assertThat(context).hasBean("restClientMtlsKeyStoreContentValidator");
                });
    }
}
