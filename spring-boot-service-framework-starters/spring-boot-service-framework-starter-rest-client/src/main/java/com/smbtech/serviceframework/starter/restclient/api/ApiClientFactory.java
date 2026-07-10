package com.smbtech.serviceframework.starter.restclient.api;

public interface ApiClientFactory {

    <T> T create(String clientName, Class<T> apiType);

    <T> T create(Class<T> apiType);
}
