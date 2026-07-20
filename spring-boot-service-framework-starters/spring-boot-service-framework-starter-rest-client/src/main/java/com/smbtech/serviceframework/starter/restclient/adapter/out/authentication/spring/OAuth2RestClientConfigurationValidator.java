package com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring;

import com.smbtech.serviceframework.httpclient.domain.AuthenticationType;
import com.smbtech.serviceframework.httpclient.domain.KeyStoreDefinition;
import com.smbtech.serviceframework.httpclient.exception.HttpClientAuthenticationException;
import com.smbtech.serviceframework.httpclient.service.ScopeValidator;
import com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.keystore.KeyStoreManager;
import com.smbtech.serviceframework.starter.restclient.autoconfigure.RestClientProperties;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

/** Provides OAuth2 rest client configuration validator behavior. */
public final class OAuth2RestClientConfigurationValidator {

    private static final AuthorizationGrantType JWT_BEARER_GRANT =
            new AuthorizationGrantType("urn:ietf:params:oauth:grant-type:jwt-bearer");
    private static final Set<ClientAuthenticationMethod> CLIENT_CREDENTIALS_AUTHENTICATION_METHODS =
            Set.of(
                    ClientAuthenticationMethod.CLIENT_SECRET_BASIC,
                    ClientAuthenticationMethod.CLIENT_SECRET_POST,
                    ClientAuthenticationMethod.PRIVATE_KEY_JWT);
    private static final Set<ClientAuthenticationMethod> JWT_BEARER_AUTHENTICATION_METHODS =
            Set.of(
                    ClientAuthenticationMethod.NONE,
                    ClientAuthenticationMethod.CLIENT_SECRET_BASIC,
                    ClientAuthenticationMethod.CLIENT_SECRET_POST,
                    ClientAuthenticationMethod.PRIVATE_KEY_JWT);

    private final ScopeValidator scopeValidator = new ScopeValidator();
    private final KeyStoreManager keyStoreManager;
    private final SigningJwkResolver signingJwkResolver;

    /** Creates a OAuth2 rest client configuration validator instance. */
    public OAuth2RestClientConfigurationValidator() {
        this(null, null);
    }

    /**
     * Creates a OAuth2 rest client configuration validator instance.
     *
     * @param keyStoreManager key store manager value
     * @param signingJwkResolver signing jwk resolver value
     */
    public OAuth2RestClientConfigurationValidator(
            KeyStoreManager keyStoreManager, SigningJwkResolver signingJwkResolver) {
        this.keyStoreManager = keyStoreManager;
        this.signingJwkResolver = signingJwkResolver;
    }

    /**
     * Performs the validate operation.
     *
     * @param properties properties value
     * @param clientRegistrationRepository client registration repository value
     * @return validate result
     */
    public OAuth2ConfigurationValidationResult validate(
            RestClientProperties properties,
            ClientRegistrationRepository clientRegistrationRepository) {
        RestClientProperties safeProperties =
                Objects.requireNonNullElseGet(properties, RestClientProperties::new);
        RestClientProperties.Validation validation =
                Objects.requireNonNullElseGet(
                        safeProperties.getValidation(), RestClientProperties.Validation::new);
        if (!validation.isEnabled()) {
            return OAuth2ConfigurationValidationResult.empty();
        }
        boolean validateKeyStoreContent = validation.isValidateKeyStoreContent();

        RestClientProperties.Authentication authentication =
                Objects.requireNonNullElseGet(
                        safeProperties.getAuthentication(),
                        RestClientProperties.Authentication::new);
        OAuth2ConfigurationValidationResult.Builder result =
                OAuth2ConfigurationValidationResult.builder();
        ValidationReferences references = new ValidationReferences();

        validateCredentialReferences(
                safeProperties.getClients(), authentication, references, result);
        collectSslKeyStoreReferences(safeProperties.getClients(), references);
        validateClients(
                authentication,
                safeProperties.getClients(),
                clientRegistrationRepository,
                references,
                result);
        validateConfiguredExtensionUsage(authentication, references, result);
        collectUsedKeyStoreCredentialReferences(authentication, references);
        validateKeyStoreContent(validateKeyStoreContent, references, result);
        validateUnusedReferences(authentication, references, result);

        return result.build();
    }

    private void validateCredentialReferences(
            Map<String, RestClientProperties.Client> clients,
            RestClientProperties.Authentication authentication,
            ValidationReferences references,
            OAuth2ConfigurationValidationResult.Builder result) {
        Set<String> credentialIds = safeMap(authentication.getCredentials()).keySet();
        safeMap(clients)
                .forEach(
                        (clientName, client) -> {
                            if (client == null || !client.isEnabled()) {
                                return;
                            }

                            RestClientProperties.BasicAuthentication basicAuthentication =
                                    basicAuthentication(client);
                            validateCredentialReference(
                                    "clients." + clientName + ".basic-authentication.username-ref",
                                    basicAuthentication.getUsernameRef(),
                                    credentialIds,
                                    result);
                            collectUsedBasicAuthenticationCredentialReference(
                                    client, basicAuthentication.getUsernameRef(), references);
                            validateCredentialReference(
                                    "clients." + clientName + ".basic-authentication.password-ref",
                                    basicAuthentication.getPasswordRef(),
                                    credentialIds,
                                    result);
                            collectUsedBasicAuthenticationCredentialReference(
                                    client, basicAuthentication.getPasswordRef(), references);
                        });

        safeMap(authentication.getKeyStores())
                .forEach(
                        (keyStoreId, keyStore) -> {
                            if (keyStore == null) {
                                return;
                            }

                            validateCredentialReference(
                                    "authentication.key-stores." + keyStoreId + ".password-ref",
                                    keyStore.getPasswordRef(),
                                    credentialIds,
                                    result);
                            validateCredentialReference(
                                    "authentication.key-stores." + keyStoreId + ".key-password-ref",
                                    keyStore.getKeyPasswordRef(),
                                    credentialIds,
                                    result);
                        });
    }

    private void validateCredentialReference(
            String path,
            String credentialRef,
            Set<String> credentialIds,
            OAuth2ConfigurationValidationResult.Builder result) {
        String normalizedCredentialRef = normalize(credentialRef);
        if (normalizedCredentialRef.isBlank() || credentialIds.contains(normalizedCredentialRef)) {
            return;
        }

        result.error(path, "references missing credential " + normalizedCredentialRef);
    }

    private void collectUsedBasicAuthenticationCredentialReference(
            RestClientProperties.Client client,
            String credentialRef,
            ValidationReferences references) {
        if (client.getAuthenticationType() == AuthenticationType.BASIC_AUTH) {
            addNormalized(references.credentialIds, credentialRef);
        }
    }

    private RestClientProperties.BasicAuthentication basicAuthentication(
            RestClientProperties.Client client) {
        return Objects.requireNonNullElseGet(
                client.getBasicAuthentication(), RestClientProperties.BasicAuthentication::new);
    }

    private void collectSslKeyStoreReferences(
            Map<String, RestClientProperties.Client> clients, ValidationReferences references) {
        safeMap(clients).values().stream()
                .filter(Objects::nonNull)
                .filter(RestClientProperties.Client::isEnabled)
                .map(this::ssl)
                .forEach(
                        ssl -> {
                            addNormalized(references.keyStoreIds, ssl.getTrustStoreId());
                            addNormalized(references.sslTrustStoreIds, ssl.getTrustStoreId());
                            addNormalized(references.keyStoreIds, ssl.getKeyStoreId());
                            addNormalized(references.sslKeyStoreIds, ssl.getKeyStoreId());
                        });
    }

    private RestClientProperties.Ssl ssl(RestClientProperties.Client client) {
        RestClientProperties.Apache apache =
                Objects.requireNonNullElseGet(client.getApache(), RestClientProperties.Apache::new);
        return Objects.requireNonNullElseGet(apache.getSsl(), RestClientProperties.Ssl::new);
    }

    private void validateClients(
            RestClientProperties.Authentication authentication,
            Map<String, RestClientProperties.Client> clients,
            ClientRegistrationRepository clientRegistrationRepository,
            ValidationReferences references,
            OAuth2ConfigurationValidationResult.Builder result) {
        safeMap(clients)
                .forEach(
                        (clientName, client) -> {
                            if (client == null
                                    || !client.isEnabled()
                                    || !requiresOAuth2(client.getAuthenticationType())) {
                                return;
                            }

                            String path = "clients." + clientName;
                            String tokenRequestId = normalize(client.getTokenRequestId());
                            if (tokenRequestId.isBlank()) {
                                result.error(
                                        path + ".token-request-id",
                                        "is required for " + client.getAuthenticationType());
                                return;
                            }

                            references.tokenRequestIds.add(tokenRequestId);
                            if (clientRegistrationRepository == null) {
                                result.error(
                                        path + ".token-request-id",
                                        "references "
                                                + tokenRequestId
                                                + " but no ClientRegistrationRepository is available");
                                return;
                            }

                            ClientRegistration registration =
                                    clientRegistrationRepository.findByRegistrationId(
                                            tokenRequestId);
                            if (registration == null) {
                                result.error(
                                        path + ".token-request-id",
                                        "references missing OAuth2 registration " + tokenRequestId);
                                return;
                            }

                            if (client.getAuthenticationType()
                                    == AuthenticationType.CLIENT_CREDENTIALS) {
                                validateClientCredentials(
                                        authentication,
                                        path,
                                        client,
                                        registration,
                                        references,
                                        result);
                            }
                            if (client.getAuthenticationType() == AuthenticationType.JWT_BEARER) {
                                validateJwtBearer(
                                        authentication,
                                        path,
                                        client,
                                        registration,
                                        references,
                                        result);
                            }
                        });
    }

    private void validateClientCredentials(
            RestClientProperties.Authentication authentication,
            String path,
            RestClientProperties.Client client,
            ClientRegistration registration,
            ValidationReferences references,
            OAuth2ConfigurationValidationResult.Builder result) {
        String registrationId = registration.getRegistrationId();
        if (!AuthorizationGrantType.CLIENT_CREDENTIALS.equals(
                registration.getAuthorizationGrantType())) {
            result.error(
                    path + ".token-request-id",
                    "references OAuth2 registration "
                            + registrationId
                            + " with authorization-grant-type "
                            + registration.getAuthorizationGrantType().getValue()
                            + "; expected client_credentials");
            return;
        }

        ClientAuthenticationMethod authenticationMethod =
                registration.getClientAuthenticationMethod();
        if (!CLIENT_CREDENTIALS_AUTHENTICATION_METHODS.contains(authenticationMethod)) {
            result.error(
                    "spring.security.oauth2.client.registration."
                            + registrationId
                            + ".client-authentication-method",
                    "uses unsupported client authentication method "
                            + authenticationMethod.getValue()
                            + " for CLIENT_CREDENTIALS; expected client_secret_basic, client_secret_post, or private_key_jwt");
            return;
        }

        if (ClientAuthenticationMethod.PRIVATE_KEY_JWT.equals(authenticationMethod)) {
            references.clientAssertionIds.add(registrationId);
            validatePrivateKeyJwtClientAssertion(
                    authentication, registrationId, references, result);
        } else {
            validateClientSecret(registration, result);
            warnAboutUnusedClientAssertionForRegistration(authentication, registrationId, result);
        }

        validateExpectedScopes(path, client, registration, result);
    }

    private void validateJwtBearer(
            RestClientProperties.Authentication authentication,
            String path,
            RestClientProperties.Client client,
            ClientRegistration registration,
            ValidationReferences references,
            OAuth2ConfigurationValidationResult.Builder result) {
        String registrationId = registration.getRegistrationId();
        references.jwtBearerIds.add(registrationId);

        if (!JWT_BEARER_GRANT.equals(registration.getAuthorizationGrantType())) {
            result.error(
                    path + ".token-request-id",
                    "references OAuth2 registration "
                            + registrationId
                            + " with authorization-grant-type "
                            + registration.getAuthorizationGrantType().getValue()
                            + "; expected urn:ietf:params:oauth:grant-type:jwt-bearer");
            return;
        }

        ClientAuthenticationMethod authenticationMethod =
                registration.getClientAuthenticationMethod();
        if (!JWT_BEARER_AUTHENTICATION_METHODS.contains(authenticationMethod)) {
            result.error(
                    "spring.security.oauth2.client.registration."
                            + registrationId
                            + ".client-authentication-method",
                    "uses unsupported client authentication method "
                            + authenticationMethod.getValue()
                            + " for JWT_BEARER; expected none, client_secret_basic, client_secret_post, or private_key_jwt");
            return;
        }

        if (ClientAuthenticationMethod.PRIVATE_KEY_JWT.equals(authenticationMethod)) {
            references.clientAssertionIds.add(registrationId);
            validatePrivateKeyJwtClientAssertion(
                    authentication, registrationId, references, result);
        } else {
            if (ClientAuthenticationMethod.CLIENT_SECRET_BASIC.equals(authenticationMethod)
                    || ClientAuthenticationMethod.CLIENT_SECRET_POST.equals(authenticationMethod)) {
                validateClientSecret(registration, result);
            }
            warnAboutUnusedClientAssertionForRegistration(authentication, registrationId, result);
        }

        validateJwtBearerExtension(authentication, registrationId, references, result);
        validateExpectedScopes(path, client, registration, result);
    }

    private void validateJwtBearerExtension(
            RestClientProperties.Authentication authentication,
            String registrationId,
            ValidationReferences references,
            OAuth2ConfigurationValidationResult.Builder result) {
        RestClientProperties.JwtBearer jwtBearer =
                safeMap(authentication.getJwtBearer()).get(registrationId);
        if (jwtBearer == null) {
            result.error(
                    "authentication.jwt-bearer." + registrationId,
                    "is required when REST client uses JWT_BEARER with OAuth2 registration "
                            + registrationId);
            return;
        }

        if (normalize(jwtBearer.getKeyStoreId()).isBlank()) {
            result.error(
                    "authentication.jwt-bearer." + registrationId + ".key-store-id",
                    "is required when REST client uses JWT_BEARER with OAuth2 registration "
                            + registrationId);
            return;
        }

        addNormalized(references.keyStoreIds, jwtBearer.getKeyStoreId());
        addNormalized(references.signingKeyStoreIds, jwtBearer.getKeyStoreId());
    }

    private void validatePrivateKeyJwtClientAssertion(
            RestClientProperties.Authentication authentication,
            String registrationId,
            ValidationReferences references,
            OAuth2ConfigurationValidationResult.Builder result) {
        RestClientProperties.ClientAssertion assertion =
                safeMap(authentication.getClientAssertions()).get(registrationId);
        if (assertion == null) {
            result.error(
                    "authentication.client-assertions." + registrationId,
                    "is required when OAuth2 registration "
                            + registrationId
                            + " uses private_key_jwt");
            return;
        }

        if (normalize(assertion.getKeyStoreId()).isBlank()) {
            result.error(
                    "authentication.client-assertions." + registrationId + ".key-store-id",
                    "is required when OAuth2 registration "
                            + registrationId
                            + " uses private_key_jwt");
        }

        addNormalized(references.keyStoreIds, assertion.getKeyStoreId());
        addNormalized(references.signingKeyStoreIds, assertion.getKeyStoreId());
    }

    private void validateClientSecret(
            ClientRegistration registration, OAuth2ConfigurationValidationResult.Builder result) {
        if (normalize(registration.getClientSecret()).isBlank()) {
            result.error(
                    "spring.security.oauth2.client.registration."
                            + registration.getRegistrationId()
                            + ".client-secret",
                    "is required when client-authentication-method is "
                            + registration.getClientAuthenticationMethod().getValue());
        }
    }

    private void warnAboutUnusedClientAssertionForRegistration(
            RestClientProperties.Authentication authentication,
            String registrationId,
            OAuth2ConfigurationValidationResult.Builder result) {
        if (safeMap(authentication.getClientAssertions()).containsKey(registrationId)) {
            result.warning(
                    "authentication.client-assertions." + registrationId,
                    "is configured but OAuth2 registration "
                            + registrationId
                            + " does not use private_key_jwt");
        }
    }

    private void validateExpectedScopes(
            String path,
            RestClientProperties.Client client,
            ClientRegistration registration,
            OAuth2ConfigurationValidationResult.Builder result) {
        Set<String> expectedScopes = scopeValidator.parse(client.getScopes());
        if (expectedScopes.isEmpty() || registration.getScopes().containsAll(expectedScopes)) {
            return;
        }

        Set<String> missingScopes = new LinkedHashSet<>(expectedScopes);
        missingScopes.removeAll(registration.getScopes());
        result.warning(
                path + ".scopes",
                "contains expected scopes not requested by Spring registration "
                        + registration.getRegistrationId()
                        + ": "
                        + String.join(" ", missingScopes));
    }

    private void validateConfiguredExtensionUsage(
            RestClientProperties.Authentication authentication,
            ValidationReferences references,
            OAuth2ConfigurationValidationResult.Builder result) {
        safeMap(authentication.getClientAssertions()).keySet().stream()
                .filter(registrationId -> !references.clientAssertionIds.contains(registrationId))
                .filter(registrationId -> !references.tokenRequestIds.contains(registrationId))
                .forEach(
                        registrationId ->
                                result.warning(
                                        "authentication.client-assertions." + registrationId,
                                        "is configured but no enabled OAuth2 REST client uses private_key_jwt with this registration id"));

        safeMap(authentication.getJwtBearer()).keySet().stream()
                .filter(registrationId -> !references.jwtBearerIds.contains(registrationId))
                .forEach(
                        registrationId ->
                                result.warning(
                                        "authentication.jwt-bearer." + registrationId,
                                        "is configured but no enabled JWT_BEARER REST client references this registration id"));
    }

    private void collectUsedKeyStoreCredentialReferences(
            RestClientProperties.Authentication authentication, ValidationReferences references) {
        safeMap(authentication.getKeyStores())
                .forEach(
                        (keyStoreId, keyStore) -> {
                            if (keyStore == null || !references.keyStoreIds.contains(keyStoreId)) {
                                return;
                            }

                            addNormalized(references.credentialIds, keyStore.getPasswordRef());
                            addNormalized(references.credentialIds, keyStore.getKeyPasswordRef());
                        });
    }

    private void validateKeyStoreContent(
            boolean validateKeyStoreContent,
            ValidationReferences references,
            OAuth2ConfigurationValidationResult.Builder result) {
        if (!validateKeyStoreContent || references.keyStoreIds.isEmpty()) {
            return;
        }
        if (keyStoreManager == null) {
            result.error(
                    "validation.validate-key-store-content",
                    "requires KeyStoreManager to validate configured key store content");
            return;
        }

        references.sslTrustStoreIds.forEach(
                keyStoreId -> validateLoadableKeyStore(keyStoreId, "SSL trust store", result));
        references.sslKeyStoreIds.forEach(keyStoreId -> validateMtlsKeyStore(keyStoreId, result));
        references.signingKeyStoreIds.forEach(
                keyStoreId -> validateSigningKeyStore(keyStoreId, result));
    }

    private void validateLoadableKeyStore(
            String keyStoreId, String usage, OAuth2ConfigurationValidationResult.Builder result) {
        try {
            keyStoreManager.getKeyStore(keyStoreId);
        } catch (Exception exception) {
            result.error(
                    "authentication.key-stores." + keyStoreId,
                    "cannot be loaded for "
                            + usage
                            + " content validation: "
                            + rootMessage(exception));
        }
    }

    private void validateMtlsKeyStore(
            String keyStoreId, OAuth2ConfigurationValidationResult.Builder result) {
        try {
            KeyStoreDefinition definition = keyStoreManager.getDefinition(keyStoreId);
            if (definition.keyAlias().isBlank()) {
                result.error(
                        "authentication.key-stores." + keyStoreId + ".key-alias",
                        "is required for mTLS key store content validation");
                return;
            }

            KeyStore keyStore = keyStoreManager.getKeyStore(keyStoreId);
            if (!keyStore.containsAlias(definition.keyAlias())) {
                result.error(
                        "authentication.key-stores." + keyStoreId + ".key-alias",
                        "references missing alias " + definition.keyAlias());
                return;
            }

            Key key =
                    keyStore.getKey(
                            definition.keyAlias(), KeyStoreManager.chars(definition.keyPassword()));
            if (!(key instanceof PrivateKey)) {
                result.error(
                        "authentication.key-stores." + keyStoreId + ".key-alias",
                        "does not contain a private key: " + definition.keyAlias());
            }
        } catch (Exception exception) {
            result.error(
                    "authentication.key-stores." + keyStoreId,
                    "cannot be loaded for mTLS key store content validation: "
                            + rootMessage(exception));
        }
    }

    private void validateSigningKeyStore(
            String keyStoreId, OAuth2ConfigurationValidationResult.Builder result) {
        if (signingJwkResolver == null) {
            result.error(
                    "validation.validate-key-store-content",
                    "requires SigningJwkResolver to validate JWT signing key store content");
            return;
        }

        try {
            signingJwkResolver.resolve(keyStoreId);
        } catch (Exception exception) {
            result.error(
                    "authentication.key-stores." + keyStoreId,
                    "cannot be used for JWT signing content validation: " + rootMessage(exception));
        }
    }

    private String rootMessage(Exception exception) {
        Throwable current = exception;
        while (current.getCause() != null
                && !(current instanceof HttpClientAuthenticationException)) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return message == null || message.isBlank() ? current.getClass().getSimpleName() : message;
    }

    private void validateUnusedReferences(
            RestClientProperties.Authentication authentication,
            ValidationReferences references,
            OAuth2ConfigurationValidationResult.Builder result) {
        safeMap(authentication.getCredentials()).keySet().stream()
                .filter(credentialId -> !references.credentialIds.contains(credentialId))
                .forEach(
                        credentialId ->
                                result.warning(
                                        "authentication.credentials." + credentialId,
                                        "is configured but no enabled REST client or used key store references this credential id"));

        safeMap(authentication.getKeyStores()).keySet().stream()
                .filter(keyStoreId -> !references.keyStoreIds.contains(keyStoreId))
                .forEach(
                        keyStoreId ->
                                result.warning(
                                        "authentication.key-stores." + keyStoreId,
                                        "is configured but no enabled REST client or OAuth2 signing configuration references this key store id"));
    }

    private boolean requiresOAuth2(AuthenticationType authenticationType) {
        return authenticationType == AuthenticationType.CLIENT_CREDENTIALS
                || authenticationType == AuthenticationType.JWT_BEARER;
    }

    private String normalize(String value) {
        return Objects.requireNonNullElse(value, "").trim();
    }

    private void addNormalized(Set<String> values, String value) {
        String normalizedValue = normalize(value);
        if (!normalizedValue.isBlank()) {
            values.add(normalizedValue);
        }
    }

    private <K, V> Map<K, V> safeMap(Map<K, V> values) {
        return values == null ? Map.of() : values;
    }

    private static final class ValidationReferences {
        private final Set<String> tokenRequestIds = new LinkedHashSet<>();
        private final Set<String> clientAssertionIds = new LinkedHashSet<>();
        private final Set<String> jwtBearerIds = new LinkedHashSet<>();
        private final Set<String> credentialIds = new LinkedHashSet<>();
        private final Set<String> keyStoreIds = new LinkedHashSet<>();
        private final Set<String> signingKeyStoreIds = new LinkedHashSet<>();
        private final Set<String> sslTrustStoreIds = new LinkedHashSet<>();
        private final Set<String> sslKeyStoreIds = new LinkedHashSet<>();
    }
}
