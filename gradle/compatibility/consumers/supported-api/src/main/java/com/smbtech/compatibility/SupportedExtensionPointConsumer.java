package com.smbtech.compatibility;

import com.smbtech.serviceframework.httpclient.port.out.CredentialDefinitionSource;
import com.smbtech.serviceframework.httpclient.port.out.CredentialProvider;
import com.smbtech.serviceframework.httpclient.port.out.HttpClientDefinitionSource;
import com.smbtech.serviceframework.httpclient.port.out.KeyStoreDefinitionSource;
import com.smbtech.serviceframework.starter.errorhandling.api.ErrorExposurePolicy;
import com.smbtech.serviceframework.starter.errorhandling.api.NotificationResponseFactory;
import com.smbtech.serviceframework.starter.errorhandling.api.NotificationResponseWriter;
import com.smbtech.serviceframework.starter.errorhandling.api.NotificationSerializer;
import com.smbtech.serviceframework.starter.restclient.api.RequestContextManager;

/** Compile-time consumer for the supported replaceable bean contracts. */
public record SupportedExtensionPointConsumer(
        NotificationResponseFactory notificationResponseFactory,
        NotificationSerializer notificationSerializer,
        NotificationResponseWriter notificationResponseWriter,
        ErrorExposurePolicy errorExposurePolicy,
        CredentialProvider credentialProvider,
        CredentialDefinitionSource credentialDefinitionSource,
        KeyStoreDefinitionSource keyStoreDefinitionSource,
        HttpClientDefinitionSource httpClientDefinitionSource,
        RequestContextManager requestContextManager) {}
