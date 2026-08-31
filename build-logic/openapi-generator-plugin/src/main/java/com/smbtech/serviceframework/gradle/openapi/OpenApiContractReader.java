package com.smbtech.serviceframework.gradle.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

final class OpenApiContractReader {

    private static final Pattern OPENAPI_VERSION = Pattern.compile("3\\.[01]\\.\\d+");
    private static final Pattern ARTIFACT_VERSION =
            Pattern.compile("[0-9]+\\.[0-9]+\\.[0-9]+(?:[-+][0-9A-Za-z][0-9A-Za-z.-]*)?");
    private static final Pattern ARTIFACT_BASE_NAME = Pattern.compile("[a-z0-9]+(?:-[a-z0-9]+)*");

    private OpenApiContractReader() {}

    static OpenApiContractIdentity read(File source) {
        OpenAPI contract = readDocument(source);
        String title = text(contract.getInfo().getTitle());
        String version = text(contract.getInfo().getVersion());
        return new OpenApiContractIdentity(title, version, normalizeTitle(title));
    }

    static OpenAPI readDocument(File source) {
        List<String> failures = new ArrayList<>();
        SwaggerParseResult result =
                parser().readLocation(source.toURI().toString(), null, options());
        OpenAPI contract = result.getOpenAPI();
        if (result.getMessages() != null) {
            result.getMessages().stream()
                    .filter(message -> message != null && !message.isBlank())
                    .forEach(failures::add);
        }
        if (contract == null) {
            failures.add("document could not be parsed as OpenAPI");
            throw invalid(source, failures);
        }
        if (contract.getOpenapi() == null
                || !OPENAPI_VERSION.matcher(contract.getOpenapi()).matches()) {
            failures.add("openapi must declare a supported 3.0.x or 3.1.x version");
        }
        if (contract.getInfo() == null) {
            failures.add("info is required");
            throw invalid(source, failures);
        }
        String title = text(contract.getInfo().getTitle());
        String version = text(contract.getInfo().getVersion());
        if (title.isEmpty()) {
            failures.add("info.title is required");
        }
        if (version.isEmpty()) {
            failures.add("info.version is required");
        } else if (!ARTIFACT_VERSION.matcher(version).matches()) {
            failures.add("info.version must be SemVer/Maven compatible, found '" + version + "'");
        }
        validateOperations(contract, failures);
        String artifactBaseName = normalizeTitle(title);
        if (!title.isEmpty() && !ARTIFACT_BASE_NAME.matcher(artifactBaseName).matches()) {
            failures.add("info.title cannot be normalized to a valid Maven artifact name");
        }
        if (!failures.isEmpty()) {
            throw invalid(source, failures);
        }
        return contract;
    }

    static String normalizeTitle(String title) {
        return text(title)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[\\s_.]+", "-")
                .replaceAll("[^a-z0-9-]", "")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }

    private static void validateOperations(OpenAPI contract, List<String> failures) {
        if (contract.getPaths() == null || contract.getPaths().isEmpty()) {
            failures.add("paths must contain at least one operation");
            return;
        }
        Set<String> operationIds = new HashSet<>();
        contract.getPaths()
                .forEach(
                        (path, item) ->
                                item.readOperationsMap()
                                        .forEach(
                                                (method, operation) ->
                                                        validateOperation(
                                                                path,
                                                                method.name(),
                                                                operation,
                                                                operationIds,
                                                                failures)));
    }

    private static void validateOperation(
            String path,
            String method,
            Operation operation,
            Set<String> operationIds,
            List<String> failures) {
        String operationId = text(operation.getOperationId());
        if (operationId.isEmpty()) {
            failures.add(method + " " + path + ": operationId is required");
        } else if (!operationIds.add(operationId)) {
            failures.add("operationId '" + operationId + "' must be unique");
        }
    }

    private static OpenAPIV3Parser parser() {
        return new OpenAPIV3Parser();
    }

    private static ParseOptions options() {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setResolveFully(false);
        options.setResolveCombinators(false);
        return options;
    }

    private static IllegalArgumentException invalid(File source, List<String> failures) {
        return new IllegalArgumentException(
                source.getPath() + ": " + String.join("; ", failures.stream().distinct().toList()));
    }

    private static String text(String value) {
        return value == null ? "" : value.trim();
    }
}
