package com.smbtech.serviceframework.starter.restclient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smbtech.serviceframework.httpclient.domain.HttpErrorCategory;
import com.smbtech.serviceframework.httpclient.domain.HttpErrorResponse;
import com.smbtech.serviceframework.httpclient.exception.HttpClientResponseException;
import com.smbtech.serviceframework.httpclient.port.out.HttpErrorResponseBodyReader;
import com.smbtech.serviceframework.starter.restclient.api.HttpErrorBodyDecoder;
import com.smbtech.serviceframework.starter.restclient.api.HttpErrorBodyDecodingException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class HttpErrorBodyDecoderTest {

    private final HttpErrorBodyDecoder decoder =
            new HttpErrorBodyDecoder(new ObjectMapper().findAndRegisterModules());

    @Test
    void implementsCoreBodyReaderContract() {
        assertThat(decoder).isInstanceOf(HttpErrorResponseBodyReader.class);
    }

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
        HttpClientResponseException exception =
                new HttpClientResponseException(
                        error(
                                """
                {"errors":[{"code":"A"},{"code":"B"}]}
                """));

        Map<String, List<Map<String, String>>> payload =
                decoder.decode(exception, new TypeReference<>() {});

        assertThat(payload.get("errors"))
                .extracting(error -> error.get("code"))
                .containsExactly("A", "B");
    }

    @Test
    void readsErrorBodyIntoClassThroughCoreContract() {
        HttpErrorResponse error = error("{\"code\":\"DOWNSTREAM_ERROR\",\"message\":\"contract\"}");

        ErrorPayload payload = decoder.read(error, ErrorPayload.class);

        assertThat(payload.code()).isEqualTo("DOWNSTREAM_ERROR");
        assertThat(payload.message()).isEqualTo("contract");
    }

    @Test
    void readsErrorBodyIntoGenericTypeThroughCoreContract() {
        HttpErrorResponse error =
                error(
                        """
                {"errors":[{"code":"A"},{"code":"B"}]}
                """);
        Type type = new TypeReference<Map<String, List<Map<String, String>>>>() {}.getType();

        Map<String, List<Map<String, String>>> payload = decoder.read(error, type);

        assertThat(payload.get("errors"))
                .extracting(item -> item.get("code"))
                .containsExactly("A", "B");
    }

    @Test
    void decodesErrorBodyIntoGenericType() {
        HttpErrorResponse error =
                error(
                        """
                {"errors":[{"code":"A"},{"code":"B"}]}
                """);
        Type type = new TypeReference<Map<String, List<Map<String, String>>>>() {}.getType();

        Map<String, List<Map<String, String>>> payload = decoder.decode(error, type);

        assertThat(payload.get("errors"))
                .extracting(item -> item.get("code"))
                .containsExactly("A", "B");
    }

    @Test
    void returnsEmptyWhenBodyIsNotPresent() {
        HttpClientResponseException exception = new HttpClientResponseException(error(""));

        assertThat(decoder.decodeIfPresent(exception, ErrorPayload.class)).isEmpty();
    }

    @Test
    void returnsEmptyForTypeReferenceWhenBodyIsNotPresent() {
        HttpClientResponseException exception = new HttpClientResponseException(error(""));

        assertThat(decoder.decodeIfPresent(exception, new TypeReference<Map<String, Object>>() {}))
                .isEmpty();
    }

    @Test
    void throwsSafeExceptionWhenBodyIsMissingAndDecodeIsRequired() {
        HttpClientResponseException exception = new HttpClientResponseException(error(""));

        assertThatThrownBy(() -> decoder.decode(exception, ErrorPayload.class))
                .isInstanceOfSatisfying(
                        HttpErrorBodyDecodingException.class,
                        decodingException -> {
                            assertThat(decodingException.error()).isSameAs(exception.error());
                            assertThat(decodingException.getMessage()).contains("status=400");
                        });
    }

    @Test
    void throwsSafeExceptionWhenBodyIsMissingAndReadIsRequiredThroughCoreContract() {
        HttpErrorResponse error = error("");

        assertThatThrownBy(() -> decoder.read(error, ErrorPayload.class))
                .isInstanceOfSatisfying(
                        HttpErrorBodyDecodingException.class,
                        decodingException -> {
                            assertThat(decodingException.error()).isSameAs(error);
                            assertThat(decodingException.getMessage())
                                    .contains("HTTP error response does not contain a body");
                        });
    }

    @Test
    void throwsSafeExceptionWithoutLeakingInvalidBodyInMessage() {
        HttpClientResponseException exception =
                new HttpClientResponseException(
                        error(
                                """
                {"secret":"should-not-appear"
                """));

        assertThatThrownBy(() -> decoder.decode(exception, ErrorPayload.class))
                .isInstanceOfSatisfying(
                        HttpErrorBodyDecodingException.class,
                        decodingException -> {
                            assertThat(decodingException.error()).isSameAs(exception.error());
                            assertThat(decodingException.getMessage()).contains("target=");
                            assertThat(decodingException.getMessage())
                                    .doesNotContain("should-not-appear");
                        });
    }

    @Test
    void throwsSafeExceptionWithoutLeakingInvalidBodyWhenReadingGenericType() {
        HttpErrorResponse error =
                error(
                        """
                {"secret":"should-not-appear"
                """);
        Type type = new TypeReference<Map<String, Object>>() {}.getType();

        assertThatThrownBy(() -> decoder.read(error, type))
                .isInstanceOfSatisfying(
                        HttpErrorBodyDecodingException.class,
                        decodingException -> {
                            assertThat(decodingException.error()).isSameAs(error);
                            assertThat(decodingException.getMessage()).contains("target=");
                            assertThat(decodingException.getMessage())
                                    .doesNotContain("should-not-appear");
                        });
    }

    @Test
    void rejectsNullClassType() {
        HttpErrorResponse error = error("{}");

        assertThatThrownBy(() -> decoder.decode(error, (Class<ErrorPayload>) null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("type must not be null");
    }

    @Test
    void rejectsNullGenericType() {
        HttpErrorResponse error = error("{}");

        assertThatThrownBy(() -> decoder.decode(error, (Type) null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("type must not be null");
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
                false);
    }

    private record ErrorPayload(String code, String message) {}
}
