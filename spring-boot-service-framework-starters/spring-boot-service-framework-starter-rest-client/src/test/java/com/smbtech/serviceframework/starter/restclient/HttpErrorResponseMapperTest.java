package com.smbtech.serviceframework.starter.restclient;

import com.smbtech.serviceframework.httpclient.domain.ApacheHttpClientPolicy;
import com.smbtech.serviceframework.httpclient.domain.AuditPolicy;
import com.smbtech.serviceframework.httpclient.domain.AuthenticationType;
import com.smbtech.serviceframework.httpclient.domain.BasicAuthentication;
import com.smbtech.serviceframework.httpclient.domain.ClientType;
import com.smbtech.serviceframework.httpclient.domain.ConnectionReusePolicy;
import com.smbtech.serviceframework.httpclient.domain.ErrorHandlingPolicy;
import com.smbtech.serviceframework.httpclient.domain.HttpClientDefinition;
import com.smbtech.serviceframework.httpclient.domain.HttpErrorResponse;
import com.smbtech.serviceframework.httpclient.domain.ObservabilityPolicy;
import com.smbtech.serviceframework.httpclient.domain.PoolingPolicy;
import com.smbtech.serviceframework.httpclient.domain.ResiliencePolicy;
import com.smbtech.serviceframework.httpclient.domain.TimeoutPolicy;
import com.smbtech.serviceframework.starter.restclient.adapter.out.error.HttpErrorResponseMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;

import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HttpErrorResponseMapperTest {

    private final HttpErrorResponseMapper mapper = new HttpErrorResponseMapper();
    private final MockClientHttpRequest request = new MockClientHttpRequest(
            HttpMethod.POST,
            URI.create("https://payments.example/v1/orders")
    );

    @Test
    void decodesBodyUsingCharsetDeclaredInContentType() throws Exception {
        Charset charset = StandardCharsets.ISO_8859_1;
        MockClientHttpResponse response = response(400, "ação inválida", charset);
        response.getHeaders().set(HttpHeaders.CONTENT_TYPE, "text/plain;charset=ISO-8859-1");

        HttpErrorResponse error = mapper.map(definition(true), request, response);

        assertThat(error.body()).isEqualTo("ação inválida");
        assertThat(error.contentType()).isEqualTo("text/plain;charset=ISO-8859-1");
        assertThat(error.charset()).isEqualTo("ISO-8859-1");
        assertThat(error.bodyTruncated()).isFalse();
    }

    @Test
    void keepsRawMalformedContentTypeAndFallsBackToUtf8() throws Exception {
        MockClientHttpResponse response = response(502, "{\"message\":\"ñ\"}", StandardCharsets.UTF_8);
        response.getHeaders().set(HttpHeaders.CONTENT_TYPE, "application/json; charset=NO_SUCH_CHARSET");

        HttpErrorResponse error = mapper.map(definition(true), request, response);

        assertThat(error.body()).isEqualTo("{\"message\":\"ñ\"}");
        assertThat(error.contentType()).isEqualTo("application/json; charset=NO_SUCH_CHARSET");
        assertThat(error.charset()).isEqualTo("UTF-8");
    }

    @Test
    void defaultsToUtf8WhenContentTypeIsMissing() throws Exception {
        MockClientHttpResponse response = response(500, "{\"message\":\"boom\"}", StandardCharsets.UTF_8);

        HttpErrorResponse error = mapper.map(definition(true), request, response);

        assertThat(error.body()).isEqualTo("{\"message\":\"boom\"}");
        assertThat(error.contentType()).isEmpty();
        assertThat(error.charset()).isEqualTo("UTF-8");
    }

    @Test
    void keepsCompleteBodyAndFlattensMultipleHeaderValues() throws Exception {
        MockClientHttpResponse response = response(400, "1234567890", StandardCharsets.UTF_8);
        response.getHeaders().add("X-Error-Code", "A");
        response.getHeaders().add("X-Error-Code", "B");

        HttpErrorResponse error = mapper.map(definition(true), request, response);

        assertThat(error.body()).isEqualTo("1234567890");
        assertThat(error.headers()).containsEntry("X-Error-Code", "A,B");
        assertThat(error.bodyTruncated()).isFalse();
    }

    @Test
    void omitsBodyWhenBodyCaptureIsDisabled() throws Exception {
        MockClientHttpResponse response = response(400, "1234567890", StandardCharsets.UTF_8);

        HttpErrorResponse error = mapper.map(definition(false), request, response);

        assertThat(error.body()).isEmpty();
        assertThat(error.charset()).isEqualTo("UTF-8");
        assertThat(error.bodyTruncated()).isFalse();
    }

    @Test
    void omitsHeadersWhenHeaderCaptureIsDisabled() throws Exception {
        MockClientHttpResponse response = response(400, "1234567890", StandardCharsets.UTF_8);
        response.getHeaders().add("X-Error-Code", "A");

        HttpErrorResponse error = mapper.map(definition(true, false), request, response);

        assertThat(error.body()).isEqualTo("1234567890");
        assertThat(error.headers()).isEmpty();
    }

    private MockClientHttpResponse response(int statusCode, String body, Charset charset) {
        return new MockClientHttpResponse(body.getBytes(charset), statusCode);
    }

    private HttpClientDefinition definition(boolean includeBody) {
        return definition(includeBody, true);
    }

    private HttpClientDefinition definition(boolean includeBody, boolean includeHeaders) {
        return new HttpClientDefinition(
                "payments",
                null,
                URI.create("https://payments.example"),
                ClientType.DEFAULT,
                AuthenticationType.NO_AUTH,
                new BasicAuthentication("", ""),
                "",
                "",
                new TimeoutPolicy(
                        Duration.ofSeconds(1),
                        Duration.ofSeconds(1),
                        Duration.ofSeconds(1)
                ),
                new PoolingPolicy(
                        ConnectionReusePolicy.DEFAULT,
                        Duration.ofSeconds(30),
                        100,
                        20,
                        false
                ),
                ApacheHttpClientPolicy.defaults(),
                new ErrorHandlingPolicy(true, includeBody, 1, includeHeaders, true, "E_SERVICE_FRAMEWORK_HTTP_CLIENT_"),
                ObservabilityPolicy.defaults(),
                ResiliencePolicy.disabled(),
                AuditPolicy.disabled(),
                Map.of()
        );
    }
}
