package com.smbtech.serviceframework.httpclient.port.out;

import java.util.Optional;

public interface CredentialProvider {

    Optional<String> findSecret(String key);
}
