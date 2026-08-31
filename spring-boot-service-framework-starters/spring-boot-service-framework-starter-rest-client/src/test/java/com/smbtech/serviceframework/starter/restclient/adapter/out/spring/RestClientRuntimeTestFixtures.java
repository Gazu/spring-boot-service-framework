package com.smbtech.serviceframework.starter.restclient.adapter.out.spring;

import com.smbtech.serviceframework.starter.restclient.api.RequestContextManager;

public final class RestClientRuntimeTestFixtures {

    private RestClientRuntimeTestFixtures() {}

    public static RequestContextManager requestContextManager() {
        return new ThreadLocalRequestContextManager();
    }
}
