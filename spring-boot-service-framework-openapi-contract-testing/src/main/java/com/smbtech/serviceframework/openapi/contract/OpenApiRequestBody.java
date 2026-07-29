package com.smbtech.serviceframework.openapi.contract;

import java.util.Map;
import java.util.Objects;
import tools.jackson.databind.JsonNode;

record OpenApiRequestBody(boolean required, Map<String, JsonNode> contentSchemas) {

    OpenApiRequestBody {
        contentSchemas = Map.copyOf(Objects.requireNonNull(contentSchemas, "contentSchemas"));
    }
}
