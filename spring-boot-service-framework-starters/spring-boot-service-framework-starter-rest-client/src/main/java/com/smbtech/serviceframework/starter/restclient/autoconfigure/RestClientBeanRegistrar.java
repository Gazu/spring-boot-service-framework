package com.smbtech.serviceframework.starter.restclient.autoconfigure;

import com.smbtech.serviceframework.httpclient.domain.HttpClientDefinition;
import com.smbtech.serviceframework.starter.restclient.api.RestClientRegistry;
import java.util.Map;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

final class RestClientBeanRegistrar
        implements BeanDefinitionRegistryPostProcessor, EnvironmentAware {
    RestClientBeanRegistrar() {}

    private Environment environment;

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry)
            throws BeansException {
        RestClientProperties properties =
                Binder.get(environment)
                        .bind("smbtech.rest-clients", Bindable.of(RestClientProperties.class))
                        .orElseGet(RestClientProperties::new);

        Map<String, HttpClientDefinition> definitions =
                new RestClientPropertiesMapper().map(properties);
        definitions.forEach((name, definition) -> registerRestClient(registry, name, definition));
    }

    private void registerRestClient(
            BeanDefinitionRegistry registry, String name, HttpClientDefinition definition) {
        if (registry.containsBeanDefinition(definition.beanName())) {
            return;
        }
        RootBeanDefinition beanDefinition = new RootBeanDefinition(DynamicRestClientFactory.class);
        beanDefinition.setFactoryMethodName("create");
        beanDefinition
                .getConstructorArgumentValues()
                .addIndexedArgumentValue(0, new RuntimeBeanReference(RestClientRegistry.class));
        beanDefinition.getConstructorArgumentValues().addIndexedArgumentValue(1, name);
        beanDefinition.setLazyInit(true);
        beanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
        registry.registerBeanDefinition(definition.beanName(), beanDefinition);
    }

    @Override
    public void postProcessBeanFactory(
            org.springframework.beans.factory.config.ConfigurableListableBeanFactory beanFactory) {}

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }
}
