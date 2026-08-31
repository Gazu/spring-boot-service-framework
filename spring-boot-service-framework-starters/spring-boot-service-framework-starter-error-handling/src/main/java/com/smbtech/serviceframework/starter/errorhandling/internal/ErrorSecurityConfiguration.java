package com.smbtech.serviceframework.starter.errorhandling.internal;

import com.smbtech.serviceframework.commons.notification.Notification;
import com.smbtech.serviceframework.error.ErrorExposure;
import com.smbtech.serviceframework.error.ResolvedError;
import com.smbtech.serviceframework.starter.errorhandling.api.security.OAuth2SecurityChallengeWriter;
import com.smbtech.serviceframework.starter.errorhandling.api.security.OAuth2SecurityMetadataFactory;
import com.smbtech.serviceframework.starter.errorhandling.api.security.RequiredScopeResolver;
import com.smbtech.serviceframework.starter.errorhandling.api.security.SecurityAuthenticationFailureResolver;
import com.smbtech.serviceframework.starter.errorhandling.api.security.SecurityAuthorizationFailureResolver;
import com.smbtech.serviceframework.starter.errorhandling.api.security.SecurityErrorCatalog;
import com.smbtech.serviceframework.starter.errorhandling.api.security.SecurityFailureReason;
import com.smbtech.serviceframework.starter.errorhandling.api.security.SecurityFailureResolution;
import com.smbtech.serviceframework.starter.errorhandling.autoconfigure.ErrorHandlingProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

/** Registers package-local Spring Security adapters behind Spring Security contracts. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({AuthenticationEntryPoint.class, AccessDeniedHandler.class})
@ConditionalOnProperty(
        prefix = "smbtech.error-handling.security",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
class ErrorSecurityConfiguration {

    @Bean
    @ConditionalOnMissingBean
    SecurityAuthorizationFailureResolver securityAuthorizationFailureResolver() {
        return new DefaultSecurityAuthorizationFailureResolver();
    }

    @Bean
    @ConditionalOnMissingBean
    RequiredScopeResolver requiredScopeResolver() {
        return new DefaultRequiredScopeResolver();
    }

    @Bean
    @ConditionalOnMissingBean
    OAuth2SecurityMetadataFactory oauth2SecurityMetadataFactory(
            ErrorHandlingProperties properties) {
        ErrorHandlingProperties.OAuth2Metadata metadata =
                properties.getSecurity().getOauth2Metadata();
        return new DefaultOAuth2SecurityMetadataFactory(
                metadata.isEnabled(),
                metadata.isIncludeErrorDescription(),
                metadata.isIncludeErrorUri(),
                metadata.isIncludeRequiredScope());
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(
            name = "org.springframework.security.oauth2.server.resource.BearerTokenError")
    static class OAuth2AuthenticationResolutionConfiguration {

        @Bean
        @ConditionalOnMissingBean
        SecurityAuthenticationFailureResolver securityAuthenticationFailureResolver() {
            return new DefaultSecurityAuthenticationFailureResolver();
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnMissingClass(
            "org.springframework.security.oauth2.server.resource.BearerTokenError")
    static class GenericAuthenticationResolutionConfiguration {

        @Bean
        @ConditionalOnMissingBean
        SecurityAuthenticationFailureResolver securityAuthenticationFailureResolver() {
            return context -> {
                if (!(context.failure() instanceof AuthenticationException exception)) {
                    throw new IllegalArgumentException(
                            "context failure must be an AuthenticationException");
                }
                SecurityErrorCatalog definition = SecurityErrorCatalog.AUTHENTICATION_REQUIRED;
                return new SecurityFailureResolution(
                        new ResolvedError(
                                Notification.builder()
                                        .code(definition.code())
                                        .message(definition.publicMessage())
                                        .severity(definition.severity())
                                        .build(),
                                definition.category(),
                                ErrorExposure.PUBLIC,
                                diagnosticMessage(exception)),
                        SecurityFailureReason.AUTHENTICATION_REQUIRED);
            };
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(
            name = {
                "org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint",
                "org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler"
            })
    static class OAuth2ChallengeConfiguration {

        @Bean
        @ConditionalOnMissingBean
        OAuth2SecurityChallengeWriter oauth2SecurityChallengeWriter() {
            return new DefaultOAuth2SecurityChallengeWriter();
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnMissingClass(
            "org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint")
    static class GenericChallengeConfiguration {

        @Bean
        @ConditionalOnMissingBean
        OAuth2SecurityChallengeWriter oauth2SecurityChallengeWriter() {
            return (request, response, context, resolution) -> {};
        }
    }

    private static String diagnosticMessage(AuthenticationException exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank()
                ? exception.getClass().getName()
                : exception.getClass().getName() + ": " + message;
    }
}
