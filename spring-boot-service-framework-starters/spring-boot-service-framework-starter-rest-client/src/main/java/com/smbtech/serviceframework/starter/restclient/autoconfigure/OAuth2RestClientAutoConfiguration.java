package com.smbtech.serviceframework.starter.restclient.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;

/** Activates Spring Security OAuth2 support when the OAuth2 client is present. */
@AutoConfiguration(
        after = RestClientAutoConfiguration.class,
        afterName = {
            "org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration",
            "org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientAutoConfiguration",
            "org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration"
        })
@ConditionalOnClass(OAuth2AuthorizedClientManager.class)
@Import(OAuth2RestClientConfigurationImportSelector.class)
public class OAuth2RestClientAutoConfiguration {

    /** Creates an OAuth2 REST client auto-configuration instance. */
    public OAuth2RestClientAutoConfiguration() {}
}
