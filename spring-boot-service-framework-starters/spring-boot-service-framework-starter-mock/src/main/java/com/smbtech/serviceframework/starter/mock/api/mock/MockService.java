package com.smbtech.serviceframework.starter.mock.api.mock;

import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

public interface MockService {
    <T> Optional<ResponseEntity<T>> exchangeMock(String mockKey, TypeReference<T> responseType);

    <T> Optional<ResponseEntity<T>> exchangeMock(String mockKey, Class<T> responseType);

    default Optional<ResponseEntity<String>> exchangeMock(String mockKey) {
        return exchangeMock(mockKey, String.class);
    }

    default <T> Optional<ResponseEntity<T>> response(String mockKey, TypeReference<T> responseType) {
        return exchangeMock(mockKey, responseType);
    }

    default <T> Optional<ResponseEntity<T>> response(String mockKey, Class<T> responseType) {
        return exchangeMock(mockKey, responseType);
    }

    default Optional<ResponseEntity<String>> response(String mockKey) {
        return exchangeMock(mockKey);
    }

    default <T> ResponseEntity<T> responseOrNotFound(String mockKey, TypeReference<T> responseType) {
        return response(mockKey, responseType)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    default <T> ResponseEntity<T> responseOrNotFound(String mockKey, Class<T> responseType) {
        return response(mockKey, responseType)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    default ResponseEntity<String> responseOrNotFound(String mockKey) {
        return response(mockKey)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
