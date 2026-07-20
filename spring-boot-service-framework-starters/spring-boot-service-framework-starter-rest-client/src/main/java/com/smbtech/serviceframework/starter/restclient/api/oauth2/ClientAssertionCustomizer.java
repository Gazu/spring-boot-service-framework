package com.smbtech.serviceframework.starter.restclient.api.oauth2;

/** Customizes a {@code private_key_jwt} client assertion before it is signed. */
@FunctionalInterface
public interface ClientAssertionCustomizer {

    /**
     * Returns a customized copy of the current client assertion context.
     *
     * @param context immutable client assertion customization context
     * @return customized client assertion context
     */
    ClientAssertionContext customize(ClientAssertionContext context);
}
