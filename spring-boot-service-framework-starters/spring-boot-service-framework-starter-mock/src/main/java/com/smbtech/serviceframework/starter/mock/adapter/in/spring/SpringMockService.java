package com.smbtech.serviceframework.starter.mock.adapter.in.spring;

import com.fasterxml.jackson.core.type.TypeReference;
import com.smbtech.serviceframework.mock.domain.MockRequest;
import com.smbtech.serviceframework.mock.port.in.MockResponder;
import com.smbtech.serviceframework.starter.mock.api.mock.MockService;
import org.springframework.http.ResponseEntity;

import java.util.Objects;
import java.util.Optional;

public final class SpringMockService implements MockService {

    private final MockResponder mockResponder;
    private final MockResponseEntityMapper responseEntityMapper;

    public SpringMockService(MockResponder mockResponder, MockResponseEntityMapper responseEntityMapper) {
        this.mockResponder = Objects.requireNonNull(mockResponder, "mockResponder must not be null");
        this.responseEntityMapper = Objects.requireNonNull(responseEntityMapper, "responseEntityMapper must not be null");
    }

    @Override
    public <T> Optional<ResponseEntity<T>> exchangeMock(String mockKey, TypeReference<T> responseType) {
        return mockResponder.respond(new MockRequest(mockKey))
                .map(response -> responseEntityMapper.toResponseEntity(response, responseType));
    }

    @Override
    public <T> Optional<ResponseEntity<T>> exchangeMock(String mockKey, Class<T> responseType) {
        return mockResponder.respond(new MockRequest(mockKey))
                .map(response -> responseEntityMapper.toResponseEntity(response, responseType));
    }
}
