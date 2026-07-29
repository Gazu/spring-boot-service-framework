package com.smbtech.serviceframework.starter.mock.adapter.in.spring;

import com.smbtech.serviceframework.mock.domain.MockResponse;
import com.smbtech.serviceframework.mock.exception.MockException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;

/** Provides mock response entity mapper behavior. */
public final class MockResponseEntityMapper {

    private final ObjectMapper objectMapper;

    /**
     * Creates a mock response entity mapper instance.
     *
     * @param objectMapper object mapper value
     */
    public MockResponseEntityMapper(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    /**
     * Performs the to response entity operation.
     *
     * @param response response value
     * @param responseType response type value
     * @return to response entity result
     * @param <T> generic value type
     */
    public <T> ResponseEntity<T> toResponseEntity(
            MockResponse response, TypeReference<T> responseType) {
        return toResponseEntity(
                response, objectMapper.getTypeFactory().constructType(responseType));
    }

    /**
     * Performs the to response entity operation.
     *
     * @param response response value
     * @param responseType response type value
     * @return to response entity result
     * @param <T> generic value type
     */
    public <T> ResponseEntity<T> toResponseEntity(MockResponse response, Class<T> responseType) {
        return toResponseEntity(
                response, objectMapper.getTypeFactory().constructType(responseType));
    }

    private <T> ResponseEntity<T> toResponseEntity(MockResponse response, JavaType responseType) {
        return ResponseEntity.status(response.status())
                .headers(toHttpHeaders(response))
                .body(toBody(response, responseType));
    }

    private HttpHeaders toHttpHeaders(MockResponse response) {
        HttpHeaders headers = new HttpHeaders();
        response.headers()
                .forEach((name, values) -> values.forEach(value -> headers.add(name, value)));
        return headers;
    }

    @SuppressWarnings("unchecked")
    private <T> T toBody(MockResponse response, JavaType responseType) {
        if (!response.hasBody()) {
            return null;
        }

        try {
            Class<?> rawClass = responseType.getRawClass();
            if (byte[].class.equals(rawClass)) {
                return (T) response.body();
            }
            if (String.class.equals(rawClass)) {
                return (T) new String(response.body(), StandardCharsets.UTF_8);
            }
            return objectMapper.readValue(response.body(), responseType);
        } catch (Exception exception) {
            throw new MockException("Error converting mock response body", exception);
        }
    }
}
