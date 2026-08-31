package com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring;

import com.smbtech.serviceframework.starter.restclient.api.RequestContextManager;
import com.smbtech.serviceframework.starter.restclient.api.customizer.RestClientAuthenticationConfigurer;
import com.smbtech.serviceframework.starter.restclient.autoconfigure.RestClientProperties;
import java.util.Objects;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;

@Configuration(proxyBeanMethods = false)
class OAuth2AuthenticationConfiguration {

    @Bean
    @ConditionalOnMissingBean(RestClientAuthenticationConfigurer.class)
    RestClientAuthenticationConfigurer oAuth2RestClientAuthenticationConfigurer(
            OAuth2AuthorizedClientManager authorizedClientManager,
            OAuth2AuthorizedClientService authorizedClientService,
            RequestContextManager requestContextManager,
            RestClientProperties properties,
            OAuth2ExtensionRegistry extensionRegistry) {
        return new OAuth2RestClientAuthenticationConfigurer(
                authorizedClientManager,
                authorizedClientService,
                requestContextManager,
                requestContextJwtBearerClaimsEnabled(properties),
                blockedJwtBearerClaims(properties),
                extensionRegistry);
    }

    private static boolean requestContextJwtBearerClaimsEnabled(RestClientProperties properties) {
        RestClientProperties.RequestContext requestContext = requestContext(properties);
        return requestContext.isEnabled() && requestContext.isJwtBearerClaims();
    }

    private static Set<String> blockedJwtBearerClaims(RestClientProperties properties) {
        return Set.copyOf(
                Objects.requireNonNullElse(
                        requestContext(properties).getBlockedJwtBearerClaims(), Set.of()));
    }

    private static RestClientProperties.RequestContext requestContext(
            RestClientProperties properties) {
        if (properties == null || properties.getRequestContext() == null) {
            return new RestClientProperties.RequestContext();
        }
        return properties.getRequestContext();
    }
}
