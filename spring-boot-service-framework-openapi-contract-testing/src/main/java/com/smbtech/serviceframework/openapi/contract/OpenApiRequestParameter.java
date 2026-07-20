package com.smbtech.serviceframework.openapi.contract;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Objects;

record OpenApiRequestParameter(String name, String location, boolean required, JsonNode schema) {

    OpenApiRequestParameter {
        name = Objects.requireNonNull(name, "name");
        location = Objects.requireNonNull(location, "location");
        schema = Objects.requireNonNull(schema, "schema");
    }
}
