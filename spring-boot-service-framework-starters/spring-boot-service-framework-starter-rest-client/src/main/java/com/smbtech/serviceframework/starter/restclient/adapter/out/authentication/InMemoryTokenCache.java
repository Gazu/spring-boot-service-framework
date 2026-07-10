package com.smbtech.serviceframework.starter.restclient.adapter.out.authentication;

import com.smbtech.serviceframework.httpclient.domain.AccessToken;
import com.smbtech.serviceframework.httpclient.port.out.TokenCache;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class InMemoryTokenCache implements TokenCache {

    private final ConcurrentMap<String, AccessToken> tokens = new ConcurrentHashMap<>();

    @Override
    public Optional<AccessToken> find(String id) {
        return Optional.ofNullable(tokens.get(id));
    }

    @Override
    public void put(String id, AccessToken token) {
        tokens.put(id, token);
    }

    @Override
    public void evict(String id) {
        tokens.remove(id);
    }
}
