package com.smbtech.serviceframework.starter.restclient;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.smbtech.serviceframework.starter.restclient.api.AccessTokenClient;
import com.smbtech.serviceframework.starter.restclient.api.customizer.ApacheHttpClientBuilderCustomizer;
import com.smbtech.serviceframework.starter.restclient.api.oauth2.JwtBearerClaimsContributor;
import java.lang.reflect.Modifier;
import java.util.List;
import org.junit.jupiter.api.Test;

class RestClientStarterEncapsulationTest {

    @Test
    void keepsConsumerContractsPublic() {
        assertTrue(Modifier.isPublic(AccessTokenClient.class.getModifiers()));
        assertTrue(Modifier.isPublic(ApacheHttpClientBuilderCustomizer.class.getModifiers()));
        assertTrue(Modifier.isPublic(JwtBearerClaimsContributor.class.getModifiers()));
    }

    @Test
    void hidesTransportAndOAuth2Implementations() throws ClassNotFoundException {
        for (String className : internalImplementations()) {
            assertFalse(Modifier.isPublic(Class.forName(className).getModifiers()), className);
        }
    }

    private List<String> internalImplementations() {
        return List.of(
                "com.smbtech.serviceframework.starter.restclient.adapter.out.apache.ApacheHttpClientConfigurator",
                "com.smbtech.serviceframework.starter.restclient.adapter.out.apache.HttpClientConfigurator",
                "com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring.OAuth2ExtensionRegistry",
                "com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring.SpringOAuth2TokenResponseClientFactory",
                "com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring.SpringSecurityAuthorizedClientTokenClient",
                "com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring.SpringSecurityJwtBearerAssertionResolver",
                "com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring.Slf4jStructuredLoggerFactory",
                "com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.keystore.KeyStoreManager",
                "com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.keystore.KeyStoreRuntime",
                "com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.keystore.PrivateKeyLoader",
                "com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.keystore.SigningJwkFactory",
                "com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.keystore.SslContextFactory",
                "com.smbtech.serviceframework.starter.restclient.adapter.out.spring.ConfiguredRestClientFactory",
                "com.smbtech.serviceframework.starter.restclient.adapter.out.spring.HttpErrorResponseMapper",
                "com.smbtech.serviceframework.starter.restclient.adapter.out.spring.DefaultApiClientFactory",
                "com.smbtech.serviceframework.starter.restclient.adapter.out.spring.DefaultRestClientRegistry",
                "com.smbtech.serviceframework.starter.restclient.adapter.out.spring.AuditLogInterceptor",
                "com.smbtech.serviceframework.starter.restclient.adapter.out.spring.ResilienceStateRegistry",
                "com.smbtech.serviceframework.starter.restclient.adapter.out.spring.ThreadLocalRequestContextManager",
                "com.smbtech.serviceframework.starter.restclient.autoconfigure.DynamicRestClientFactory",
                "com.smbtech.serviceframework.starter.restclient.autoconfigure.DynamicRestClientRegistrationConfiguration",
                "com.smbtech.serviceframework.starter.restclient.autoconfigure.CredentialPropertiesMapper",
                "com.smbtech.serviceframework.starter.restclient.autoconfigure.CredentialResolver",
                "com.smbtech.serviceframework.starter.restclient.autoconfigure.KeyStorePropertiesMapper",
                "com.smbtech.serviceframework.starter.restclient.autoconfigure.PropertiesCredentialDefinitionSource",
                "com.smbtech.serviceframework.starter.restclient.autoconfigure.PropertiesCredentialProvider",
                "com.smbtech.serviceframework.starter.restclient.autoconfigure.PropertiesHttpClientDefinitionSource",
                "com.smbtech.serviceframework.starter.restclient.autoconfigure.PropertiesKeyStoreDefinitionSource",
                "com.smbtech.serviceframework.starter.restclient.autoconfigure.RestClientBeanRegistrar",
                "com.smbtech.serviceframework.starter.restclient.autoconfigure.RestClientPropertiesMapper");
    }
}
