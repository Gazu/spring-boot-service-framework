package com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring;

import com.smbtech.serviceframework.logging.domain.EventType;
import com.smbtech.serviceframework.logging.port.in.StructuredLogger;
import com.smbtech.serviceframework.starter.restclient.autoconfigure.RestClientProperties;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContextException;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

/** Provides OAuth2 rest client configuration validation runner behavior. */
public final class OAuth2RestClientConfigurationValidationRunner
        implements SmartInitializingSingleton {

    private static final EventType VALIDATION_EVENT =
            EventType.named("OAUTH2_CONFIGURATION_VALIDATION");

    private final RestClientProperties properties;
    private final OAuth2RestClientConfigurationValidator validator;
    private final ObjectProvider<ClientRegistrationRepository> clientRegistrationRepository;
    private final StructuredLogger logger;

    /**
     * Creates a OAuth2 rest client configuration validation runner instance.
     *
     * @param properties properties value
     * @param validator validator value
     * @param clientRegistrationRepository client registration repository value
     * @param logger logger value
     */
    public OAuth2RestClientConfigurationValidationRunner(
            RestClientProperties properties,
            OAuth2RestClientConfigurationValidator validator,
            ObjectProvider<ClientRegistrationRepository> clientRegistrationRepository,
            StructuredLogger logger) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.validator = Objects.requireNonNull(validator, "validator must not be null");
        this.clientRegistrationRepository =
                Objects.requireNonNull(
                        clientRegistrationRepository,
                        "clientRegistrationRepository must not be null");
        this.logger = Objects.requireNonNull(logger, "logger must not be null");
    }

    @Override
    public void afterSingletonsInstantiated() {
        OAuth2ConfigurationValidationResult result =
                validator.validate(properties, clientRegistrationRepository.getIfAvailable());
        result.warnings().forEach(this::logWarning);

        RestClientProperties.Validation validation = validationProperties();
        if (result.shouldFail(validation.isFailOnWarnings())) {
            throw new ApplicationContextException(formatFailure(result));
        }
    }

    private RestClientProperties.Validation validationProperties() {
        return properties.getValidation() == null
                ? new RestClientProperties.Validation()
                : properties.getValidation();
    }

    private void logWarning(OAuth2ConfigurationValidationIssue issue) {
        logger.warn(
                event ->
                        event.type(VALIDATION_EVENT)
                                .message("OAuth2 REST client configuration warning")
                                .with("severity", issue.severity().name())
                                .with("path", issue.path())
                                .with("message", issue.message())
                                .with("suggestedFix", issue.suggestedFix())
                                .tag("oauth2")
                                .tag("configuration"));
    }

    private String formatFailure(OAuth2ConfigurationValidationResult result) {
        StringBuilder message =
                new StringBuilder(
                        "Invalid SMBTech REST client OAuth2 configuration. Found "
                                + result.errors().size()
                                + " error(s) and "
                                + result.warnings().size()
                                + " warning(s).");
        appendSection(message, "Errors", result.errors());
        appendSection(message, "Warnings", result.warnings());
        message.append(System.lineSeparator())
                .append(System.lineSeparator())
                .append("Review the YAML paths above under smbtech.rest-clients or ")
                .append("spring.security.oauth2.client. To allow startup with warnings, keep ")
                .append("smbtech.rest-clients.validation.fail-on-warnings=false.");
        return message.toString();
    }

    private void appendSection(
            StringBuilder message, String title, List<OAuth2ConfigurationValidationIssue> issues) {
        if (issues.isEmpty()) {
            return;
        }

        message.append(System.lineSeparator())
                .append(System.lineSeparator())
                .append(title)
                .append(':');
        issues.forEach(
                issue ->
                        message.append(System.lineSeparator()).append("- ").append(issue.format()));
    }
}
