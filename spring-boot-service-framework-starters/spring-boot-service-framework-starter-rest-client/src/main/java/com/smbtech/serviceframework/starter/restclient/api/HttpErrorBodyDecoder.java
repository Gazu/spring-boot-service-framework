package com.smbtech.serviceframework.starter.restclient.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smbtech.serviceframework.httpclient.domain.HttpErrorResponse;
import com.smbtech.serviceframework.httpclient.exception.HttpClientResponseException;
import com.smbtech.serviceframework.httpclient.port.out.HttpErrorResponseBodyReader;
import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;

/** Decodes downstream HTTP error bodies captured by {@link HttpClientResponseException}. */
public final class HttpErrorBodyDecoder implements HttpErrorResponseBodyReader {

    private final ObjectMapper objectMapper;

    /**
     * Creates a http error body decoder instance.
     *
     * @param objectMapper object mapper value
     */
    public HttpErrorBodyDecoder(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    /**
     * Decodes the complete response body carried by an HTTP client exception.
     *
     * @param exception HTTP client response exception
     * @param type target type
     * @return decoded body
     * @param <T> target type
     */
    public <T> T decode(HttpClientResponseException exception, Class<T> type) {
        Objects.requireNonNull(exception, "exception must not be null");
        return decode(exception.error(), type);
    }

    /**
     * Decodes the complete response body carried by an HTTP client exception.
     *
     * @param exception HTTP client response exception
     * @param type target type reference
     * @return decoded body
     * @param <T> target type
     */
    public <T> T decode(HttpClientResponseException exception, TypeReference<T> type) {
        Objects.requireNonNull(exception, "exception must not be null");
        return decode(exception.error(), type);
    }

    @Override
    public <T> T read(HttpErrorResponse error, Class<T> type) {
        return decode(error, type);
    }

    @Override
    public <T> T read(HttpErrorResponse error, Type type) {
        return decode(error, type);
    }

    /**
     * Decodes an HTTP error response body.
     *
     * @param error HTTP error response
     * @param type target type
     * @return decoded body
     * @param <T> target type
     */
    public <T> T decode(HttpErrorResponse error, Class<T> type) {
        Objects.requireNonNull(type, "type must not be null");
        HttpErrorResponse safeError = requireBody(error);
        try {
            return objectMapper.readValue(safeError.body(), type);
        } catch (JsonProcessingException exception) {
            throw new HttpErrorBodyDecodingException(safeError, type.getName(), exception);
        }
    }

    /**
     * Decodes an HTTP error response body.
     *
     * @param error HTTP error response
     * @param type target generic type
     * @return decoded body
     * @param <T> target type
     */
    public <T> T decode(HttpErrorResponse error, Type type) {
        Objects.requireNonNull(type, "type must not be null");
        HttpErrorResponse safeError = requireBody(error);
        JavaType javaType = objectMapper.getTypeFactory().constructType(type);
        try {
            return objectMapper.readValue(safeError.body(), javaType);
        } catch (JsonProcessingException exception) {
            throw new HttpErrorBodyDecodingException(safeError, type.getTypeName(), exception);
        }
    }

    /**
     * Decodes an HTTP error response body.
     *
     * @param error HTTP error response
     * @param type target type reference
     * @return decoded body
     * @param <T> target type
     */
    public <T> T decode(HttpErrorResponse error, TypeReference<T> type) {
        Objects.requireNonNull(type, "type must not be null");
        HttpErrorResponse safeError = requireBody(error);
        try {
            return objectMapper.readValue(safeError.body(), type);
        } catch (JsonProcessingException exception) {
            throw new HttpErrorBodyDecodingException(
                    safeError, type.getType().getTypeName(), exception);
        }
    }

    /**
     * Decodes the body when present, otherwise returns {@link Optional#empty()}.
     *
     * @param exception HTTP client response exception
     * @param type target type
     * @return decoded body when the exception carries a body
     * @param <T> target type
     */
    public <T> Optional<T> decodeIfPresent(HttpClientResponseException exception, Class<T> type) {
        Objects.requireNonNull(exception, "exception must not be null");
        return decodeIfPresent(exception.error(), type);
    }

    /**
     * Decodes the body when present, otherwise returns {@link Optional#empty()}.
     *
     * @param exception HTTP client response exception
     * @param type target type reference
     * @return decoded body when the exception carries a body
     * @param <T> target type
     */
    public <T> Optional<T> decodeIfPresent(
            HttpClientResponseException exception, TypeReference<T> type) {
        Objects.requireNonNull(exception, "exception must not be null");
        return decodeIfPresent(exception.error(), type);
    }

    /**
     * Decodes the body when present, otherwise returns {@link Optional#empty()}.
     *
     * @param error HTTP error response
     * @param type target type
     * @return decoded body when present
     * @param <T> target type
     */
    public <T> Optional<T> decodeIfPresent(HttpErrorResponse error, Class<T> type) {
        if (error == null || error.body().isBlank()) {
            return Optional.empty();
        }
        return Optional.of(decode(error, type));
    }

    /**
     * Decodes the body when present, otherwise returns {@link Optional#empty()}.
     *
     * @param error HTTP error response
     * @param type target type reference
     * @return decoded body when present
     * @param <T> target type
     */
    public <T> Optional<T> decodeIfPresent(HttpErrorResponse error, TypeReference<T> type) {
        if (error == null || error.body().isBlank()) {
            return Optional.empty();
        }
        return Optional.of(decode(error, type));
    }

    private HttpErrorResponse requireBody(HttpErrorResponse error) {
        HttpErrorResponse safeError = Objects.requireNonNull(error, "error must not be null");
        if (safeError.body().isBlank()) {
            throw new HttpErrorBodyDecodingException(
                    safeError, "HTTP error response does not contain a body");
        }
        return safeError;
    }
}
