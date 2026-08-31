package com.smbtech.serviceframework.starter.mock.adapter.out.restclient;

import com.smbtech.serviceframework.mock.port.in.MockResponder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(ClientHttpRequestInterceptor.class)
class RestClientMockConfiguration {

    @Bean(name = "mockRestClientInterceptor")
    @ConditionalOnMissingBean(name = "mockRestClientInterceptor")
    ClientHttpRequestInterceptor mockRestClientInterceptor(MockResponder mockResponder) {
        return new MockRestClientInterceptor(mockResponder, new MockRestClientRequestMapper());
    }
}
