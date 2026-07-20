package com.smbtech.serviceframework.openapi.contract;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import jakarta.servlet.http.Cookie;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.util.UriUtils;

/** Provides open api mvc contract tester behavior. */
public final class OpenApiMvcContractTester {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;
    private final OpenApiContract contract;

    /**
     * Creates an OpenAPI mvc contract tester instance.
     *
     * @param mockMvc mock mvc value
     * @param objectMapper object mapper value
     * @param contract contract value
     */
    public OpenApiMvcContractTester(
            MockMvc mockMvc, ObjectMapper objectMapper, OpenApiContract contract) {
        this.mockMvc = Objects.requireNonNull(mockMvc, "mockMvc");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.contract = Objects.requireNonNull(contract, "contract");
    }

    /**
     * Performs the verify operation.
     *
     * @param testCases test cases value
     * @return verify result
     */
    public OpenApiContractTestResult verify(OpenApiContractTestCase... testCases) {
        Objects.requireNonNull(testCases, "testCases");
        List<OpenApiContractViolation> violations = new ArrayList<>();
        for (OpenApiContractTestCase testCase : testCases) {
            verifyCase(Objects.requireNonNull(testCase, "testCase"), violations);
        }
        return new OpenApiContractTestResult(violations);
    }

    /**
     * Performs the verify all operation.
     *
     * @param testCases test cases value
     * @return verify all result
     */
    public OpenApiContractTestResult verifyAll(OpenApiContractTestCase... testCases) {
        OpenApiContractTestResult executed = verify(testCases);
        List<OpenApiContractViolation> violations = new ArrayList<>(executed.violations());
        Set<String> coveredOperations = new HashSet<>();
        for (OpenApiContractTestCase testCase : testCases) {
            coveredOperations.add(testCase.operationId());
        }
        contract.operations().stream()
                .filter(operation -> !coveredOperations.contains(operation.operationId()))
                .forEach(
                        operation ->
                                violations.add(
                                        violation(
                                                OpenApiContractViolationCode.MISSING_TEST_CASE,
                                                operation.operationId(),
                                                operation.method() + " " + operation.pathTemplate(),
                                                "operation has no contract test case")));
        return new OpenApiContractTestResult(violations);
    }

    private void verifyCase(
            OpenApiContractTestCase testCase, List<OpenApiContractViolation> violations) {
        OpenApiOperation operation = contract.findOperation(testCase.operationId()).orElse(null);
        if (operation == null) {
            violations.add(
                    violation(
                            OpenApiContractViolationCode.UNKNOWN_OPERATION,
                            testCase.operationId(),
                            "operationId",
                            "operation is not declared by "
                                    + contract.title()
                                    + " "
                                    + contract.version()));
            return;
        }

        String resolvedPath = resolvePath(operation, testCase, violations);
        int violationsBeforeRequestValidation = violations.size();
        validateRequest(operation, testCase, violations);
        if (resolvedPath == null || violations.size() > violationsBeforeRequestValidation) {
            return;
        }

        MvcResult result;
        try {
            MockHttpServletRequestBuilder requestBuilder =
                    request(HttpMethod.valueOf(operation.method()), URI.create(resolvedPath));
            testCase.queryParameters().forEach(requestBuilder::queryParam);
            testCase.headers().forEach(requestBuilder::header);
            testCase.cookies()
                    .forEach((name, value) -> requestBuilder.cookie(new Cookie(name, value)));
            if (testCase.requestBody() != null) {
                requestBuilder
                        .contentType(testCase.requestContentType())
                        .content(testCase.requestBody());
            }
            result = mockMvc.perform(requestBuilder).andReturn();
        } catch (Exception exception) {
            violations.add(
                    violation(
                            OpenApiContractViolationCode.REQUEST_EXECUTION_FAILED,
                            operation.operationId(),
                            operation.method() + " " + resolvedPath,
                            exception.getClass().getSimpleName() + ": " + exception.getMessage()));
            return;
        }

        validateResponse(operation, testCase, result, violations);
    }

    private void validateRequest(
            OpenApiOperation operation,
            OpenApiContractTestCase testCase,
            List<OpenApiContractViolation> violations) {
        OpenApiRequestDefinition definition = contract.requestDefinition(operation.operationId());
        for (OpenApiRequestParameter parameter : definition.parameters().values()) {
            String value = requestParameterValue(testCase, parameter);
            if (value == null) {
                if (parameter.required() && !"path".equals(parameter.location())) {
                    violations.add(
                            violation(
                                    OpenApiContractViolationCode.MISSING_REQUEST_PARAMETER,
                                    operation.operationId(),
                                    "request." + parameter.location() + "." + parameter.name(),
                                    "required "
                                            + parameter.location()
                                            + " parameter has no test value"));
                }
                continue;
            }
            validateSchema(
                    operation.operationId(),
                    "request." + parameter.location() + "." + parameter.name(),
                    parameterValue(value, parameter.schema()),
                    parameter.schema(),
                    new HashSet<>(),
                    OpenApiContractViolationCode.REQUEST_SCHEMA_MISMATCH,
                    violations);
        }

        OpenApiRequestBody requestBody = definition.requestBody();
        if (testCase.requestBody() == null) {
            if (requestBody != null && requestBody.required()) {
                violations.add(
                        violation(
                                OpenApiContractViolationCode.MISSING_REQUEST_BODY,
                                operation.operationId(),
                                "request.body",
                                "required request body has no test value"));
            }
            return;
        }
        if (requestBody == null || requestBody.contentSchemas().isEmpty()) {
            violations.add(
                    violation(
                            OpenApiContractViolationCode.UNDECLARED_REQUEST_BODY,
                            operation.operationId(),
                            "request.body",
                            "request body is present but the OpenAPI operation declares no content"));
            return;
        }

        Map.Entry<String, JsonNode> declaredContent =
                findCompatibleContent(requestBody.contentSchemas(), testCase.requestContentType());
        if (declaredContent == null) {
            violations.add(
                    violation(
                            OpenApiContractViolationCode.UNDECLARED_REQUEST_CONTENT_TYPE,
                            operation.operationId(),
                            "request.contentType",
                            "content type "
                                    + testCase.requestContentType()
                                    + " is not declared; expected one of "
                                    + requestBody.contentSchemas().keySet()));
            return;
        }

        JsonNode body;
        try {
            body = objectMapper.readTree(testCase.requestBody());
        } catch (Exception exception) {
            violations.add(
                    violation(
                            OpenApiContractViolationCode.INVALID_JSON_REQUEST,
                            operation.operationId(),
                            "request.body",
                            "request is not valid JSON: " + exception.getMessage()));
            return;
        }
        if (body == null) {
            violations.add(
                    violation(
                            OpenApiContractViolationCode.INVALID_JSON_REQUEST,
                            operation.operationId(),
                            "request.body",
                            "request is empty"));
            return;
        }
        validateSchema(
                operation.operationId(),
                "request.body",
                body,
                declaredContent.getValue(),
                new HashSet<>(),
                OpenApiContractViolationCode.REQUEST_SCHEMA_MISMATCH,
                violations);
    }

    private static String requestParameterValue(
            OpenApiContractTestCase testCase, OpenApiRequestParameter parameter) {
        return switch (parameter.location()) {
            case "path" -> testCase.pathParameters().get(parameter.name());
            case "query" -> testCase.queryParameters().get(parameter.name());
            case "header" ->
                    testCase.headers().entrySet().stream()
                            .filter(entry -> entry.getKey().equalsIgnoreCase(parameter.name()))
                            .map(Map.Entry::getValue)
                            .findFirst()
                            .orElse(null);
            case "cookie" -> testCase.cookies().get(parameter.name());
            default -> null;
        };
    }

    private static JsonNode parameterValue(String value, JsonNode schema) {
        String type = schema.path("type").asText();
        try {
            return switch (type) {
                case "integer" -> JsonNodeFactory.instance.numberNode(Long.parseLong(value));
                case "number" -> JsonNodeFactory.instance.numberNode(new BigDecimal(value));
                case "boolean" ->
                        "true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)
                                ? JsonNodeFactory.instance.booleanNode(Boolean.parseBoolean(value))
                                : JsonNodeFactory.instance.textNode(value);
                default -> JsonNodeFactory.instance.textNode(value);
            };
        } catch (NumberFormatException exception) {
            return JsonNodeFactory.instance.textNode(value);
        }
    }

    private String resolvePath(
            OpenApiOperation operation,
            OpenApiContractTestCase testCase,
            List<OpenApiContractViolation> violations) {
        String resolved = operation.pathTemplate();
        boolean missing = false;
        for (String name : operation.requiredPathParameters()) {
            String value = testCase.pathParameters().get(name);
            if (value == null || value.isBlank()) {
                violations.add(
                        violation(
                                OpenApiContractViolationCode.MISSING_PATH_PARAMETER,
                                operation.operationId(),
                                "path." + name,
                                "required path parameter has no test value"));
                missing = true;
            } else {
                resolved =
                        resolved.replace(
                                "{" + name + "}",
                                UriUtils.encodePathSegment(value, StandardCharsets.UTF_8));
            }
        }
        return missing ? null : resolved;
    }

    private void validateResponse(
            OpenApiOperation operation,
            OpenApiContractTestCase testCase,
            MvcResult result,
            List<OpenApiContractViolation> violations) {
        int actualStatus = result.getResponse().getStatus();
        String location = operation.method() + " " + operation.pathTemplate();
        int expectedStatus =
                testCase.expectedStatus() == null
                        ? operation.successfulResponse().status()
                        : testCase.expectedStatus();
        OpenApiResponse response = operation.responses().get(expectedStatus);
        if (response == null) {
            violations.add(
                    violation(
                            OpenApiContractViolationCode.UNDECLARED_STATUS,
                            operation.operationId(),
                            location,
                            "expected HTTP "
                                    + expectedStatus
                                    + " is not declared; declared statuses are "
                                    + operation.responses().keySet().stream().sorted().toList()));
            return;
        }
        if (actualStatus != expectedStatus) {
            violations.add(
                    violation(
                            OpenApiContractViolationCode.UNEXPECTED_STATUS,
                            operation.operationId(),
                            location,
                            "expected HTTP "
                                    + expectedStatus
                                    + " but received HTTP "
                                    + actualStatus));
            return;
        }

        byte[] body = result.getResponse().getContentAsByteArray();
        if (response.contentSchemas().isEmpty()) {
            if (!new String(body, StandardCharsets.UTF_8).isBlank()) {
                violations.add(
                        violation(
                                OpenApiContractViolationCode.RESPONSE_SCHEMA_MISMATCH,
                                operation.operationId(),
                                "response.body",
                                "response body is present but the OpenAPI response declares no content"));
            }
            return;
        }

        String contentTypeValue = result.getResponse().getContentType();
        Map.Entry<String, JsonNode> declaredContent =
                findCompatibleContent(response.contentSchemas(), contentTypeValue);
        if (declaredContent == null) {
            violations.add(
                    violation(
                            OpenApiContractViolationCode.UNDECLARED_CONTENT_TYPE,
                            operation.operationId(),
                            "response.contentType",
                            "content type "
                                    + contentTypeValue
                                    + " is not declared; expected one of "
                                    + response.contentSchemas().keySet()));
            return;
        }

        JsonNode actualBody;
        try {
            actualBody = objectMapper.readTree(body);
        } catch (Exception exception) {
            violations.add(
                    violation(
                            OpenApiContractViolationCode.INVALID_JSON_RESPONSE,
                            operation.operationId(),
                            "response.body",
                            "response is not valid JSON: " + exception.getMessage()));
            return;
        }
        validateSchema(
                operation.operationId(),
                "$",
                actualBody,
                declaredContent.getValue(),
                new HashSet<>(),
                OpenApiContractViolationCode.RESPONSE_SCHEMA_MISMATCH,
                violations);
    }

    private static Map.Entry<String, JsonNode> findCompatibleContent(
            Map<String, JsonNode> contentSchemas, String actual) {
        if (actual == null) {
            return null;
        }
        MediaType actualType;
        try {
            actualType = MediaType.parseMediaType(actual);
        } catch (IllegalArgumentException exception) {
            return null;
        }
        return contentSchemas.entrySet().stream()
                .filter(entry -> isCompatible(entry.getKey(), actualType))
                .findFirst()
                .orElse(null);
    }

    private static boolean isCompatible(String declared, MediaType actual) {
        try {
            return MediaType.parseMediaType(declared).isCompatibleWith(actual);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private void validateSchema(
            String operationId,
            String location,
            JsonNode actual,
            JsonNode declared,
            Set<String> references,
            OpenApiContractViolationCode violationCode,
            List<OpenApiContractViolation> violations) {
        if (declared == null || declared.isMissingNode() || declared.isNull()) {
            return;
        }
        if (declared.hasNonNull("$ref")) {
            String reference = declared.path("$ref").asText();
            if (!references.add(reference)) {
                return;
            }
            validateSchema(
                    operationId,
                    location,
                    actual,
                    contract.resolveSchema(declared),
                    references,
                    violationCode,
                    violations);
            references.remove(reference);
            return;
        }

        if (actual != null && actual.isNull() && declared.path("nullable").asBoolean()) {
            return;
        }

        String type = declared.path("type").asText();
        if (!matchesType(actual, type)) {
            addSchemaViolation(
                    operationId,
                    location,
                    "expected " + type + " but found " + jsonType(actual),
                    violationCode,
                    violations);
            return;
        }

        if ("object".equals(type)) {
            declared.path("required")
                    .forEach(
                            required -> {
                                String field = required.asText();
                                if (!actual.has(field) || actual.path(field).isNull()) {
                                    addSchemaViolation(
                                            operationId,
                                            location + "." + field,
                                            "required property is missing",
                                            violationCode,
                                            violations);
                                }
                            });
            declared.path("properties")
                    .properties()
                    .forEach(
                            property -> {
                                if (actual.has(property.getKey())
                                        && !actual.path(property.getKey()).isNull()) {
                                    validateSchema(
                                            operationId,
                                            location + "." + property.getKey(),
                                            actual.path(property.getKey()),
                                            property.getValue(),
                                            references,
                                            violationCode,
                                            violations);
                                }
                            });
        } else if ("array".equals(type)) {
            int index = 0;
            for (JsonNode item : actual) {
                validateSchema(
                        operationId,
                        location + "[" + index + "]",
                        item,
                        declared.path("items"),
                        references,
                        violationCode,
                        violations);
                index++;
            }
        }

        validateConstraints(operationId, location, actual, declared, violationCode, violations);

        if (declared.path("enum").isArray()) {
            boolean accepted = false;
            for (JsonNode allowed : declared.path("enum")) {
                if (allowed.equals(actual)) {
                    accepted = true;
                    break;
                }
            }
            if (!accepted) {
                addSchemaViolation(
                        operationId,
                        location,
                        "value is not part of the declared enum",
                        violationCode,
                        violations);
            }
        }
    }

    private static void validateConstraints(
            String operationId,
            String location,
            JsonNode actual,
            JsonNode declared,
            OpenApiContractViolationCode violationCode,
            List<OpenApiContractViolation> violations) {
        if (actual.isTextual()) {
            int length = actual.textValue().length();
            if (declared.has("minLength") && length < declared.path("minLength").asInt()) {
                addSchemaViolation(
                        operationId,
                        location,
                        "length is below minLength",
                        violationCode,
                        violations);
            }
            if (declared.has("maxLength") && length > declared.path("maxLength").asInt()) {
                addSchemaViolation(
                        operationId,
                        location,
                        "length exceeds maxLength",
                        violationCode,
                        violations);
            }
            if (declared.hasNonNull("pattern")
                    && !java.util.regex.Pattern.compile(declared.path("pattern").asText())
                            .matcher(actual.textValue())
                            .find()) {
                addSchemaViolation(
                        operationId,
                        location,
                        "value does not match pattern",
                        violationCode,
                        violations);
            }
        }
        if (actual.isNumber()) {
            BigDecimal value = actual.decimalValue();
            if (declared.has("minimum")
                    && value.compareTo(declared.path("minimum").decimalValue()) < 0) {
                addSchemaViolation(
                        operationId, location, "value is below minimum", violationCode, violations);
            }
            if (declared.has("maximum")
                    && value.compareTo(declared.path("maximum").decimalValue()) > 0) {
                addSchemaViolation(
                        operationId, location, "value exceeds maximum", violationCode, violations);
            }
        }
        if (actual.isArray()) {
            if (declared.has("minItems") && actual.size() < declared.path("minItems").asInt()) {
                addSchemaViolation(
                        operationId, location, "size is below minItems", violationCode, violations);
            }
            if (declared.has("maxItems") && actual.size() > declared.path("maxItems").asInt()) {
                addSchemaViolation(
                        operationId, location, "size exceeds maxItems", violationCode, violations);
            }
        }
    }

    private static boolean matchesType(JsonNode actual, String type) {
        if (actual == null) {
            return false;
        }
        return switch (type) {
            case "object" -> actual.isObject();
            case "array" -> actual.isArray();
            case "string" -> actual.isTextual();
            case "integer" -> actual.isIntegralNumber();
            case "number" -> actual.isNumber();
            case "boolean" -> actual.isBoolean();
            case "" -> true;
            default -> true;
        };
    }

    private static String jsonType(JsonNode node) {
        return node == null ? "null" : node.getNodeType().name().toLowerCase(java.util.Locale.ROOT);
    }

    private static void addSchemaViolation(
            String operationId,
            String location,
            String message,
            OpenApiContractViolationCode violationCode,
            List<OpenApiContractViolation> violations) {
        violations.add(violation(violationCode, operationId, location, message));
    }

    private static OpenApiContractViolation violation(
            OpenApiContractViolationCode code,
            String operationId,
            String location,
            String message) {
        return new OpenApiContractViolation(code, operationId, location, message);
    }
}
