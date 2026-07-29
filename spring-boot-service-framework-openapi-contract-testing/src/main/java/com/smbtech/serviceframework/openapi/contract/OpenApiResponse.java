package com.smbtech.serviceframework.openapi.contract;

import java.util.Map;
import java.util.Objects;
import tools.jackson.databind.JsonNode;

/**
 * Carries immutable open api response data.
 *
 * @param status status value
 * @param contentSchemas content schemas value
 */
public record OpenApiResponse(int status, Map<String, JsonNode> contentSchemas) {

    /** Creates and validates the record components. */
    public OpenApiResponse {
        if (status < 100 || status > 599) {
            throw new IllegalArgumentException("status must be between 100 and 599");
        }
        contentSchemas = Map.copyOf(Objects.requireNonNull(contentSchemas, "contentSchemas"));
    }
}
