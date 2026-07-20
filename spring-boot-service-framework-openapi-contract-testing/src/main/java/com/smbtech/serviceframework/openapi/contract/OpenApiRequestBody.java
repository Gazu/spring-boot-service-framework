package com.smbtech.serviceframework.openapi.contract;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import java.util.Objects;

record OpenApiRequestBody(boolean required, Map<String, JsonNode> contentSchemas) {

    OpenApiRequestBody {
        contentSchemas = Map.copyOf(Objects.requireNonNull(contentSchemas, "contentSchemas"));
    }
}
