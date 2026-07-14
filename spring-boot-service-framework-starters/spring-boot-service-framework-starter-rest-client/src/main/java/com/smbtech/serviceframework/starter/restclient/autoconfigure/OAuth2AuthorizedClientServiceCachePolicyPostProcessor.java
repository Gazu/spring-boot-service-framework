package com.smbtech.serviceframework.starter.restclient.autoconfigure;

import com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring.GrantAwareOAuth2AuthorizedClientService;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

final class OAuth2AuthorizedClientServiceCachePolicyPostProcessor implements BeanPostProcessor {

    private final ObjectProvider<ClientRegistrationRepository> clientRegistrationRepository;
    private final ObjectProvider<RestClientProperties> properties;

    OAuth2AuthorizedClientServiceCachePolicyPostProcessor(
            ObjectProvider<ClientRegistrationRepository> clientRegistrationRepository,
            ObjectProvider<RestClientProperties> properties
    ) {
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.properties = properties;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (!(bean instanceof OAuth2AuthorizedClientService authorizedClientService)
                || bean instanceof GrantAwareOAuth2AuthorizedClientService) {
            return bean;
        }
        ClientRegistrationRepository registrationRepository = clientRegistrationRepository.getIfAvailable();
        if (registrationRepository == null) {
            return bean;
        }
        return new GrantAwareOAuth2AuthorizedClientService(
                registrationRepository,
                authorizedClientService,
                properties.getIfAvailable(RestClientProperties::new)
        );
    }
}
