package com.smbtech.serviceframework.starter.mock.adapter.in.spring;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smbtech.serviceframework.mock.domain.MockResponse;
import com.smbtech.serviceframework.mock.exception.MockException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MockResponseEntityMapperTest {

    private final MockResponseEntityMapper mapper = new MockResponseEntityMapper(
            new ObjectMapper().findAndRegisterModules()
    );

    @Test
    void mapsHeadersStatusAndTypedBody() {
        MockResponse response = new MockResponse(
                201,
                Map.of("X-Mock", List.of("true")),
                "{\"id\":\"pay-123\",\"status\":\"MOCKED\"}".getBytes(StandardCharsets.UTF_8)
        );

        var entity = mapper.toResponseEntity(response, PaymentMock.class);

        assertThat(entity.getStatusCode().value()).isEqualTo(201);
        assertThat(entity.getHeaders().getFirst("X-Mock")).isEqualTo("true");
        assertThat(entity.getBody()).isEqualTo(new PaymentMock("pay-123", "MOCKED"));
    }

    @Test
    void mapsGenericBodyWithTypeReference() {
        MockResponse response = MockResponse.ok(
                "{\"items\":[{\"id\":\"pay-123\"}]}".getBytes(StandardCharsets.UTF_8)
        );

        var entity = mapper.toResponseEntity(response, new TypeReference<Map<String, List<Map<String, String>>>>() {
        });

        assertThat(entity.getBody())
                .containsEntry("items", List.of(Map.of("id", "pay-123")));
    }

    @Test
    void mapsRawStringAndBytes() {
        MockResponse response = MockResponse.ok("{\"ok\":true}".getBytes(StandardCharsets.UTF_8));

        var stringEntity = mapper.toResponseEntity(response, String.class);
        var bytesEntity = mapper.toResponseEntity(response, byte[].class);

        assertThat(stringEntity.getBody()).isEqualTo("{\"ok\":true}");
        assertThat(bytesEntity.getBody()).containsSequence((byte) '{', (byte) '"');
    }

    @Test
    void mapsEmptyBodyAsNullBody() {
        MockResponse response = new MockResponse(204, Map.of(), new byte[0]);

        var entity = mapper.toResponseEntity(response, PaymentMock.class);

        assertThat(entity.getStatusCode().value()).isEqualTo(204);
        assertThat(entity.getBody()).isNull();
    }

    @Test
    void wrapsConversionFailuresAsMockException() {
        MockResponse response = MockResponse.ok("not-json".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> mapper.toResponseEntity(response, PaymentMock.class))
                .isInstanceOf(MockException.class)
                .hasMessage("Error converting mock response body");
    }

    private record PaymentMock(String id, String status) {
    }
}
