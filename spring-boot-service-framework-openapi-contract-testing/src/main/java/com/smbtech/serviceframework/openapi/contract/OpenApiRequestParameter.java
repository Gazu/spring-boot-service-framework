package com.smbtech.serviceframework.openapi.contract;

import java.util.Objects;
import tools.jackson.databind.JsonNode;

record OpenApiRequestParameter(String name, String location, boolean required, JsonNode schema) {

    OpenApiRequestParameter {
        name = Objects.requireNonNull(name, "name");
        location = Objects.requireNonNull(location, "location");
        schema = Objects.requireNonNull(schema, "schema");
    }
}
