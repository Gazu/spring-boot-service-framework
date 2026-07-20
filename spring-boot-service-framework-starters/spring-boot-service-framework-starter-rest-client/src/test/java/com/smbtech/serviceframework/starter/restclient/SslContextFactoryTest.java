package com.smbtech.serviceframework.starter.restclient;

import static org.assertj.core.api.Assertions.assertThat;

import com.smbtech.serviceframework.httpclient.domain.HttpClientDefinition;
import com.smbtech.serviceframework.httpclient.port.in.HttpClientCatalog;
import com.smbtech.serviceframework.starter.restclient.adapter.out.apache.SslContextFactory;
import com.smbtech.serviceframework.starter.restclient.autoconfigure.RestClientAutoConfiguration;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import javax.net.ssl.SSLContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class SslContextFactoryTest {

    @TempDir Path tempDir;

    @Test
    void buildsClientSslContextFromConfiguredTrustStoreAndKeyStore() throws Exception {
        Path keyStore = createKeyStore("client");

        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(RestClientAutoConfiguration.class))
                .withPropertyValues(
                        "smbtech.rest-clients.clients.payments.base-url=https://payments.example",
                        "smbtech.rest-clients.clients.payments.client-type=APACHE_HTTP",
                        "smbtech.rest-clients.clients.payments.apache.ssl.enabled=true",
                        "smbtech.rest-clients.clients.payments.apache.ssl.trust-store-id=payments-trust",
                        "smbtech.rest-clients.clients.payments.apache.ssl.key-store-id=payments-client-cert",
                        "smbtech.rest-clients.authentication.credentials.store-password.value=changeit",
                        "smbtech.rest-clients.authentication.credentials.key-password.value=changeit",
                        "smbtech.rest-clients.authentication.key-stores.payments-trust.location=file:"
                                + keyStore,
                        "smbtech.rest-clients.authentication.key-stores.payments-trust.type=PKCS12",
                        "smbtech.rest-clients.authentication.key-stores.payments-trust.password-ref=store-password",
                        "smbtech.rest-clients.authentication.key-stores.payments-client-cert.location=file:"
                                + keyStore,
                        "smbtech.rest-clients.authentication.key-stores.payments-client-cert.type=PKCS12",
                        "smbtech.rest-clients.authentication.key-stores.payments-client-cert.password-ref=store-password",
                        "smbtech.rest-clients.authentication.key-stores.payments-client-cert.key-alias=client",
                        "smbtech.rest-clients.authentication.key-stores.payments-client-cert.key-password-ref=key-password")
                .run(
                        context -> {
                            HttpClientDefinition definition =
                                    context.getBean(HttpClientCatalog.class)
                                            .requireByName("payments");

                            SSLContext sslContext =
                                    context.getBean(SslContextFactory.class)
                                            .build(definition, null);

                            assertThat(definition.apache().ssl().enabled()).isTrue();
                            assertThat(definition.apache().ssl().trustStoreId())
                                    .isEqualTo("payments-trust");
                            assertThat(definition.apache().ssl().keyStoreId())
                                    .isEqualTo("payments-client-cert");
                            assertThat(sslContext).isNotNull();
                            assertThat(sslContext.getProtocol()).startsWith("TLS");
                        });
    }

    @Test
    void buildsClientSslContextFromBase64TrustStoreKeyStoreAndCredentialPasswords()
            throws Exception {
        String keyStoreBase64 =
                Base64.getEncoder()
                        .encodeToString(Files.readAllBytes(createKeyStore("inline-client")));
        String passwordBase64 =
                Base64.getEncoder().encodeToString("changeit".getBytes(StandardCharsets.UTF_8));

        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(RestClientAutoConfiguration.class))
                .withPropertyValues(
                        "smbtech.rest-clients.clients.payments.base-url=https://payments.example",
                        "smbtech.rest-clients.clients.payments.client-type=APACHE_HTTP",
                        "smbtech.rest-clients.clients.payments.apache.ssl.enabled=true",
                        "smbtech.rest-clients.clients.payments.apache.ssl.trust-store-id=payments-trust",
                        "smbtech.rest-clients.clients.payments.apache.ssl.key-store-id=payments-client-cert",
                        "smbtech.rest-clients.authentication.credentials.store-password.base64="
                                + passwordBase64,
                        "smbtech.rest-clients.authentication.credentials.key-password.base64="
                                + passwordBase64,
                        "smbtech.rest-clients.authentication.key-stores.payments-trust.base64="
                                + keyStoreBase64,
                        "smbtech.rest-clients.authentication.key-stores.payments-trust.type=PKCS12",
                        "smbtech.rest-clients.authentication.key-stores.payments-trust.password-ref=store-password",
                        "smbtech.rest-clients.authentication.key-stores.payments-client-cert.base64="
                                + keyStoreBase64,
                        "smbtech.rest-clients.authentication.key-stores.payments-client-cert.type=PKCS12",
                        "smbtech.rest-clients.authentication.key-stores.payments-client-cert.password-ref=store-password",
                        "smbtech.rest-clients.authentication.key-stores.payments-client-cert.key-alias=inline-client",
                        "smbtech.rest-clients.authentication.key-stores.payments-client-cert.key-password-ref=key-password")
                .run(
                        context -> {
                            HttpClientDefinition definition =
                                    context.getBean(HttpClientCatalog.class)
                                            .requireByName("payments");

                            SSLContext sslContext =
                                    context.getBean(SslContextFactory.class)
                                            .build(definition, null);

                            assertThat(sslContext).isNotNull();
                            assertThat(sslContext.getProtocol()).startsWith("TLS");
                        });
    }

    @Test
    void buildsClientSslContextFromJksWithDifferentStorePasswordAndKeyPassword() throws Exception {
        Path keyStore = createJksKeyStore("jks-client");

        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(RestClientAutoConfiguration.class))
                .withPropertyValues(
                        "smbtech.rest-clients.clients.payments.base-url=https://payments.example",
                        "smbtech.rest-clients.clients.payments.client-type=APACHE_HTTP",
                        "smbtech.rest-clients.clients.payments.apache.ssl.enabled=true",
                        "smbtech.rest-clients.clients.payments.apache.ssl.trust-store-id=payments-trust",
                        "smbtech.rest-clients.clients.payments.apache.ssl.key-store-id=payments-client-cert",
                        "smbtech.rest-clients.authentication.credentials.store-password.value=storepass",
                        "smbtech.rest-clients.authentication.credentials.key-password.value=keypass1",
                        "smbtech.rest-clients.authentication.key-stores.payments-trust.location=file:"
                                + keyStore,
                        "smbtech.rest-clients.authentication.key-stores.payments-trust.type=JKS",
                        "smbtech.rest-clients.authentication.key-stores.payments-trust.password-ref=store-password",
                        "smbtech.rest-clients.authentication.key-stores.payments-client-cert.location=file:"
                                + keyStore,
                        "smbtech.rest-clients.authentication.key-stores.payments-client-cert.type=JKS",
                        "smbtech.rest-clients.authentication.key-stores.payments-client-cert.password-ref=store-password",
                        "smbtech.rest-clients.authentication.key-stores.payments-client-cert.key-alias=jks-client",
                        "smbtech.rest-clients.authentication.key-stores.payments-client-cert.key-password-ref=key-password")
                .run(
                        context -> {
                            HttpClientDefinition definition =
                                    context.getBean(HttpClientCatalog.class)
                                            .requireByName("payments");

                            SSLContext sslContext =
                                    context.getBean(SslContextFactory.class)
                                            .build(definition, null);

                            assertThat(sslContext).isNotNull();
                            assertThat(sslContext.getProtocol()).startsWith("TLS");
                        });
    }

    @Test
    void trustStoreDoesNotRequireKeyAlias() throws Exception {
        Path trustStore = createKeyStore("trusted");

        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(RestClientAutoConfiguration.class))
                .withPropertyValues(
                        "smbtech.rest-clients.clients.audit.base-url=https://audit.example",
                        "smbtech.rest-clients.clients.audit.client-type=APACHE_HTTP",
                        "smbtech.rest-clients.clients.audit.apache.ssl.enabled=true",
                        "smbtech.rest-clients.clients.audit.apache.ssl.trust-store-id=audit-trust",
                        "smbtech.rest-clients.authentication.credentials.store-password.value=changeit",
                        "smbtech.rest-clients.authentication.key-stores.audit-trust.location=file:"
                                + trustStore,
                        "smbtech.rest-clients.authentication.key-stores.audit-trust.type=PKCS12",
                        "smbtech.rest-clients.authentication.key-stores.audit-trust.password-ref=store-password")
                .run(
                        context -> {
                            HttpClientDefinition definition =
                                    context.getBean(HttpClientCatalog.class).requireByName("audit");

                            SSLContext sslContext =
                                    context.getBean(SslContextFactory.class)
                                            .build(definition, null);

                            assertThat(definition.apache().ssl().trustStoreId())
                                    .isEqualTo("audit-trust");
                            assertThat(sslContext).isNotNull();
                            assertThat(sslContext.getProtocol()).startsWith("TLS");
                        });
    }

    private Path createKeyStore(String alias) throws Exception {
        Path keyStore = tempDir.resolve(alias + ".p12");
        Path keytool = Path.of(System.getProperty("java.home"), "bin", executable("keytool"));
        Process process =
                new ProcessBuilder(
                                keytool.toString(),
                                "-genkeypair",
                                "-alias",
                                alias,
                                "-keyalg",
                                "RSA",
                                "-keysize",
                                "2048",
                                "-storetype",
                                "PKCS12",
                                "-keystore",
                                keyStore.toString(),
                                "-storepass",
                                "changeit",
                                "-keypass",
                                "changeit",
                                "-dname",
                                "CN=" + alias,
                                "-validity",
                                "365",
                                "-noprompt")
                        .redirectErrorStream(true)
                        .start();

        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IllegalStateException("keytool failed: " + output);
        }
        return keyStore;
    }

    private Path createJksKeyStore(String alias) throws Exception {
        Path keyStore = tempDir.resolve(alias + ".jks");
        Path keytool = Path.of(System.getProperty("java.home"), "bin", executable("keytool"));
        Process process =
                new ProcessBuilder(
                                keytool.toString(),
                                "-genkeypair",
                                "-alias",
                                alias,
                                "-keyalg",
                                "RSA",
                                "-keysize",
                                "2048",
                                "-storetype",
                                "JKS",
                                "-keystore",
                                keyStore.toString(),
                                "-storepass",
                                "storepass",
                                "-keypass",
                                "keypass1",
                                "-dname",
                                "CN=" + alias,
                                "-validity",
                                "365",
                                "-noprompt")
                        .redirectErrorStream(true)
                        .start();

        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IllegalStateException("keytool failed: " + output);
        }
        return keyStore;
    }

    private String executable(String name) {
        return System.getProperty("os.name", "").toLowerCase().contains("win")
                ? name + ".exe"
                : name;
    }
}
