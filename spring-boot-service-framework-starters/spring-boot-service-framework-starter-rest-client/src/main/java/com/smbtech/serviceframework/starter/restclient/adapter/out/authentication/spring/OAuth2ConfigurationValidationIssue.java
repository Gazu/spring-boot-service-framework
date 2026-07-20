package com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring;

import java.util.Objects;

/**
 * Carries immutable OAuth2 configuration validation issue data.
 *
 * @param severity severity value
 * @param path path value
 * @param message message value
 */
public record OAuth2ConfigurationValidationIssue(
        OAuth2ConfigurationValidationSeverity severity, String path, String message) {

    /** Creates and validates the record components. */
    public OAuth2ConfigurationValidationIssue {
        severity = Objects.requireNonNull(severity, "severity must not be null");
        path = requireText(path, "path");
        message = requireText(message, "message");
    }

    /**
     * Performs the error operation.
     *
     * @param path path value
     * @param message message value
     * @return error result
     */
    public static OAuth2ConfigurationValidationIssue error(String path, String message) {
        return new OAuth2ConfigurationValidationIssue(
                OAuth2ConfigurationValidationSeverity.ERROR, path, message);
    }

    /**
     * Performs the warning operation.
     *
     * @param path path value
     * @param message message value
     * @return warning result
     */
    public static OAuth2ConfigurationValidationIssue warning(String path, String message) {
        return new OAuth2ConfigurationValidationIssue(
                OAuth2ConfigurationValidationSeverity.WARNING, path, message);
    }

    /**
     * Reports whether error.
     *
     * @return is error result
     */
    public boolean isError() {
        return severity == OAuth2ConfigurationValidationSeverity.ERROR;
    }

    /**
     * Reports whether warning.
     *
     * @return is warning result
     */
    public boolean isWarning() {
        return severity == OAuth2ConfigurationValidationSeverity.WARNING;
    }

    /**
     * Performs the format operation.
     *
     * @return format result
     */
    public String format() {
        StringBuilder formatted =
                new StringBuilder()
                        .append(severity)
                        .append(" ")
                        .append(path)
                        .append(" - ")
                        .append(message);
        String suggestion = suggestedFix();
        if (!suggestion.isBlank()) {
            formatted.append(" Fix: ").append(suggestion);
        }
        return formatted.toString();
    }

    /**
     * Performs the suggested fix operation.
     *
     * @return suggested fix result
     */
    public String suggestedFix() {
        if (path.endsWith(".token-request-id")) {
            return "Set smbtech.rest-clients."
                    + path
                    + " to an existing Spring OAuth2 registration id with the expected grant type.";
        }
        if (path.endsWith(".client-authentication-method")) {
            return "Update "
                    + springPath(path)
                    + " to one of the supported methods for this REST client authentication type.";
        }
        if (path.endsWith(".client-secret")) {
            return "Set "
                    + springPath(path)
                    + " or switch the registration to an authentication method that does not require a secret.";
        }
        if (path.contains(".basic-authentication.") && path.endsWith("-ref")) {
            return "Create smbtech.rest-clients.authentication.credentials."
                    + referencedId()
                    + " or update the reference to an existing credential id.";
        }
        if (path.startsWith("authentication.credentials.")) {
            return "Remove smbtech.rest-clients."
                    + path
                    + " or reference it from an enabled REST client or used key store.";
        }
        if (path.startsWith("authentication.key-stores.") && path.endsWith("-ref")) {
            return "Create smbtech.rest-clients.authentication.credentials."
                    + referencedId()
                    + " or update the reference to an existing credential id.";
        }
        if (path.startsWith("authentication.key-stores.")) {
            return "Remove smbtech.rest-clients."
                    + path
                    + " or reference it from SSL, private_key_jwt, or JWT bearer signing configuration.";
        }
        if (path.startsWith("authentication.client-assertions.")
                && path.endsWith(".key-store-id")) {
            return "Set smbtech.rest-clients."
                    + path
                    + " to a configured smbtech.rest-clients.authentication.key-stores.<id>.";
        }
        if (path.startsWith("authentication.client-assertions.")) {
            return "Add smbtech.rest-clients."
                    + path
                    + ".key-store-id for private_key_jwt, or remove the unused client assertion block.";
        }
        if (path.startsWith("authentication.jwt-bearer.") && path.endsWith(".key-store-id")) {
            return "Set smbtech.rest-clients."
                    + path
                    + " to a configured smbtech.rest-clients.authentication.key-stores.<id>.";
        }
        if (path.startsWith("authentication.jwt-bearer.")) {
            return "Add smbtech.rest-clients."
                    + path
                    + ".key-store-id for JWT bearer signing, or remove the unused JWT bearer block.";
        }
        if (path.endsWith(".scopes")) {
            return "Add the missing scopes to spring.security.oauth2.client.registration.<id>.scope "
                    + "or lower the expected smbtech.rest-clients."
                    + path
                    + " value.";
        }
        if ("validation.validate-key-store-content".equals(path)) {
            return "Keep validate-key-store-content disabled or provide the keystore infrastructure required "
                    + "for deep content checks.";
        }
        return "";
    }

    private String referencedId() {
        int index = message.lastIndexOf(' ');
        if (index < 0 || index == message.length() - 1) {
            return "<id>";
        }
        return message.substring(index + 1);
    }

    private String springPath(String value) {
        return value.startsWith("spring.") ? value : "spring." + value;
    }

    private static String requireText(String value, String name) {
        String text = Objects.requireNonNull(value, name + " must not be null").trim();
        if (text.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return text;
    }
}
