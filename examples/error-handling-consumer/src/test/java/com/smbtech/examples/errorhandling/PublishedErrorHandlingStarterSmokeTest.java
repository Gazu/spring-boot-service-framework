package com.smbtech.examples.errorhandling;

import static org.hamcrest.Matchers.hasItems;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.smbtech.examples.errorhandling.domain.OrderErrors;
import com.smbtech.serviceframework.starter.errorhandling.api.security.SecurityErrorCatalog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class PublishedErrorHandlingStarterSmokeTest {

    @Autowired private WebApplicationContext applicationContext;

    @Autowired private FilterChainProxy springSecurityFilterChain;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc =
                MockMvcBuilders.webAppContextSetup(applicationContext)
                        .apply(springSecurity(springSecurityFilterChain))
                        .build();
    }

    @Test
    void returnsApplicationCatalogError() throws Exception {
        mockMvc.perform(get("/api/orders/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(OrderErrors.ORDER_NOT_FOUND.code()))
                .andExpect(jsonPath("$.message").value(OrderErrors.ORDER_NOT_FOUND.publicMessage()))
                .andExpect(jsonPath("$.severity").value("ERROR"))
                .andExpect(jsonPath("$.field_name").value(""));
    }

    @Test
    void returnsMultipleValidationViolationsInSnakeCase() throws Exception {
        mockMvc.perform(
                        post("/api/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.metadata.violations.length()").value(3))
                .andExpect(
                        jsonPath("$.metadata.violations[*].field_name")
                                .value(hasItems("customerId", "amount", "items")))
                .andExpect(jsonPath("$.metadata.violations[*].code").exists())
                .andExpect(jsonPath("$.fieldName").doesNotExist());
    }

    @Test
    void hidesDownstreamHeadersBodyAndCause() throws Exception {
        mockMvc.perform(get("/api/simulations/downstream"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("E_SERVICE_FRAMEWORK_HTTP_CLIENT_0503"))
                .andExpect(jsonPath("$.message").value("Downstream service request failed"))
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.not(
                                                org.hamcrest.Matchers.containsString(
                                                        "downstream-uri-secret"))))
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.not(
                                                org.hamcrest.Matchers.containsString(
                                                        "downstream-header-secret"))))
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.not(
                                                org.hamcrest.Matchers.containsString(
                                                        "downstream-body-secret"))))
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.not(
                                                org.hamcrest.Matchers.containsString(
                                                        "downstream-cause-secret"))));
    }

    @Test
    void hidesUnexpectedExceptionDetails() throws Exception {
        mockMvc.perform(get("/api/simulations/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("E_SERVICE_FRAMEWORK_INTERNAL_0001"))
                .andExpect(jsonPath("$.message").value("The request could not be completed"))
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.not(
                                                org.hamcrest.Matchers.containsString(
                                                        "internal-secret"))))
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.not(
                                                org.hamcrest.Matchers.containsString(
                                                        "OrderRepository"))));
    }

    @Test
    void returnsNotificationForUnauthenticatedRequest() throws Exception {
        mockMvc.perform(get("/api/secure/profile"))
                .andExpect(status().isUnauthorized())
                .andExpect(
                        jsonPath("$.code")
                                .value(SecurityErrorCatalog.AUTHENTICATION_REQUIRED.code()))
                .andExpect(jsonPath("$.field_name").value(""));
    }

    @Test
    @WithMockUser(username = "operator", roles = "USER")
    void returnsNotificationForForbiddenRequest() throws Exception {
        mockMvc.perform(get("/api/secure/admin"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(SecurityErrorCatalog.ACCESS_DENIED.code()))
                .andExpect(jsonPath("$.field_name").value(""));
    }
}
