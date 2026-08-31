package com.smbtech.serviceframework.starter.restclient.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;

/** Connects configured REST clients to the available Spring Security OAuth2 infrastructure. */
@AutoConfiguration(after = OAuth2RestClientAutoConfiguration.class)
@ConditionalOnClass(OAuth2AuthorizedClientManager.class)
@ConditionalOnBean({OAuth2AuthorizedClientManager.class, OAuth2AuthorizedClientService.class})
@Import(OAuth2RestClientAuthenticationConfigurationImportSelector.class)
public class OAuth2RestClientAuthenticationAutoConfiguration {

    /** Creates an OAuth2 REST client authentication auto-configuration instance. */
    public OAuth2RestClientAuthenticationAutoConfiguration() {}
}
