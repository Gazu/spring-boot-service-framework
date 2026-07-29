package com.smbtech.serviceframework.starter.restclient.authentication;

import static org.assertj.core.api.Assertions.assertThat;

import com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring.ClientAssertionJwkResolver;
import com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring.OAuth2RestClientConfigurationValidationRunner;
import com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring.OAuth2RestClientConfigurationValidator;
import com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring.SpringOAuth2TokenResponseClientFactory;
import com.smbtech.serviceframework.starter.restclient.autoconfigure.OAuth2RestClientAuthenticationAutoConfiguration;
import com.smbtech.serviceframework.starter.restclient.autoconfigure.OAuth2RestClientAutoConfiguration;
import com.smbtech.serviceframework.starter.restclient.autoconfigure.RestClientAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

class OAuth2ConfigurationValidationIntegrationTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withConfiguration(
                            AutoConfigurations.of(
                                    RestClientAutoConfiguration.class,
                                    OAuth2RestClientAutoConfiguration.class,
                                    OAuth2RestClientAuthenticationAutoConfiguration.class,
                                    OAuth2ClientAutoConfiguration.class));

    @Test
    void startsWhenClientCredentialsRegistrationMatchesRestClientConfiguration() {
        contextRunner
                .withPropertyValues(
                        "spring.security.oauth2.client.provider.my-provider.token-uri=https://auth.example/oauth2/token",
                        "spring.security.oauth2.client.registration.payments-token.provider=my-provider",
                        "spring.security.oauth2.client.registration.payments-token.client-id=payments-client",
                        "spring.security.oauth2.client.registration.payments-token.client-secret=payments-secret",
                        "spring.security.oauth2.client.registration.payments-token.client-authentication-method=client_secret_basic",
                        "spring.security.oauth2.client.registration.payments-token.authorization-grant-type=client_credentials",
                        "spring.security.oauth2.client.registration.payments-token.scope=payment.read",
                        "smbtech.rest-clients.clients.payments.base-url=https://payments.example",
                        "smbtech.rest-clients.clients.payments.authentication-type=CLIENT_CREDENTIALS",
                        "smbtech.rest-clients.clients.payments.token-request-id=payments-token",
                        "smbtech.rest-clients.clients.payments.scopes=payment.read")
                .run(
                        context -> {
                            assertThat(context).hasNotFailed();
                            assertThat(context).hasSingleBean(ClientRegistrationRepository.class);
                            assertThat(context)
                                    .hasSingleBean(OAuth2RestClientConfigurationValidator.class);
                            assertThat(context)
                                    .hasSingleBean(
                                            OAuth2RestClientConfigurationValidationRunner.class);

                            var registration =
                                    context.getBean(ClientRegistrationRepository.class)
                                            .findByRegistrationId("payments-token");
                            assertThat(registration).isNotNull();
                            assertThat(registration.getAuthorizationGrantType())
                                    .isEqualTo(AuthorizationGrantType.CLIENT_CREDENTIALS);
                            assertThat(registration.getClientAuthenticationMethod())
                                    .isEqualTo(ClientAuthenticationMethod.CLIENT_SECRET_BASIC);
                        });
    }

    @Test
    void failsStartupWhenRestClientReferencesMissingSpringOAuth2Registration() {
        contextRunner
                .withPropertyValues(
                        "spring.security.oauth2.client.provider.my-provider.token-uri=https://auth.example/oauth2/token",
                        "spring.security.oauth2.client.registration.other-token.provider=my-provider",
                        "spring.security.oauth2.client.registration.other-token.client-id=other-client",
                        "spring.security.oauth2.client.registration.other-token.client-secret=other-secret",
                        "spring.security.oauth2.client.registration.other-token.client-authentication-method=client_secret_basic",
                        "spring.security.oauth2.client.registration.other-token.authorization-grant-type=client_credentials",
                        "smbtech.rest-clients.clients.payments.base-url=https://payments.example",
                        "smbtech.rest-clients.clients.payments.authentication-type=CLIENT_CREDENTIALS",
                        "smbtech.rest-clients.clients.payments.token-request-id=payments-token")
                .run(
                        context -> {
                            assertThat(context).hasFailed();
                            assertThat(context.getStartupFailure())
                                    .hasMessageContaining(
                                            "Invalid SMBTech REST client OAuth2 configuration")
                                    .hasMessageContaining("clients.payments.token-request-id")
                                    .hasMessageContaining(
                                            "references missing OAuth2 registration payments-token")
                                    .hasMessageContaining(
                                            "Fix: Set smbtech.rest-clients.clients.payments."
                                                    + "token-request-id");
                        });
    }

    @Test
    void failsStartupWhenSpringOAuth2RegistrationGrantDoesNotMatchRestClientAuthenticationType() {
        contextRunner
                .withPropertyValues(
                        "spring.security.oauth2.client.provider.my-provider.token-uri=https://auth.example/oauth2/token",
                        "spring.security.oauth2.client.registration.payments-token.provider=my-provider",
                        "spring.security.oauth2.client.registration.payments-token.client-id=payments-client",
                        "spring.security.oauth2.client.registration.payments-token.client-authentication-method=none",
                        "spring.security.oauth2.client.registration.payments-token.authorization-grant-type="
                                + "urn:ietf:params:oauth:grant-type:jwt-bearer",
                        "spring.security.oauth2.client.registration.payments-token.scope=payment.read",
                        "smbtech.rest-clients.clients.payments.base-url=https://payments.example",
                        "smbtech.rest-clients.clients.payments.authentication-type=CLIENT_CREDENTIALS",
                        "smbtech.rest-clients.clients.payments.token-request-id=payments-token")
                .run(
                        context -> {
                            assertThat(context).hasFailed();
                            assertThat(context.getStartupFailure())
                                    .hasMessageContaining(
                                            "Invalid SMBTech REST client OAuth2 configuration")
                                    .hasMessageContaining(
                                            "authorization-grant-type "
                                                    + "urn:ietf:params:oauth:grant-type:jwt-bearer; expected client_credentials");
                        });
    }

    @Test
    void startsWhenJwtBearerAndPrivateKeyJwtConfigurationAreBothComplete() {
        contextRunner
                .withPropertyValues(
                        "spring.security.oauth2.client.provider.my-provider.token-uri=https://auth.example/oauth2/token",
                        "spring.security.oauth2.client.registration.payments-token.provider=my-provider",
                        "spring.security.oauth2.client.registration.payments-token.client-id=payments-client",
                        "spring.security.oauth2.client.registration.payments-token.client-authentication-method=private_key_jwt",
                        "spring.security.oauth2.client.registration.payments-token.authorization-grant-type="
                                + "urn:ietf:params:oauth:grant-type:jwt-bearer",
                        "spring.security.oauth2.client.registration.payments-token.scope=payment.read",
                        "smbtech.rest-clients.clients.payments.base-url=https://payments.example",
                        "smbtech.rest-clients.clients.payments.authentication-type=JWT_BEARER",
                        "smbtech.rest-clients.clients.payments.token-request-id=payments-token",
                        "smbtech.rest-clients.clients.payments.scopes=payment.read",
                        "smbtech.rest-clients.authentication.client-assertions.payments-token.key-store-id="
                                + "client-assertion-signing-key",
                        "smbtech.rest-clients.authentication.jwt-bearer.payments-token.key-store-id="
                                + "jwt-bearer-signing-key")
                .run(
                        context -> {
                            assertThat(context).hasNotFailed();
                            assertThat(context).hasSingleBean(ClientAssertionJwkResolver.class);
                            assertThat(context)
                                    .hasSingleBean(SpringOAuth2TokenResponseClientFactory.class);
                            assertThat(context).hasSingleBean(OAuth2AuthorizedClientManager.class);

                            var registration =
                                    context.getBean(ClientRegistrationRepository.class)
                                            .findByRegistrationId("payments-token");
                            assertThat(registration).isNotNull();
                            assertThat(registration.getAuthorizationGrantType().getValue())
                                    .isEqualTo("urn:ietf:params:oauth:grant-type:jwt-bearer");
                            assertThat(registration.getClientAuthenticationMethod())
                                    .isEqualTo(ClientAuthenticationMethod.PRIVATE_KEY_JWT);
                        });
    }

    @Test
    void failsStartupWhenKeyStoreContentValidationCannotLoadJwtBearerSigningKey() {
        contextRunner
                .withPropertyValues(
                        "spring.security.oauth2.client.provider.my-provider.token-uri=https://auth.example/oauth2/token",
                        "spring.security.oauth2.client.registration.payments-token.provider=my-provider",
                        "spring.security.oauth2.client.registration.payments-token.client-id=payments-client",
                        "spring.security.oauth2.client.registration.payments-token.client-authentication-method=none",
                        "spring.security.oauth2.client.registration.payments-token.authorization-grant-type="
                                + "urn:ietf:params:oauth:grant-type:jwt-bearer",
                        "spring.security.oauth2.client.registration.payments-token.scope=payment.read",
                        "smbtech.rest-clients.validation.validate-key-store-content=true",
                        "smbtech.rest-clients.clients.payments.base-url=https://payments.example",
                        "smbtech.rest-clients.clients.payments.authentication-type=JWT_BEARER",
                        "smbtech.rest-clients.clients.payments.token-request-id=payments-token",
                        "smbtech.rest-clients.authentication.jwt-bearer.payments-token.key-store-id="
                                + "jwt-bearer-signing-key",
                        "smbtech.rest-clients.authentication.key-stores.jwt-bearer-signing-key.base64=not-valid-base64",
                        "smbtech.rest-clients.authentication.key-stores.jwt-bearer-signing-key.type=PKCS12",
                        "smbtech.rest-clients.authentication.key-stores.jwt-bearer-signing-key.password-ref="
                                + "keystore-password",
                        "smbtech.rest-clients.authentication.key-stores.jwt-bearer-signing-key.key-alias=auth",
                        "smbtech.rest-clients.authentication.key-stores.jwt-bearer-signing-key.key-password-ref="
                                + "key-password",
                        "smbtech.rest-clients.authentication.credentials.keystore-password.value=changeit",
                        "smbtech.rest-clients.authentication.credentials.key-password.value=changeit")
                .run(
                        context -> {
                            assertThat(context).hasFailed();
                            assertThat(context.getStartupFailure())
                                    .hasMessageContaining(
                                            "Invalid SMBTech REST client OAuth2 configuration")
                                    .hasMessageContaining(
                                            "authentication.key-stores.jwt-bearer-signing-key")
                                    .hasMessageContaining(
                                            "cannot be used for JWT signing content validation")
                                    .hasMessageContaining(
                                            "Fix: Remove smbtech.rest-clients.authentication.key-stores."
                                                    + "jwt-bearer-signing-key");
                        });
    }
}
