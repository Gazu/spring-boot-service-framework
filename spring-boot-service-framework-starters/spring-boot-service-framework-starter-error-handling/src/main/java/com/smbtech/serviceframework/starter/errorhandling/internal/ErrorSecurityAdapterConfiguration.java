package com.smbtech.serviceframework.starter.errorhandling.internal;

import com.smbtech.serviceframework.starter.errorhandling.api.NotificationResponseWriter;
import com.smbtech.serviceframework.starter.errorhandling.api.security.OAuth2SecurityChallengeWriter;
import com.smbtech.serviceframework.starter.errorhandling.api.security.OAuth2SecurityMetadataFactory;
import com.smbtech.serviceframework.starter.errorhandling.api.security.RequiredScopeResolver;
import com.smbtech.serviceframework.starter.errorhandling.api.security.SecurityAuthenticationFailureResolver;
import com.smbtech.serviceframework.starter.errorhandling.api.security.SecurityAuthorizationFailureResolver;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

/** Connects Spring Security contracts to the package-local error response pipeline. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({AuthenticationEntryPoint.class, AccessDeniedHandler.class})
@ConditionalOnProperty(
        prefix = "smbtech.error-handling.security",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
class ErrorSecurityAdapterConfiguration {

    @Bean
    @ConditionalOnMissingBean(AuthenticationEntryPoint.class)
    AuthenticationEntryPoint securityAuthenticationEntryPoint(
            NotificationResponseWriter responseWriter,
            ErrorResponsePipeline responsePipeline,
            SecurityAuthenticationFailureResolver failureResolver,
            OAuth2SecurityMetadataFactory metadataFactory,
            OAuth2SecurityChallengeWriter challengeWriter) {
        return new SecurityAuthenticationEntryPoint(
                responseWriter,
                responsePipeline,
                failureResolver,
                metadataFactory,
                challengeWriter);
    }

    @Bean
    @ConditionalOnMissingBean(AccessDeniedHandler.class)
    AccessDeniedHandler securityAccessDeniedHandler(
            NotificationResponseWriter responseWriter,
            ErrorResponsePipeline responsePipeline,
            SecurityAuthorizationFailureResolver failureResolver,
            RequiredScopeResolver requiredScopeResolver,
            OAuth2SecurityMetadataFactory metadataFactory,
            OAuth2SecurityChallengeWriter challengeWriter) {
        return new SecurityAccessDeniedHandler(
                responseWriter,
                responsePipeline,
                failureResolver,
                requiredScopeResolver,
                metadataFactory,
                challengeWriter);
    }
}
