package com.smbtech.serviceframework.starter.mock.api;

import java.util.Optional;
import org.springframework.http.ResponseEntity;
import tools.jackson.core.type.TypeReference;

/** Defines the mock service contract. */
public interface MockService {
    /**
     * Performs the exchange mock operation.
     *
     * @param mockKey mock key value
     * @param responseType response type value
     * @return exchange mock result
     * @param <T> generic value type
     */
    <T> Optional<ResponseEntity<T>> exchangeMock(String mockKey, TypeReference<T> responseType);

    /**
     * Performs the exchange mock operation.
     *
     * @param mockKey mock key value
     * @param responseType response type value
     * @return exchange mock result
     * @param <T> generic value type
     */
    <T> Optional<ResponseEntity<T>> exchangeMock(String mockKey, Class<T> responseType);

    /**
     * Performs the exchange mock operation.
     *
     * @param mockKey mock key value
     * @return exchange mock result
     */
    default Optional<ResponseEntity<String>> exchangeMock(String mockKey) {
        return exchangeMock(mockKey, String.class);
    }

    /**
     * Performs the response operation.
     *
     * @param mockKey mock key value
     * @param responseType response type value
     * @return response result
     * @param <T> generic value type
     */
    default <T> Optional<ResponseEntity<T>> response(
            String mockKey, TypeReference<T> responseType) {
        return exchangeMock(mockKey, responseType);
    }

    /**
     * Performs the response operation.
     *
     * @param mockKey mock key value
     * @param responseType response type value
     * @return response result
     * @param <T> generic value type
     */
    default <T> Optional<ResponseEntity<T>> response(String mockKey, Class<T> responseType) {
        return exchangeMock(mockKey, responseType);
    }

    /**
     * Performs the response operation.
     *
     * @param mockKey mock key value
     * @return response result
     */
    default Optional<ResponseEntity<String>> response(String mockKey) {
        return exchangeMock(mockKey);
    }

    /**
     * Performs the response or not found operation.
     *
     * @param mockKey mock key value
     * @param responseType response type value
     * @return response or not found result
     * @param <T> generic value type
     */
    default <T> ResponseEntity<T> responseOrNotFound(
            String mockKey, TypeReference<T> responseType) {
        return response(mockKey, responseType).orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Performs the response or not found operation.
     *
     * @param mockKey mock key value
     * @param responseType response type value
     * @return response or not found result
     * @param <T> generic value type
     */
    default <T> ResponseEntity<T> responseOrNotFound(String mockKey, Class<T> responseType) {
        return response(mockKey, responseType).orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Performs the response or not found operation.
     *
     * @param mockKey mock key value
     * @return response or not found result
     */
    default ResponseEntity<String> responseOrNotFound(String mockKey) {
        return response(mockKey).orElseGet(() -> ResponseEntity.notFound().build());
    }
}
