package com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.OAuth2AuthorizationContext;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OAuth2AuthorizationContextAttributesMapperTest {

    private final OAuth2AuthorizationContextAttributesMapper mapper =
            new OAuth2AuthorizationContextAttributesMapper();

    @Test
    void mapsSpringSecurityDefaultsAndInternalJwtBearerCustomClaims() {
        Map<String, Object> customClaims = Map.of("customer_id", "17952397-3");
        OAuth2AuthorizeRequest request = OAuth2AuthorizeRequest
                .withClientRegistrationId("payments-api")
                .principal("spring-boot-service-framework")
                .attribute(OAuth2ParameterNames.SCOPE, "payment.read payment.write")
                .attribute(OAuth2AuthorizationAttributes.JWT_BEARER_CUSTOM_CLAIMS, customClaims)
                .build();

        Map<String, Object> attributes = mapper.apply(request);

        assertThat(attributes)
                .containsEntry(OAuth2AuthorizationAttributes.JWT_BEARER_CUSTOM_CLAIMS, customClaims);
        assertThat((String[]) attributes.get(OAuth2AuthorizationContext.REQUEST_SCOPE_ATTRIBUTE_NAME))
                .containsExactly("payment.read", "payment.write");
    }

    @Test
    void doesNotAddInternalJwtBearerAttributeWhenCustomClaimsAreMissing() {
        OAuth2AuthorizeRequest request = OAuth2AuthorizeRequest
                .withClientRegistrationId("payments-api")
                .principal("spring-boot-service-framework")
                .build();

        Map<String, Object> attributes = mapper.apply(request);

        assertThat(attributes).doesNotContainKey(OAuth2AuthorizationAttributes.JWT_BEARER_CUSTOM_CLAIMS);
    }
}
