package com.smbtech.serviceframework.starter.mock.adapter.out.resource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smbtech.serviceframework.mock.domain.MockDefinition;
import com.smbtech.serviceframework.mock.domain.MockRequest;
import com.smbtech.serviceframework.mock.domain.MockResponse;
import com.smbtech.serviceframework.mock.exception.MockException;
import com.smbtech.serviceframework.mock.port.out.MockResponseSource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ResourceMockResponseSource implements MockResponseSource {

    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;

    public ResourceMockResponseSource(ResourceLoader resourceLoader, ObjectMapper objectMapper) {
        this.resourceLoader = Objects.requireNonNull(resourceLoader, "resourceLoader must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    @Override
    public MockResponse load(MockDefinition definition, MockRequest request) {
        try {
            JsonNode root = readRoot(definition.file());
            int status = root.path("status").asInt(200);
            Map<String, List<String>> headers = parseHeaders(root.path("headers"));
            byte[] body = parseBody(root.path("body"));

            return new MockResponse(status, headers, body, definition.delay(), Map.of(
                    "key", definition.key(),
                    "file", definition.file()
            ));
        } catch (MockException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new MockException("Error loading mock response: " + definition.file(), exception);
        }
    }

    private JsonNode readRoot(String location) {
        try {
            Resource resource = resourceLoader.getResource(normalizeLocation(location));

            if (!resource.exists()) {
                throw new MockException("Mock file does not exist: " + location);
            }

            try (InputStream inputStream = resource.getInputStream()) {
                return objectMapper.readTree(inputStream);
            }
        } catch (MockException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new MockException("Error reading mock file: " + location, exception);
        }
    }

    private String normalizeLocation(String location) {
        if (location == null || location.isBlank()) {
            throw new MockException("Mock file location cannot be empty");
        }

        if (location.startsWith("classpath:")
                || location.startsWith("file:")) {
            return location;
        }

        return "classpath:" + location;
    }

    private Map<String, List<String>> parseHeaders(JsonNode headersNode) {
        Map<String, List<String>> headers = new LinkedHashMap<>();

        if (headersNode == null || headersNode.isMissingNode() || !headersNode.isObject()) {
            return headers;
        }

        for (Map.Entry<String, JsonNode> field : headersNode.properties()) {
            JsonNode value = field.getValue();

            if (value.isArray()) {
                headers.put(field.getKey(), streamTexts(value));
            } else {
                headers.put(field.getKey(), List.of(value.asText()));
            }
        }
        return headers;
    }

    private List<String> streamTexts(JsonNode value) {
        java.util.ArrayList<String> values = new java.util.ArrayList<>();
        value.forEach(item -> values.add(item.asText()));
        return values;
    }

    private byte[] parseBody(JsonNode bodyNode) throws Exception {
        if (bodyNode == null || bodyNode.isMissingNode() || bodyNode.isNull()) {
            return new byte[0];
        }
        return objectMapper.writeValueAsBytes(bodyNode);
    }
}
