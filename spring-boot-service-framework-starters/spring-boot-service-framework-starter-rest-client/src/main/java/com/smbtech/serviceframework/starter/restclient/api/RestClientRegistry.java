package com.smbtech.serviceframework.starter.restclient.api;

import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.Set;

public interface RestClientRegistry {

    RestClient get(String name);

    Set<String> names();

    Map<String, RestClient> all();
}
