package com.smbtech.serviceframework.httpclient.port.out;

import com.smbtech.serviceframework.httpclient.domain.AccessToken;

import java.util.Optional;

public interface TokenCache {

    Optional<AccessToken> find(String id);

    void put(String id, AccessToken token);

    void evict(String id);
}
