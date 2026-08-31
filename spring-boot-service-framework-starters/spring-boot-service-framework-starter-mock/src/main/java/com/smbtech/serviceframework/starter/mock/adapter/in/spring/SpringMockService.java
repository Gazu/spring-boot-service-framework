package com.smbtech.serviceframework.starter.mock.adapter.in.spring;

import com.smbtech.serviceframework.mock.domain.MockRequest;
import com.smbtech.serviceframework.mock.port.in.MockResponder;
import com.smbtech.serviceframework.starter.mock.api.MockService;
import java.util.Objects;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import tools.jackson.core.type.TypeReference;

/** Provides spring mock service behavior. */
final class SpringMockService implements MockService {

    private final MockResponder mockResponder;
    private final MockResponseEntityMapper responseEntityMapper;

    /**
     * Creates a spring mock service instance.
     *
     * @param mockResponder mock responder value
     * @param responseEntityMapper response entity mapper value
     */
    SpringMockService(MockResponder mockResponder, MockResponseEntityMapper responseEntityMapper) {
        this.mockResponder =
                Objects.requireNonNull(mockResponder, "mockResponder must not be null");
        this.responseEntityMapper =
                Objects.requireNonNull(
                        responseEntityMapper, "responseEntityMapper must not be null");
    }

    @Override
    public <T> Optional<ResponseEntity<T>> exchangeMock(
            String mockKey, TypeReference<T> responseType) {
        return mockResponder
                .respond(new MockRequest(mockKey))
                .map(response -> responseEntityMapper.toResponseEntity(response, responseType));
    }

    @Override
    public <T> Optional<ResponseEntity<T>> exchangeMock(String mockKey, Class<T> responseType) {
        return mockResponder
                .respond(new MockRequest(mockKey))
                .map(response -> responseEntityMapper.toResponseEntity(response, responseType));
    }
}
