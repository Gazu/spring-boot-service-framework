package com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class OAuth2ConfigurationValidationResultTest {

    @Test
    void emptyResultIsValidAndDoesNotFail() {
        OAuth2ConfigurationValidationResult result = OAuth2ConfigurationValidationResult.empty();

        assertThat(result.issues()).isEmpty();
        assertThat(result.errors()).isEmpty();
        assertThat(result.warnings()).isEmpty();
        assertThat(result.isValid()).isTrue();
        assertThat(result.shouldFail(false)).isFalse();
        assertThat(result.shouldFail(true)).isFalse();
    }

    @Test
    void separatesErrorsAndWarnings() {
        OAuth2ConfigurationValidationResult result =
                OAuth2ConfigurationValidationResult.builder()
                        .error("clients.payments.token-request-id", "is required")
                        .warning("authentication.jwt-bearer.old-token", "is not used")
                        .build();

        assertThat(result.issues()).hasSize(2);
        assertThat(result.errors())
                .containsExactly(
                        OAuth2ConfigurationValidationIssue.error(
                                "clients.payments.token-request-id", "is required"));
        assertThat(result.warnings())
                .containsExactly(
                        OAuth2ConfigurationValidationIssue.warning(
                                "authentication.jwt-bearer.old-token", "is not used"));
        assertThat(result.isValid()).isFalse();
    }

    @Test
    void warningsOnlyFailWhenConfigured() {
        OAuth2ConfigurationValidationResult result =
                OAuth2ConfigurationValidationResult.builder()
                        .warning("authentication.client-assertions.old-token", "is not used")
                        .build();

        assertThat(result.isValid()).isTrue();
        assertThat(result.shouldFail(false)).isFalse();
        assertThat(result.shouldFail(true)).isTrue();
    }

    @Test
    void errorsAlwaysFail() {
        OAuth2ConfigurationValidationResult result =
                OAuth2ConfigurationValidationResult.builder()
                        .error(
                                "clients.payments",
                                "OAuth2 registration payments-token was not found")
                        .build();

        assertThat(result.shouldFail(false)).isTrue();
        assertThat(result.shouldFail(true)).isTrue();
    }

    @Test
    void resultIsImmutable() {
        List<OAuth2ConfigurationValidationIssue> issues = new ArrayList<>();
        issues.add(OAuth2ConfigurationValidationIssue.error("clients.payments", "is invalid"));

        OAuth2ConfigurationValidationResult result = OAuth2ConfigurationValidationResult.of(issues);
        issues.add(
                OAuth2ConfigurationValidationIssue.warning(
                        "authentication.jwt-bearer.extra", "is not used"));

        assertThat(result.issues()).hasSize(1);
        assertThatThrownBy(
                        () ->
                                result.issues()
                                        .add(OAuth2ConfigurationValidationIssue.warning("x", "y")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void mergePreservesIssueOrder() {
        OAuth2ConfigurationValidationResult first =
                OAuth2ConfigurationValidationResult.builder()
                        .error("clients.payments", "is invalid")
                        .build();
        OAuth2ConfigurationValidationResult second =
                OAuth2ConfigurationValidationResult.builder()
                        .warning("authentication.jwt-bearer.extra", "is not used")
                        .build();

        OAuth2ConfigurationValidationResult merged = first.merge(second);

        assertThat(merged.issues())
                .containsExactlyElementsOf(
                        List.of(
                                OAuth2ConfigurationValidationIssue.error(
                                        "clients.payments", "is invalid"),
                                OAuth2ConfigurationValidationIssue.warning(
                                        "authentication.jwt-bearer.extra", "is not used")));
    }

    @Test
    void issueRequiresSeverityPathAndMessage() {
        assertThatThrownBy(
                        () ->
                                new OAuth2ConfigurationValidationIssue(
                                        null, "clients.payments", "is invalid"))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> OAuth2ConfigurationValidationIssue.error(" ", "is invalid"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("path must not be blank");

        assertThatThrownBy(
                        () -> OAuth2ConfigurationValidationIssue.warning("clients.payments", " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("message must not be blank");
    }

    @Test
    void issueFormatUsesSeverityYamlPathMessageAndSuggestedFix() {
        OAuth2ConfigurationValidationIssue issue =
                OAuth2ConfigurationValidationIssue.error(
                        "clients.payments.token-request-id",
                        "references missing OAuth2 registration payments-token");

        assertThat(issue.format())
                .isEqualTo(
                        "ERROR clients.payments.token-request-id "
                                + "- references missing OAuth2 registration payments-token "
                                + "Fix: Set smbtech.rest-clients.clients.payments.token-request-id "
                                + "to an existing Spring OAuth2 registration id with the expected grant type.");
    }

    @Test
    void issueFormatOmitsSuggestedFixWhenNoSuggestionCanBeInferred() {
        OAuth2ConfigurationValidationIssue issue =
                OAuth2ConfigurationValidationIssue.error("clients.payments", "is invalid");

        assertThat(issue.suggestedFix()).isBlank();
        assertThat(issue.format()).isEqualTo("ERROR clients.payments - is invalid");
    }

    @Test
    void suggestedFixesCoverKnownOAuth2ConfigurationFamilies() {
        assertThat(suggestedFix("clients.payments.token-request-id", "is required"))
                .isEqualTo(
                        "Set smbtech.rest-clients.clients.payments.token-request-id "
                                + "to an existing Spring OAuth2 registration id with the expected grant type.");
        assertThat(
                        suggestedFix(
                                "spring.security.oauth2.client.registration.payments-token.client-authentication-method",
                                "uses unsupported client authentication method none"))
                .isEqualTo(
                        "Update spring.security.oauth2.client.registration.payments-token.client-authentication-method "
                                + "to one of the supported methods for this REST client authentication type.");
        assertThat(
                        suggestedFix(
                                "spring.security.oauth2.client.registration.payments-token.client-secret",
                                "is required"))
                .isEqualTo(
                        "Set spring.security.oauth2.client.registration.payments-token.client-secret "
                                + "or switch the registration to an authentication method that does not require a secret.");
        assertThat(
                        suggestedFix(
                                "clients.secure-api.basic-authentication.password-ref",
                                "references missing credential secure-password"))
                .isEqualTo(
                        "Create smbtech.rest-clients.authentication.credentials.secure-password "
                                + "or update the reference to an existing credential id.");
        assertThat(
                        suggestedFix(
                                "authentication.key-stores.payments-signing-key.password-ref",
                                "references missing credential keystore-password"))
                .isEqualTo(
                        "Create smbtech.rest-clients.authentication.credentials.keystore-password "
                                + "or update the reference to an existing credential id.");
        assertThat(
                        suggestedFix(
                                "authentication.credentials.unused-secret",
                                "is configured but unused"))
                .isEqualTo(
                        "Remove smbtech.rest-clients.authentication.credentials.unused-secret "
                                + "or reference it from an enabled REST client or used key store.");
        assertThat(
                        suggestedFix(
                                "authentication.key-stores.unused-key-store",
                                "is configured but unused"))
                .isEqualTo(
                        "Remove smbtech.rest-clients.authentication.key-stores.unused-key-store "
                                + "or reference it from SSL, private_key_jwt, or JWT bearer signing configuration.");
        assertThat(suggestedFix("authentication.client-assertions.payments-token", "is required"))
                .isEqualTo(
                        "Add smbtech.rest-clients.authentication.client-assertions.payments-token.key-store-id "
                                + "for private_key_jwt, or remove the unused client assertion block.");
        assertThat(
                        suggestedFix(
                                "authentication.client-assertions.payments-token.key-store-id",
                                "is required"))
                .isEqualTo(
                        "Set smbtech.rest-clients.authentication.client-assertions.payments-token.key-store-id "
                                + "to a configured smbtech.rest-clients.authentication.key-stores.<id>.");
        assertThat(suggestedFix("authentication.jwt-bearer.payments-token", "is required"))
                .isEqualTo(
                        "Add smbtech.rest-clients.authentication.jwt-bearer.payments-token.key-store-id "
                                + "for JWT bearer signing, or remove the unused JWT bearer block.");
        assertThat(
                        suggestedFix(
                                "authentication.jwt-bearer.payments-token.key-store-id",
                                "is required"))
                .isEqualTo(
                        "Set smbtech.rest-clients.authentication.jwt-bearer.payments-token.key-store-id "
                                + "to a configured smbtech.rest-clients.authentication.key-stores.<id>.");
        assertThat(
                        suggestedFix(
                                "clients.payments.scopes",
                                "contains expected scopes not requested"))
                .isEqualTo(
                        "Add the missing scopes to spring.security.oauth2.client.registration.<id>.scope "
                                + "or lower the expected smbtech.rest-clients.clients.payments.scopes value.");
        assertThat(suggestedFix("validation.validate-key-store-content", "cannot be checked"))
                .isEqualTo(
                        "Keep validate-key-store-content disabled or provide the keystore infrastructure required "
                                + "for deep content checks.");
    }

    private String suggestedFix(String path, String message) {
        return OAuth2ConfigurationValidationIssue.error(path, message).suggestedFix();
    }
}
