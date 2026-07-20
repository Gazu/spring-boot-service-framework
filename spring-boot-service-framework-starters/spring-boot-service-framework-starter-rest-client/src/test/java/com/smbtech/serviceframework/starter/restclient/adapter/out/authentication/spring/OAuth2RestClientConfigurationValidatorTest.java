package com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring;

import static org.assertj.core.api.Assertions.assertThat;

import com.smbtech.serviceframework.httpclient.domain.AuthenticationType;
import com.smbtech.serviceframework.httpclient.port.out.CredentialDefinitionSource;
import com.smbtech.serviceframework.httpclient.port.out.CredentialProvider;
import com.smbtech.serviceframework.httpclient.port.out.KeyStoreDefinitionSource;
import com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.PropertiesCredentialDefinitionSource;
import com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.PropertiesCredentialProvider;
import com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.PropertiesKeyStoreDefinitionSource;
import com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.keystore.KeyStoreManager;
import com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.keystore.PrivateKeyLoader;
import com.smbtech.serviceframework.starter.restclient.autoconfigure.CredentialPropertiesMapper;
import com.smbtech.serviceframework.starter.restclient.autoconfigure.CredentialResolver;
import com.smbtech.serviceframework.starter.restclient.autoconfigure.KeyStorePropertiesMapper;
import com.smbtech.serviceframework.starter.restclient.autoconfigure.RestClientProperties;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

class OAuth2RestClientConfigurationValidatorTest {

    private final OAuth2RestClientConfigurationValidator validator =
            new OAuth2RestClientConfigurationValidator();

    @TempDir Path tempDir;

    @Test
    void returnsEmptyResultWhenValidationIsDisabled() {
        RestClientProperties properties = new RestClientProperties();
        properties.getValidation().setEnabled(false);
        properties.setClients(
                Map.of("payments", OAuth2Client(AuthenticationType.CLIENT_CREDENTIALS, "")));

        OAuth2ConfigurationValidationResult result = validator.validate(properties, null);

        assertThat(result.issues()).isEmpty();
    }

    @Test
    void ignoresNonOAuth2Clients() {
        RestClientProperties properties = new RestClientProperties();
        properties.setClients(
                Map.of(
                        "public-api", OAuth2Client(AuthenticationType.NO_AUTH, ""),
                        "secure-api", OAuth2Client(AuthenticationType.BASIC_AUTH, "")));

        OAuth2ConfigurationValidationResult result = validator.validate(properties, null);

        assertThat(result.issues()).isEmpty();
    }

    @Test
    void reportsMissingBasicAuthenticationCredentialReferences() {
        RestClientProperties.Client client = OAuth2Client(AuthenticationType.BASIC_AUTH, "");
        RestClientProperties.BasicAuthentication basicAuthentication =
                new RestClientProperties.BasicAuthentication();
        basicAuthentication.setUsernameRef("secure-username");
        basicAuthentication.setPasswordRef("secure-password");
        client.setBasicAuthentication(basicAuthentication);

        RestClientProperties properties = new RestClientProperties();
        properties.setClients(Map.of("secure-api", client));

        OAuth2ConfigurationValidationResult result = validator.validate(properties, null);

        assertThat(result.errors())
                .containsExactly(
                        OAuth2ConfigurationValidationIssue.error(
                                "clients.secure-api.basic-authentication.username-ref",
                                "references missing credential secure-username"),
                        OAuth2ConfigurationValidationIssue.error(
                                "clients.secure-api.basic-authentication.password-ref",
                                "references missing credential secure-password"));
    }

    @Test
    void reportsMissingKeyStoreCredentialReferences() {
        RestClientProperties.KeyStore keyStore = new RestClientProperties.KeyStore();
        keyStore.setPasswordRef("keystore-password");
        keyStore.setKeyPasswordRef("key-password");

        RestClientProperties properties = new RestClientProperties();
        properties.getAuthentication().setKeyStores(Map.of("payments-signing-key", keyStore));

        OAuth2ConfigurationValidationResult result = validator.validate(properties, null);

        assertThat(result.errors())
                .containsExactly(
                        OAuth2ConfigurationValidationIssue.error(
                                "authentication.key-stores.payments-signing-key.password-ref",
                                "references missing credential keystore-password"),
                        OAuth2ConfigurationValidationIssue.error(
                                "authentication.key-stores.payments-signing-key.key-password-ref",
                                "references missing credential key-password"));
    }

    @Test
    void acceptsExistingCredentialReferencesAndIgnoresBlankReferences() {
        RestClientProperties.Client client = OAuth2Client(AuthenticationType.BASIC_AUTH, "");
        RestClientProperties.BasicAuthentication basicAuthentication =
                new RestClientProperties.BasicAuthentication();
        basicAuthentication.setUsernameRef("secure-username");
        basicAuthentication.setPasswordRef(" ");
        client.setBasicAuthentication(basicAuthentication);
        client.getApache().getSsl().setEnabled(true);
        client.getApache().getSsl().setKeyStoreId("payments-signing-key");

        RestClientProperties.KeyStore keyStore = new RestClientProperties.KeyStore();
        keyStore.setPasswordRef("keystore-password");
        keyStore.setKeyPasswordRef("");

        RestClientProperties properties = new RestClientProperties();
        properties.setClients(Map.of("secure-api", client));
        properties
                .getAuthentication()
                .setCredentials(
                        Map.of(
                                "secure-username",
                                credential("demo"),
                                "keystore-password",
                                credential("changeit")));
        properties.getAuthentication().setKeyStores(Map.of("payments-signing-key", keyStore));

        OAuth2ConfigurationValidationResult result = validator.validate(properties, null);

        assertThat(result.issues()).isEmpty();
    }

    @Test
    void warnsAboutUnusedCredentialsAndKeyStores() {
        RestClientProperties.KeyStore keyStore = new RestClientProperties.KeyStore();
        keyStore.setPasswordRef("unused-keystore-password");
        RestClientProperties properties = new RestClientProperties();
        Map<String, RestClientProperties.Credential> credentials = new LinkedHashMap<>();
        credentials.put("unused-secret", credential("secret"));
        credentials.put("unused-keystore-password", credential("changeit"));
        properties.getAuthentication().setCredentials(credentials);
        properties.getAuthentication().setKeyStores(Map.of("unused-key-store", keyStore));

        OAuth2ConfigurationValidationResult result = validator.validate(properties, null);

        assertThat(result.warnings())
                .containsExactly(
                        OAuth2ConfigurationValidationIssue.warning(
                                "authentication.credentials.unused-secret",
                                "is configured but no enabled REST client or used key store references this credential id"),
                        OAuth2ConfigurationValidationIssue.warning(
                                "authentication.credentials.unused-keystore-password",
                                "is configured but no enabled REST client or used key store references this credential id"),
                        OAuth2ConfigurationValidationIssue.warning(
                                "authentication.key-stores.unused-key-store",
                                "is configured but no enabled REST client or OAuth2 signing configuration references this key store id"));
    }

    @Test
    void doesNotWarnAboutCredentialsAndKeyStoresUsedByOAuthSigningConfiguration() {
        RestClientProperties.Client client =
                OAuth2Client(AuthenticationType.JWT_BEARER, "payments-token");
        RestClientProperties.KeyStore keyStore = new RestClientProperties.KeyStore();
        keyStore.setPasswordRef("keystore-password");
        keyStore.setKeyPasswordRef("key-password");

        RestClientProperties properties = new RestClientProperties();
        properties.setClients(Map.of("payments", client));
        properties
                .getAuthentication()
                .setCredentials(
                        Map.of(
                                "keystore-password",
                                credential("changeit"),
                                "key-password",
                                credential("changeit")));
        properties.getAuthentication().setKeyStores(Map.of("payments-signing-key", keyStore));
        properties
                .getAuthentication()
                .setJwtBearer(Map.of("payments-token", jwtBearer("payments-signing-key")));

        OAuth2ConfigurationValidationResult result =
                validator.validate(
                        properties,
                        new InMemoryClientRegistrationRepository(
                                jwtBearerRegistration("payments-token")));

        assertThat(result.issues()).isEmpty();
    }

    @Test
    void doesNotOpenKeyStoreContentWhenContentValidationIsDisabled() {
        RestClientProperties properties = jwtBearerSigningProperties("not-valid-base64");
        properties.getValidation().setValidateKeyStoreContent(false);

        OAuth2ConfigurationValidationResult result =
                validator.validate(
                        properties,
                        new InMemoryClientRegistrationRepository(
                                jwtBearerRegistration("payments-token")));

        assertThat(result.issues()).isEmpty();
    }

    @Test
    void reportsInvalidSigningKeyStoreContentWhenContentValidationIsEnabled() {
        RestClientProperties properties = jwtBearerSigningProperties("not-valid-base64");
        properties.getValidation().setValidateKeyStoreContent(true);

        OAuth2ConfigurationValidationResult result =
                contentValidator(properties)
                        .validate(
                                properties,
                                new InMemoryClientRegistrationRepository(
                                        jwtBearerRegistration("payments-token")));

        assertThat(result.errors())
                .anySatisfy(
                        issue -> {
                            assertThat(issue.path())
                                    .isEqualTo("authentication.key-stores.payments-signing-key");
                            assertThat(issue.message())
                                    .contains("cannot be used for JWT signing content validation");
                        });
    }

    @Test
    void acceptsValidSigningKeyStoreContentWhenContentValidationIsEnabled() throws Exception {
        Path keyStorePath = createKeyStore("payments-signing-key.p12", "auth");
        RestClientProperties properties = jwtBearerSigningProperties("");
        properties.getValidation().setValidateKeyStoreContent(true);
        RestClientProperties.KeyStore keyStore =
                properties.getAuthentication().getKeyStores().get("payments-signing-key");
        keyStore.setBase64("");
        keyStore.setLocation("file:" + keyStorePath);

        OAuth2ConfigurationValidationResult result =
                contentValidator(properties)
                        .validate(
                                properties,
                                new InMemoryClientRegistrationRepository(
                                        jwtBearerRegistration("payments-token")));

        assertThat(result.issues()).isEmpty();
    }

    @Test
    void reportsInvalidMtlsKeyStoreAliasWhenContentValidationIsEnabled() throws Exception {
        Path keyStorePath = createKeyStore("mtls.p12", "auth");
        RestClientProperties.Client client = OAuth2Client(AuthenticationType.NO_AUTH, "");
        client.getApache().getSsl().setEnabled(true);
        client.getApache().getSsl().setKeyStoreId("mtls-key");

        RestClientProperties.KeyStore keyStore = new RestClientProperties.KeyStore();
        keyStore.setLocation("file:" + keyStorePath);
        keyStore.setType("PKCS12");
        keyStore.setPassword("changeit");
        keyStore.setKeyPassword("changeit");
        keyStore.setKeyAlias("missing");

        RestClientProperties properties = new RestClientProperties();
        properties.getValidation().setValidateKeyStoreContent(true);
        properties.setClients(Map.of("payments", client));
        properties.getAuthentication().setKeyStores(Map.of("mtls-key", keyStore));

        OAuth2ConfigurationValidationResult result =
                contentValidator(properties).validate(properties, null);

        assertThat(result.errors())
                .containsExactly(
                        OAuth2ConfigurationValidationIssue.error(
                                "authentication.key-stores.mtls-key.key-alias",
                                "references missing alias missing"));
    }

    @Test
    void ignoresDisabledOAuth2Clients() {
        RestClientProperties.Client client =
                OAuth2Client(AuthenticationType.CLIENT_CREDENTIALS, "payments-token");
        client.setEnabled(false);
        RestClientProperties properties = new RestClientProperties();
        properties.setClients(Map.of("payments", client));
        properties
                .getAuthentication()
                .setClientAssertions(
                        Map.of("payments-token", new RestClientProperties.ClientAssertion()));

        OAuth2ConfigurationValidationResult result = validator.validate(properties, null);

        assertThat(result.issues())
                .containsExactly(
                        OAuth2ConfigurationValidationIssue.warning(
                                "authentication.client-assertions.payments-token",
                                "is configured but no enabled OAuth2 REST client uses private_key_jwt with this registration id"));
    }

    @Test
    void reportsMissingTokenRequestIdForClientCredentialsClient() {
        RestClientProperties properties = new RestClientProperties();
        properties.setClients(
                Map.of("payments", OAuth2Client(AuthenticationType.CLIENT_CREDENTIALS, " ")));

        OAuth2ConfigurationValidationResult result = validator.validate(properties, null);

        assertThat(result.errors())
                .containsExactly(
                        OAuth2ConfigurationValidationIssue.error(
                                "clients.payments.token-request-id",
                                "is required for CLIENT_CREDENTIALS"));
    }

    @Test
    void reportsMissingTokenRequestIdForJwtBearerClient() {
        RestClientProperties properties = new RestClientProperties();
        properties.setClients(
                Map.of("payments", OAuth2Client(AuthenticationType.JWT_BEARER, null)));

        OAuth2ConfigurationValidationResult result = validator.validate(properties, null);

        assertThat(result.errors())
                .containsExactly(
                        OAuth2ConfigurationValidationIssue.error(
                                "clients.payments.token-request-id", "is required for JWT_BEARER"));
    }

    @Test
    void reportsMissingClientRegistrationRepository() {
        RestClientProperties properties = new RestClientProperties();
        properties.setClients(
                Map.of(
                        "payments",
                        OAuth2Client(AuthenticationType.CLIENT_CREDENTIALS, "payments-token")));

        OAuth2ConfigurationValidationResult result = validator.validate(properties, null);

        assertThat(result.errors())
                .containsExactly(
                        OAuth2ConfigurationValidationIssue.error(
                                "clients.payments.token-request-id",
                                "references payments-token but no ClientRegistrationRepository is available"));
    }

    @Test
    void reportsMissingOAuth2Registration() {
        RestClientProperties properties = new RestClientProperties();
        properties.setClients(
                Map.of(
                        "payments",
                        OAuth2Client(AuthenticationType.CLIENT_CREDENTIALS, "payments-token")));

        OAuth2ConfigurationValidationResult result =
                validator.validate(
                        properties,
                        new InMemoryClientRegistrationRepository(registration("other-token")));

        assertThat(result.errors())
                .containsExactly(
                        OAuth2ConfigurationValidationIssue.error(
                                "clients.payments.token-request-id",
                                "references missing OAuth2 registration payments-token"));
    }

    @Test
    void validReferencedOAuth2RegistrationProducesNoIssues() {
        RestClientProperties properties = new RestClientProperties();
        properties.setClients(
                Map.of(
                        "payments",
                        OAuth2Client(AuthenticationType.CLIENT_CREDENTIALS, "payments-token")));

        OAuth2ConfigurationValidationResult result =
                validator.validate(
                        properties,
                        new InMemoryClientRegistrationRepository(registration("payments-token")));

        assertThat(result.issues()).isEmpty();
    }

    @Test
    void reportsClientCredentialsGrantMismatch() {
        RestClientProperties properties = new RestClientProperties();
        properties.setClients(
                Map.of(
                        "payments",
                        OAuth2Client(AuthenticationType.CLIENT_CREDENTIALS, "payments-token")));

        OAuth2ConfigurationValidationResult result =
                validator.validate(
                        properties,
                        new InMemoryClientRegistrationRepository(
                                jwtBearerRegistration("payments-token")));

        assertThat(result.errors())
                .containsExactly(
                        OAuth2ConfigurationValidationIssue.error(
                                "clients.payments.token-request-id",
                                "references OAuth2 registration payments-token with authorization-grant-type "
                                        + "urn:ietf:params:oauth:grant-type:jwt-bearer; expected client_credentials"));
    }

    @Test
    void reportsUnsupportedClientCredentialsAuthenticationMethod() {
        RestClientProperties properties = new RestClientProperties();
        properties.setClients(
                Map.of(
                        "payments",
                        OAuth2Client(AuthenticationType.CLIENT_CREDENTIALS, "payments-token")));

        OAuth2ConfigurationValidationResult result =
                validator.validate(
                        properties,
                        new InMemoryClientRegistrationRepository(
                                registration(
                                        "payments-token", ClientAuthenticationMethod.NONE, null)));

        assertThat(result.errors())
                .containsExactly(
                        OAuth2ConfigurationValidationIssue.error(
                                "spring.security.oauth2.client.registration.payments-token.client-authentication-method",
                                "uses unsupported client authentication method none for CLIENT_CREDENTIALS; "
                                        + "expected client_secret_basic, client_secret_post, or private_key_jwt"));
    }

    @Test
    void reportsMissingSecretForClientSecretAuthentication() {
        RestClientProperties properties = new RestClientProperties();
        properties.setClients(
                Map.of(
                        "payments",
                        OAuth2Client(AuthenticationType.CLIENT_CREDENTIALS, "payments-token")));

        OAuth2ConfigurationValidationResult result =
                validator.validate(
                        properties,
                        new InMemoryClientRegistrationRepository(
                                registration(
                                        "payments-token",
                                        ClientAuthenticationMethod.CLIENT_SECRET_BASIC,
                                        " ")));

        assertThat(result.errors())
                .containsExactly(
                        OAuth2ConfigurationValidationIssue.error(
                                "spring.security.oauth2.client.registration.payments-token.client-secret",
                                "is required when client-authentication-method is client_secret_basic"));
    }

    @Test
    void reportsMissingPrivateKeyJwtClientAssertionConfiguration() {
        RestClientProperties properties = new RestClientProperties();
        properties.setClients(
                Map.of(
                        "payments",
                        OAuth2Client(AuthenticationType.CLIENT_CREDENTIALS, "payments-token")));

        OAuth2ConfigurationValidationResult result =
                validator.validate(
                        properties,
                        new InMemoryClientRegistrationRepository(
                                privateKeyJwtRegistration("payments-token")));

        assertThat(result.errors())
                .containsExactly(
                        OAuth2ConfigurationValidationIssue.error(
                                "authentication.client-assertions.payments-token",
                                "is required when OAuth2 registration payments-token uses private_key_jwt"));
    }

    @Test
    void reportsMissingPrivateKeyJwtClientAssertionKeyStoreId() {
        RestClientProperties properties = new RestClientProperties();
        properties.setClients(
                Map.of(
                        "payments",
                        OAuth2Client(AuthenticationType.CLIENT_CREDENTIALS, "payments-token")));
        RestClientProperties.ClientAssertion assertion = new RestClientProperties.ClientAssertion();
        assertion.setKeyStoreId(" ");
        properties.getAuthentication().setClientAssertions(Map.of("payments-token", assertion));

        OAuth2ConfigurationValidationResult result =
                validator.validate(
                        properties,
                        new InMemoryClientRegistrationRepository(
                                privateKeyJwtRegistration("payments-token")));

        assertThat(result.errors())
                .containsExactly(
                        OAuth2ConfigurationValidationIssue.error(
                                "authentication.client-assertions.payments-token.key-store-id",
                                "is required when OAuth2 registration payments-token uses private_key_jwt"));
    }

    @Test
    void acceptsPrivateKeyJwtClientCredentialsWhenClientAssertionHasKeyStoreId() {
        RestClientProperties properties = new RestClientProperties();
        properties.setClients(
                Map.of(
                        "payments",
                        OAuth2Client(AuthenticationType.CLIENT_CREDENTIALS, "payments-token")));
        RestClientProperties.ClientAssertion assertion = new RestClientProperties.ClientAssertion();
        assertion.setKeyStoreId("payments-signing-key");
        properties.getAuthentication().setClientAssertions(Map.of("payments-token", assertion));

        OAuth2ConfigurationValidationResult result =
                validator.validate(
                        properties,
                        new InMemoryClientRegistrationRepository(
                                privateKeyJwtRegistration("payments-token")));

        assertThat(result.issues()).isEmpty();
    }

    @Test
    void warnsWhenClientAssertionIsConfiguredForClientSecretAuthentication() {
        RestClientProperties properties = new RestClientProperties();
        properties.setClients(
                Map.of(
                        "payments",
                        OAuth2Client(AuthenticationType.CLIENT_CREDENTIALS, "payments-token")));
        RestClientProperties.ClientAssertion assertion = new RestClientProperties.ClientAssertion();
        assertion.setKeyStoreId("payments-signing-key");
        properties.getAuthentication().setClientAssertions(Map.of("payments-token", assertion));

        OAuth2ConfigurationValidationResult result =
                validator.validate(
                        properties,
                        new InMemoryClientRegistrationRepository(registration("payments-token")));

        assertThat(result.warnings())
                .containsExactly(
                        OAuth2ConfigurationValidationIssue.warning(
                                "authentication.client-assertions.payments-token",
                                "is configured but OAuth2 registration payments-token does not use private_key_jwt"));
    }

    @Test
    void warnsWhenClientCredentialsExpectedScopesAreNotRequestedBySpringRegistration() {
        RestClientProperties.Client client =
                OAuth2Client(AuthenticationType.CLIENT_CREDENTIALS, "payments-token");
        client.setScopes("payment.read payment.write");
        RestClientProperties properties = new RestClientProperties();
        properties.setClients(Map.of("payments", client));

        OAuth2ConfigurationValidationResult result =
                validator.validate(
                        properties,
                        new InMemoryClientRegistrationRepository(registration("payments-token")));

        assertThat(result.warnings())
                .containsExactly(
                        OAuth2ConfigurationValidationIssue.warning(
                                "clients.payments.scopes",
                                "contains expected scopes not requested by Spring registration payments-token: payment.write"));
    }

    @Test
    void reportsJwtBearerGrantMismatch() {
        RestClientProperties properties = new RestClientProperties();
        properties.setClients(
                Map.of("payments", OAuth2Client(AuthenticationType.JWT_BEARER, "payments-token")));
        properties
                .getAuthentication()
                .setJwtBearer(Map.of("payments-token", jwtBearer("payments-signing-key")));

        OAuth2ConfigurationValidationResult result =
                validator.validate(
                        properties,
                        new InMemoryClientRegistrationRepository(registration("payments-token")));

        assertThat(result.errors())
                .containsExactly(
                        OAuth2ConfigurationValidationIssue.error(
                                "clients.payments.token-request-id",
                                "references OAuth2 registration payments-token with authorization-grant-type "
                                        + "client_credentials; expected urn:ietf:params:oauth:grant-type:jwt-bearer"));
    }

    @Test
    void reportsUnsupportedJwtBearerAuthenticationMethod() {
        RestClientProperties properties = new RestClientProperties();
        properties.setClients(
                Map.of("payments", OAuth2Client(AuthenticationType.JWT_BEARER, "payments-token")));
        properties
                .getAuthentication()
                .setJwtBearer(Map.of("payments-token", jwtBearer("payments-signing-key")));

        OAuth2ConfigurationValidationResult result =
                validator.validate(
                        properties,
                        new InMemoryClientRegistrationRepository(
                                jwtBearerRegistration(
                                        "payments-token",
                                        ClientAuthenticationMethod.CLIENT_SECRET_JWT,
                                        "payments-secret")));

        assertThat(result.errors())
                .containsExactly(
                        OAuth2ConfigurationValidationIssue.error(
                                "spring.security.oauth2.client.registration.payments-token.client-authentication-method",
                                "uses unsupported client authentication method client_secret_jwt for JWT_BEARER; "
                                        + "expected none, client_secret_basic, client_secret_post, or private_key_jwt"));
    }

    @Test
    void reportsMissingJwtBearerExtensionConfiguration() {
        RestClientProperties properties = new RestClientProperties();
        properties.setClients(
                Map.of("payments", OAuth2Client(AuthenticationType.JWT_BEARER, "payments-token")));

        OAuth2ConfigurationValidationResult result =
                validator.validate(
                        properties,
                        new InMemoryClientRegistrationRepository(
                                jwtBearerRegistration("payments-token")));

        assertThat(result.errors())
                .containsExactly(
                        OAuth2ConfigurationValidationIssue.error(
                                "authentication.jwt-bearer.payments-token",
                                "is required when REST client uses JWT_BEARER with OAuth2 registration payments-token"));
    }

    @Test
    void reportsMissingJwtBearerKeyStoreId() {
        RestClientProperties properties = new RestClientProperties();
        properties.setClients(
                Map.of("payments", OAuth2Client(AuthenticationType.JWT_BEARER, "payments-token")));
        properties.getAuthentication().setJwtBearer(Map.of("payments-token", jwtBearer(" ")));

        OAuth2ConfigurationValidationResult result =
                validator.validate(
                        properties,
                        new InMemoryClientRegistrationRepository(
                                jwtBearerRegistration("payments-token")));

        assertThat(result.errors())
                .containsExactly(
                        OAuth2ConfigurationValidationIssue.error(
                                "authentication.jwt-bearer.payments-token.key-store-id",
                                "is required when REST client uses JWT_BEARER with OAuth2 registration payments-token"));
    }

    @Test
    void acceptsJwtBearerWithNoneClientAuthenticationAndSigningKeyStoreId() {
        RestClientProperties properties = new RestClientProperties();
        properties.setClients(
                Map.of("payments", OAuth2Client(AuthenticationType.JWT_BEARER, "payments-token")));
        properties
                .getAuthentication()
                .setJwtBearer(Map.of("payments-token", jwtBearer("payments-signing-key")));

        OAuth2ConfigurationValidationResult result =
                validator.validate(
                        properties,
                        new InMemoryClientRegistrationRepository(
                                jwtBearerRegistration("payments-token")));

        assertThat(result.issues()).isEmpty();
    }

    @Test
    void validatesPrivateKeyJwtClientAuthenticationForJwtBearerGrant() {
        RestClientProperties properties = new RestClientProperties();
        properties.setClients(
                Map.of("payments", OAuth2Client(AuthenticationType.JWT_BEARER, "payments-token")));
        RestClientProperties.ClientAssertion assertion = new RestClientProperties.ClientAssertion();
        assertion.setKeyStoreId("client-auth-signing-key");
        properties.getAuthentication().setClientAssertions(Map.of("payments-token", assertion));
        properties
                .getAuthentication()
                .setJwtBearer(Map.of("payments-token", jwtBearer("jwt-bearer-signing-key")));

        OAuth2ConfigurationValidationResult result =
                validator.validate(
                        properties,
                        new InMemoryClientRegistrationRepository(
                                jwtBearerRegistration(
                                        "payments-token",
                                        ClientAuthenticationMethod.PRIVATE_KEY_JWT,
                                        null)));

        assertThat(result.issues()).isEmpty();
    }

    @Test
    void reportsMissingClientSecretForJwtBearerClientSecretAuthentication() {
        RestClientProperties properties = new RestClientProperties();
        properties.setClients(
                Map.of("payments", OAuth2Client(AuthenticationType.JWT_BEARER, "payments-token")));
        properties
                .getAuthentication()
                .setJwtBearer(Map.of("payments-token", jwtBearer("payments-signing-key")));

        OAuth2ConfigurationValidationResult result =
                validator.validate(
                        properties,
                        new InMemoryClientRegistrationRepository(
                                jwtBearerRegistration(
                                        "payments-token",
                                        ClientAuthenticationMethod.CLIENT_SECRET_POST,
                                        "")));

        assertThat(result.errors())
                .containsExactly(
                        OAuth2ConfigurationValidationIssue.error(
                                "spring.security.oauth2.client.registration.payments-token.client-secret",
                                "is required when client-authentication-method is client_secret_post"));
    }

    @Test
    void warnsWhenJwtBearerExpectedScopesAreNotRequestedBySpringRegistration() {
        RestClientProperties.Client client =
                OAuth2Client(AuthenticationType.JWT_BEARER, "payments-token");
        client.setScopes("payment.read payment.write");
        RestClientProperties properties = new RestClientProperties();
        properties.setClients(Map.of("payments", client));
        properties
                .getAuthentication()
                .setJwtBearer(Map.of("payments-token", jwtBearer("payments-signing-key")));

        OAuth2ConfigurationValidationResult result =
                validator.validate(
                        properties,
                        new InMemoryClientRegistrationRepository(
                                jwtBearerRegistration("payments-token")));

        assertThat(result.warnings())
                .containsExactly(
                        OAuth2ConfigurationValidationIssue.warning(
                                "clients.payments.scopes",
                                "contains expected scopes not requested by Spring registration payments-token: payment.write"));
    }

    @Test
    void warnsAboutConfiguredOAuth2ExtensionsThatNoClientReferences() {
        RestClientProperties properties = new RestClientProperties();
        properties.setClients(
                Map.of("payments", OAuth2Client(AuthenticationType.JWT_BEARER, "payments-token")));
        RestClientProperties.Authentication authentication = properties.getAuthentication();
        authentication.setClientAssertions(
                Map.of("unused-client-assertion", new RestClientProperties.ClientAssertion()));
        authentication.setJwtBearer(
                Map.of(
                        "payments-token",
                        jwtBearer("payments-signing-key"),
                        "unused-jwt-bearer",
                        new RestClientProperties.JwtBearer()));

        OAuth2ConfigurationValidationResult result =
                validator.validate(
                        properties,
                        new InMemoryClientRegistrationRepository(
                                jwtBearerRegistration("payments-token")));

        assertThat(result.warnings())
                .containsExactly(
                        OAuth2ConfigurationValidationIssue.warning(
                                "authentication.client-assertions.unused-client-assertion",
                                "is configured but no enabled OAuth2 REST client uses private_key_jwt with this registration id"),
                        OAuth2ConfigurationValidationIssue.warning(
                                "authentication.jwt-bearer.unused-jwt-bearer",
                                "is configured but no enabled JWT_BEARER REST client references this registration id"));
    }

    @Test
    void handlesNullNestedMaps() {
        RestClientProperties properties = new RestClientProperties();
        properties.setClients(null);
        properties.getAuthentication().setClientAssertions(null);
        properties.getAuthentication().setJwtBearer(null);

        OAuth2ConfigurationValidationResult result = validator.validate(properties, null);

        assertThat(result.issues()).isEmpty();
    }

    @Test
    void preservesClientIterationOrderInIssues() {
        Map<String, RestClientProperties.Client> clients = new LinkedHashMap<>();
        clients.put("payments", OAuth2Client(AuthenticationType.CLIENT_CREDENTIALS, ""));
        clients.put("orders", OAuth2Client(AuthenticationType.JWT_BEARER, ""));
        RestClientProperties properties = new RestClientProperties();
        properties.setClients(clients);

        OAuth2ConfigurationValidationResult result = validator.validate(properties, null);

        assertThat(result.errors())
                .containsExactly(
                        OAuth2ConfigurationValidationIssue.error(
                                "clients.payments.token-request-id",
                                "is required for CLIENT_CREDENTIALS"),
                        OAuth2ConfigurationValidationIssue.error(
                                "clients.orders.token-request-id", "is required for JWT_BEARER"));
    }

    private RestClientProperties.Client OAuth2Client(
            AuthenticationType authenticationType, String tokenRequestId) {
        RestClientProperties.Client client = new RestClientProperties.Client();
        client.setAuthenticationType(authenticationType);
        client.setTokenRequestId(tokenRequestId);
        return client;
    }

    private ClientRegistration registration(String registrationId) {
        return registration(
                registrationId, ClientAuthenticationMethod.CLIENT_SECRET_BASIC, "payments-secret");
    }

    private ClientRegistration privateKeyJwtRegistration(String registrationId) {
        return registration(registrationId, ClientAuthenticationMethod.PRIVATE_KEY_JWT, null);
    }

    private ClientRegistration registration(
            String registrationId,
            ClientAuthenticationMethod clientAuthenticationMethod,
            String clientSecret) {
        return ClientRegistration.withRegistrationId(registrationId)
                .tokenUri("https://auth.example/oauth2/token")
                .clientId("payments-client")
                .clientSecret(clientSecret)
                .clientAuthenticationMethod(clientAuthenticationMethod)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .scope("payment.read")
                .build();
    }

    private ClientRegistration jwtBearerRegistration(String registrationId) {
        return jwtBearerRegistration(registrationId, ClientAuthenticationMethod.NONE, null);
    }

    private ClientRegistration jwtBearerRegistration(
            String registrationId,
            ClientAuthenticationMethod clientAuthenticationMethod,
            String clientSecret) {
        return ClientRegistration.withRegistrationId(registrationId)
                .tokenUri("https://auth.example/oauth2/token")
                .clientId("payments-client")
                .clientSecret(clientSecret)
                .clientAuthenticationMethod(clientAuthenticationMethod)
                .authorizationGrantType(
                        new AuthorizationGrantType("urn:ietf:params:oauth:grant-type:jwt-bearer"))
                .scope("payment.read")
                .build();
    }

    private RestClientProperties.JwtBearer jwtBearer(String keyStoreId) {
        RestClientProperties.JwtBearer jwtBearer = new RestClientProperties.JwtBearer();
        jwtBearer.setKeyStoreId(keyStoreId);
        return jwtBearer;
    }

    private RestClientProperties jwtBearerSigningProperties(String keyStoreBase64) {
        RestClientProperties.Client client =
                OAuth2Client(AuthenticationType.JWT_BEARER, "payments-token");
        RestClientProperties.KeyStore keyStore = new RestClientProperties.KeyStore();
        keyStore.setBase64(keyStoreBase64);
        keyStore.setType("PKCS12");
        keyStore.setPassword("changeit");
        keyStore.setKeyPassword("changeit");
        keyStore.setKeyAlias("auth");

        RestClientProperties properties = new RestClientProperties();
        properties.setClients(Map.of("payments", client));
        properties.getAuthentication().setKeyStores(Map.of("payments-signing-key", keyStore));
        properties
                .getAuthentication()
                .setJwtBearer(Map.of("payments-token", jwtBearer("payments-signing-key")));
        return properties;
    }

    private OAuth2RestClientConfigurationValidator contentValidator(
            RestClientProperties properties) {
        CredentialDefinitionSource credentialDefinitionSource =
                new PropertiesCredentialDefinitionSource(
                        properties, new CredentialPropertiesMapper());
        CredentialProvider credentialProvider =
                new PropertiesCredentialProvider(credentialDefinitionSource);
        CredentialResolver credentialResolver = new CredentialResolver(credentialProvider);
        KeyStoreDefinitionSource keyStoreDefinitionSource =
                new PropertiesKeyStoreDefinitionSource(
                        properties, new KeyStorePropertiesMapper(credentialResolver));
        KeyStoreManager keyStoreManager =
                new KeyStoreManager(keyStoreDefinitionSource, new DefaultResourceLoader());
        SigningJwkResolver signingJwkResolver =
                new SigningJwkResolver(new PrivateKeyLoader(keyStoreManager));
        return new OAuth2RestClientConfigurationValidator(keyStoreManager, signingJwkResolver);
    }

    private Path createKeyStore(String fileName, String alias) throws Exception {
        Path keyStore = tempDir.resolve(fileName);
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
                                "CN=OAuth2 Validator Test",
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

    private RestClientProperties.Credential credential(String value) {
        RestClientProperties.Credential credential = new RestClientProperties.Credential();
        credential.setValue(value);
        return credential;
    }
}
