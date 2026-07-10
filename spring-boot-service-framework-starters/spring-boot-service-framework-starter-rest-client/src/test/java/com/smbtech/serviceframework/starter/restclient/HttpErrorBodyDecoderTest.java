package com.smbtech.serviceframework.starter.restclient;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smbtech.serviceframework.httpclient.domain.HttpErrorCategory;
import com.smbtech.serviceframework.httpclient.domain.HttpErrorResponse;
import com.smbtech.serviceframework.httpclient.exception.HttpClientResponseException;
import com.smbtech.serviceframework.starter.restclient.api.HttpErrorBodyDecoder;
import com.smbtech.serviceframework.starter.restclient.api.HttpErrorBodyDecodingException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HttpErrorBodyDecoderTest {

    private final HttpErrorBodyDecoder decoder = new HttpErrorBodyDecoder(new ObjectMapper().findAndRegisterModules());

    @Test
    void decodesCompleteExceptionBodyIntoClass() {
        String fullBody = "{\"code\":\"DOWNSTREAM_ERROR\",\"message\":\"1234567890-full-body\"}";
        HttpClientResponseException exception = new HttpClientResponseException(error(fullBody));

        ErrorPayload payload = decoder.decode(exception, ErrorPayload.class);

        assertThat(payload.code()).isEqualTo("DOWNSTREAM_ERROR");
        assertThat(payload.message()).isEqualTo("1234567890-full-body");
        assertThat(exception.responseBody()).isEqualTo(fullBody);
    }

    @Test
    void decodesCompleteExceptionBodyIntoTypeReference() {
        HttpClientResponseException exception = new HttpClientResponseException(error("""
                {"errors":[{"code":"A"},{"code":"B"}]}
                """));

        Map<String, List<Map<String, String>>> payload = decoder.decode(
                exception,
                new TypeReference<>() {
                }
        );

        assertThat(payload.get("errors"))
                .extracting(error -> error.get("code"))
                .containsExactly("A", "B");
    }

    @Test
    void returnsEmptyWhenBodyIsNotPresent() {
        HttpClientResponseException exception = new HttpClientResponseException(error(""));

        assertThat(decoder.decodeIfPresent(exception, ErrorPayload.class)).isEmpty();
    }

    @Test
    void throwsSafeExceptionWhenBodyIsMissingAndDecodeIsRequired() {
        HttpClientResponseException exception = new HttpClientResponseException(error(""));

        assertThatThrownBy(() -> decoder.decode(exception, ErrorPayload.class))
                .isInstanceOfSatisfying(HttpErrorBodyDecodingException.class, decodingException -> {
                    assertThat(decodingException.error()).isSameAs(exception.error());
                    assertThat(decodingException.getMessage()).contains("status=400");
                });
    }

    @Test
    void throwsSafeExceptionWithoutLeakingInvalidBodyInMessage() {
        HttpClientResponseException exception = new HttpClientResponseException(error("""
                {"secret":"should-not-appear"
                """));

        assertThatThrownBy(() -> decoder.decode(exception, ErrorPayload.class))
                .isInstanceOfSatisfying(HttpErrorBodyDecodingException.class, decodingException -> {
                    assertThat(decodingException.error()).isSameAs(exception.error());
                    assertThat(decodingException.getMessage()).contains("target=");
                    assertThat(decodingException.getMessage()).doesNotContain("should-not-appear");
                });
    }

    private HttpErrorResponse error(String body) {
        return new HttpErrorResponse(
                "payments",
                "POST",
                "https://payments.example/v1/orders",
                400,
                "Bad Request",
                HttpErrorCategory.CLIENT_ERROR,
                Map.of("Content-Type", "application/json"),
                body,
                "application/json",
                "UTF-8",
                false
        );
    }

    private record ErrorPayload(String code, String message) {
    }
}
