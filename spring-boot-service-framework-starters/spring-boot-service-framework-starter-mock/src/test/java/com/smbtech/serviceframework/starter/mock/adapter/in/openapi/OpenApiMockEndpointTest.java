package com.smbtech.serviceframework.starter.mock.adapter.in.openapi;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.request.ServletWebRequest;

class OpenApiMockEndpointTest {

    @Test
    void ignoresStatusHeaderWhenOverrideIsDisabled() {
        OpenApiMockEndpoint endpoint = new OpenApiMockEndpoint(operation(), "");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Mock-Status", "404");

        assertThat(endpoint.handle(new ServletWebRequest(request)).getStatusCode().value())
                .isEqualTo(200);
    }

    @Test
    void acceptsOnlyDeclaredStatusWhenOverrideIsEnabled() {
        OpenApiMockEndpoint endpoint = new OpenApiMockEndpoint(operation(), "X-Mock-Status");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Mock-Status", "404");

        assertThat(endpoint.handle(new ServletWebRequest(request)).getStatusCode().value())
                .isEqualTo(404);
    }

    private static OpenApiMockOperation operation() {
        return new OpenApiMockOperation(
                "getPet",
                RequestMethod.GET,
                "/pets/{id}",
                200,
                Map.of(
                        200,
                                new OpenApiMockResponse(
                                        200, "application/json", Map.of(), new byte[0]),
                        404,
                                new OpenApiMockResponse(
                                        404, "application/problem+json", Map.of(), new byte[0])),
                Duration.ZERO);
    }
}
