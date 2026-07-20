package com.smbtech.serviceframework.openapi.contract;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

record OpenApiRequestDefinition(
        Map<String, OpenApiRequestParameter> parameters, OpenApiRequestBody requestBody) {

    static final OpenApiRequestDefinition EMPTY = new OpenApiRequestDefinition(Map.of(), null);

    OpenApiRequestDefinition {
        parameters = Map.copyOf(Objects.requireNonNull(parameters, "parameters"));
    }

    Optional<OpenApiRequestParameter> findParameter(String location, String name) {
        return Optional.ofNullable(parameters.get(key(location, name)));
    }

    static String key(String location, String name) {
        return location + ":" + name;
    }
}
