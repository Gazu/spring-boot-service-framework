package com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring;

import static org.assertj.core.api.Assertions.assertThat;

import com.smbtech.serviceframework.starter.restclient.adapter.out.context.ThreadLocalRequestContextManager;
import com.smbtech.serviceframework.starter.restclient.api.RequestContextScope;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.OAuth2AuthorizationContext;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;

class OAuth2AuthorizationContextAttributesMapperTest {

    private final OAuth2AuthorizationContextAttributesMapper mapper =
            new OAuth2AuthorizationContextAttributesMapper();

    @Test
    void mapsSpringSecurityDefaultsAndInternalJwtBearerCustomClaims() {
        Map<String, Object> customClaims = Map.of("customer_id", "17952397-3");
        OAuth2AuthorizeRequest request =
                OAuth2AuthorizeRequest.withClientRegistrationId("payments-api")
                        .principal("spring-boot-service-framework")
                        .attribute(OAuth2ParameterNames.SCOPE, "payment.read payment.write")
                        .attribute(
                                OAuth2AuthorizationAttributes.JWT_BEARER_CUSTOM_CLAIMS,
                                customClaims)
                        .build();

        Map<String, Object> attributes = mapper.apply(request);

        assertThat(attributes)
                .containsEntry(
                        OAuth2AuthorizationAttributes.JWT_BEARER_CUSTOM_CLAIMS, customClaims);
        assertThat(
                        (String[])
                                attributes.get(
                                        OAuth2AuthorizationContext.REQUEST_SCOPE_ATTRIBUTE_NAME))
                .containsExactly("payment.read", "payment.write");
    }

    @Test
    void doesNotAddInternalJwtBearerAttributeWhenCustomClaimsAreMissing() {
        OAuth2AuthorizeRequest request =
                OAuth2AuthorizeRequest.withClientRegistrationId("payments-api")
                        .principal("spring-boot-service-framework")
                        .build();

        Map<String, Object> attributes = mapper.apply(request);

        assertThat(attributes)
                .doesNotContainKey(OAuth2AuthorizationAttributes.JWT_BEARER_CUSTOM_CLAIMS);
    }

    @Test
    void mapsRequestContextJwtBearerClaimsAndLetsRequestClaimsOverrideThem() {
        ThreadLocalRequestContextManager requestContextManager =
                new ThreadLocalRequestContextManager();
        OAuth2AuthorizationContextAttributesMapper mapper =
                new OAuth2AuthorizationContextAttributesMapper(requestContextManager);
        OAuth2AuthorizeRequest request =
                OAuth2AuthorizeRequest.withClientRegistrationId("payments-api")
                        .principal("spring-boot-service-framework")
                        .attribute(
                                OAuth2AuthorizationAttributes.JWT_BEARER_CUSTOM_CLAIMS,
                                Map.of("channel", "mobile", "operation_id", "op-123"))
                        .build();

        Map<String, Object> attributes;
        try (RequestContextScope ignored =
                requestContextManager.open(
                        context ->
                                context.jwtBearerClaim("customer_id", "17952397-3")
                                        .jwtBearerClaim("channel", "web")
                                        .jwtBearerClaim("sub", "must-not-override-subject"))) {
            attributes = mapper.apply(request);
        }

        assertThat(attributes)
                .containsEntry(
                        OAuth2AuthorizationAttributes.JWT_BEARER_CUSTOM_CLAIMS,
                        Map.of(
                                "customer_id", "17952397-3",
                                "channel", "mobile",
                                "operation_id", "op-123"));
    }

    @Test
    void ignoresRequestContextJwtBearerClaimsWhenDisabled() {
        ThreadLocalRequestContextManager requestContextManager =
                new ThreadLocalRequestContextManager();
        OAuth2AuthorizationContextAttributesMapper mapper =
                new OAuth2AuthorizationContextAttributesMapper(requestContextManager, false);
        OAuth2AuthorizeRequest request =
                OAuth2AuthorizeRequest.withClientRegistrationId("payments-api")
                        .principal("spring-boot-service-framework")
                        .attribute(
                                OAuth2AuthorizationAttributes.JWT_BEARER_CUSTOM_CLAIMS,
                                Map.of("operation_id", "op-123"))
                        .build();

        Map<String, Object> attributes;
        try (RequestContextScope ignored =
                requestContextManager.openJwtBearerClaim("customer_id", "17952397-3")) {
            attributes = mapper.apply(request);
        }

        assertThat(attributes)
                .containsEntry(
                        OAuth2AuthorizationAttributes.JWT_BEARER_CUSTOM_CLAIMS,
                        Map.of("operation_id", "op-123"));
    }

    @Test
    void sanitizesRequestContextJwtBearerClaimsWithConfiguredBlockedClaims() {
        ThreadLocalRequestContextManager requestContextManager =
                new ThreadLocalRequestContextManager();
        OAuth2AuthorizationContextAttributesMapper mapper =
                new OAuth2AuthorizationContextAttributesMapper(
                        requestContextManager, true, java.util.Set.of("customer_id"));
        OAuth2AuthorizeRequest request =
                OAuth2AuthorizeRequest.withClientRegistrationId("payments-api")
                        .principal("spring-boot-service-framework")
                        .build();

        Map<String, Object> attributes;
        try (RequestContextScope ignored =
                requestContextManager.open(
                        context ->
                                context.jwtBearerClaim("customer_id", "17952397-3")
                                        .jwtBearerClaim("channel", "mobile"))) {
            attributes = mapper.apply(request);
        }

        assertThat(attributes)
                .containsEntry(
                        OAuth2AuthorizationAttributes.JWT_BEARER_CUSTOM_CLAIMS,
                        Map.of("channel", "mobile"));
    }
}
