package com.smbtech.serviceframework.starter.mock.adapter.out.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smbtech.serviceframework.mock.domain.MockDefinition;
import com.smbtech.serviceframework.mock.domain.MockRequest;
import com.smbtech.serviceframework.mock.exception.MockException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

class ResourceMockResponseSourceTest {

    private final ResourceMockResponseSource source =
            new ResourceMockResponseSource(
                    new DefaultResourceLoader(), new ObjectMapper().findAndRegisterModules());

    @Test
    void loadsResponseWithDefaultStatusAndMetadata() {
        MockDefinition definition =
                new MockDefinition(
                        "default-response",
                        true,
                        "mocks/default-response.json",
                        Duration.ofMillis(10));

        var response = source.load(definition, new MockRequest("default-response"));

        assertThat(response.status()).isEqualTo(200);
        assertThat(response.delay()).isEqualTo(Duration.ofMillis(10));
        assertThat(response.headers()).isEmpty();
        assertThat(new String(response.body(), StandardCharsets.UTF_8)).isEqualTo("{\"ok\":true}");
        assertThat(response.metadata())
                .containsEntry("key", "default-response")
                .containsEntry("file", "mocks/default-response.json");
    }

    @Test
    void loadsSingleAndMultiValueHeaders() {
        MockDefinition definition =
                new MockDefinition(
                        "headers-response",
                        true,
                        "classpath:mocks/headers-response.json",
                        Duration.ZERO);

        var response = source.load(definition, new MockRequest("headers-response"));

        assertThat(response.status()).isEqualTo(202);
        assertThat(response.headers())
                .containsEntry("X-Mock", java.util.List.of("true"))
                .containsEntry("Set-Cookie", java.util.List.of("a=1", "b=2"));
        assertThat(new String(response.body(), StandardCharsets.UTF_8))
                .isEqualTo("{\"status\":\"ACCEPTED\"}");
    }

    @Test
    void failsWhenFileIsMissing() {
        MockDefinition definition =
                new MockDefinition("missing", true, "mocks/missing.json", Duration.ZERO);

        assertThatThrownBy(() -> source.load(definition, new MockRequest("missing")))
                .isInstanceOf(MockException.class)
                .hasMessageContaining("Mock file does not exist: mocks/missing.json");
    }

    @Test
    void failsWhenFileLocationIsBlank() {
        MockDefinition definition = new MockDefinition("blank", true, "", Duration.ZERO);

        assertThatThrownBy(() -> source.load(definition, new MockRequest("blank")))
                .isInstanceOf(MockException.class)
                .hasMessageContaining("Mock file location cannot be empty");
    }
}
