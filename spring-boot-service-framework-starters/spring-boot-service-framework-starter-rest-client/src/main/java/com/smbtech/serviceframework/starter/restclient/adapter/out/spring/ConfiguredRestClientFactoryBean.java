package com.smbtech.serviceframework.starter.restclient.adapter.out.spring;

import com.smbtech.serviceframework.starter.restclient.api.RestClientRegistry;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.web.client.RestClient;

public final class ConfiguredRestClientFactoryBean implements FactoryBean<RestClient>, BeanFactoryAware {

    private final String clientName;
    private BeanFactory beanFactory;

    public ConfiguredRestClientFactoryBean(String clientName) {
        this.clientName = clientName;
    }

    @Override
    public RestClient getObject() {
        return beanFactory.getBean(RestClientRegistry.class).get(clientName);
    }

    @Override
    public Class<?> getObjectType() {
        return RestClient.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }
}
