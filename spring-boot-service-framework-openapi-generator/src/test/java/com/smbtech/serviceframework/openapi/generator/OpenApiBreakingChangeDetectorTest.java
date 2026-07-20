package com.smbtech.serviceframework.openapi.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OpenApiBreakingChangeDetectorTest {

    @TempDir Path tempDir;

    private final OpenApiBreakingChangeDetector detector = new OpenApiBreakingChangeDetector();

    @Test
    void classifiesCompatibleAdditionsAndAcceptsMinorVersionIncrease() throws Exception {
        Path baseline = writeSpec("baseline.yaml", "1.0.0", baselinePaths(), baselineSchemas());
        Path current =
                writeSpec(
                        "current.yaml",
                        "1.1.0",
                        baselinePaths()
                                + """
                  /orders:
                    post:
                      operationId: createOrder
                      responses:
                        '201':
                          description: Created
                """,
                        """
                    OrderStatusResponse:
                      type: object
                      required: [orderId, status]
                      properties:
                        orderId: { type: string }
                        status:
                          type: string
                          enum: [processing, completed, cancelled]
                        updatedAt: { type: string, format: date-time }
                """);

        OpenApiCompatibilityReport report = detector.compare(baseline, current);

        assertFalse(report.hasBreakingChanges());
        assertTrue(report.versionPolicyValid());
        assertTrue(
                report.changes().stream()
                        .anyMatch(change -> change.code() == OpenApiChangeCode.OPERATION_ADDED));
        assertTrue(
                report.changes().stream()
                        .anyMatch(change -> change.code() == OpenApiChangeCode.PROPERTY_ADDED));
        assertTrue(
                report.changes().stream()
                        .anyMatch(change -> change.code() == OpenApiChangeCode.ENUM_VALUE_ADDED));
    }

    @Test
    void detectsBreakingOperationSchemaAndResponseChanges() throws Exception {
        Path baseline = writeSpec("baseline.yaml", "1.2.0", baselinePaths(), baselineSchemas());
        Path current =
                writeSpec(
                        "current.yaml",
                        "1.3.0",
                        """
                  /orders/{orderId}/status:
                    get:
                      operationId: findOrderStatus
                      parameters:
                        - name: orderId
                          in: path
                          required: true
                          schema: { type: integer }
                        - name: channel
                          in: header
                          required: true
                          schema: { type: string }
                      responses:
                        '202':
                          description: Accepted
                """,
                        """
                    OrderStatusResponse:
                      type: object
                      required: [orderId, status, updatedAt]
                      properties:
                        orderId: { type: integer }
                        status:
                          type: string
                          enum: [processing]
                        updatedAt: { type: string, format: date-time }
                """);

        OpenApiCompatibilityReport report = detector.compare(baseline, current);

        assertTrue(report.hasBreakingChanges());
        assertFalse(report.versionPolicyValid());
        assertTrue(
                report.changes().stream()
                        .anyMatch(
                                change -> change.code() == OpenApiChangeCode.OPERATION_ID_CHANGED));
        assertTrue(
                report.changes().stream()
                        .anyMatch(change -> change.code() == OpenApiChangeCode.PARAMETER_ADDED));
        assertTrue(
                report.changes().stream()
                        .anyMatch(change -> change.code() == OpenApiChangeCode.RESPONSE_REMOVED));
        assertTrue(
                report.changes().stream()
                        .anyMatch(change -> change.code() == OpenApiChangeCode.TYPE_CHANGED));
        assertTrue(
                report.changes().stream()
                        .anyMatch(change -> change.code() == OpenApiChangeCode.ENUM_VALUE_REMOVED));
        assertTrue(
                report.changes().stream()
                        .anyMatch(change -> change.code() == OpenApiChangeCode.PROPERTY_ADDED));
    }

    @Test
    void acceptsExplicitMajorVersionForBreakingChanges() throws Exception {
        Path baseline = writeSpec("baseline.yaml", "1.2.0", baselinePaths(), baselineSchemas());
        Path current = writeSpec("current.yaml", "2.0.0", "paths: {}\n", baselineSchemas());

        OpenApiCompatibilityReport report = detector.compare(baseline, current);

        assertTrue(report.hasBreakingChanges());
        assertTrue(report.versionPolicyValid());
    }

    @Test
    void baselineResolverChoosesLatestEarlierVersion() throws Exception {
        Path baselineRoot = tempDir.resolve("baselines");
        Path contractDirectory = Files.createDirectories(baselineRoot.resolve("orders-api"));
        writeSpec(contractDirectory.resolve("1.0.0.yaml"), "1.0.0", "paths: {}\n", "schemas: {}\n");
        Path expected =
                writeSpec(
                        contractDirectory.resolve("1.1.0.yaml"),
                        "1.1.0",
                        "paths: {}\n",
                        "schemas: {}\n");
        writeSpec(contractDirectory.resolve("2.0.0.yaml"), "2.0.0", "paths: {}\n", "schemas: {}\n");
        Path currentSource = writeSpec("current.yaml", "1.2.0", "paths: {}\n", "schemas: {}\n");
        OpenApiSpecInfo current = new OpenApiSpecReader().read(currentSource);

        Path actual = new OpenApiBaselineResolver().findLatest(baselineRoot, current).orElseThrow();

        assertEquals(expected, actual);
    }

    @Test
    void cliAllowsVersionedBreakingChangesAndOffersStrictMode() throws Exception {
        Path baselineRoot = tempDir.resolve("baselines");
        Path contractDirectory = Files.createDirectories(baselineRoot.resolve("orders-api"));
        writeSpec(
                contractDirectory.resolve("1.0.0.yaml"),
                "1.0.0",
                baselinePaths(),
                baselineSchemas());
        Path current = writeSpec("current.yaml", "2.0.0", "paths: {}\n", baselineSchemas());
        writeSpec(
                contractDirectory.resolve("2.0.0.yaml"), "2.0.0", "paths: {}\n", baselineSchemas());
        ByteArrayOutputStream standardOutput = new ByteArrayOutputStream();
        ByteArrayOutputStream errorOutput = new ByteArrayOutputStream();

        int normalExit =
                OpenApiBreakingChangeCli.run(
                        new String[] {"--baseline-root=" + baselineRoot, current.toString()},
                        new PrintStream(standardOutput),
                        new PrintStream(errorOutput));
        int strictExit =
                OpenApiBreakingChangeCli.run(
                        new String[] {
                            "--baseline-root=" + baselineRoot,
                            "--fail-on-breaking",
                            current.toString()
                        },
                        new PrintStream(standardOutput),
                        new PrintStream(errorOutput));

        assertEquals(0, normalExit);
        assertEquals(1, strictExit);
        assertTrue(standardOutput.toString().contains("BREAKING [OPERATION_REMOVED]"));
        assertTrue(errorOutput.toString().contains("strict mode rejects every breaking change"));
    }

    private String baselinePaths() {
        return """
                  /orders/{orderId}/status:
                    get:
                      operationId: getOrderStatus
                      parameters:
                        - name: orderId
                          in: path
                          required: true
                          schema: { type: string }
                      responses:
                        '200':
                          description: Found
                          content:
                            application/json:
                              schema:
                                $ref: '#/components/schemas/OrderStatusResponse'
                """;
    }

    private String baselineSchemas() {
        return """
                    OrderStatusResponse:
                      type: object
                      required: [orderId, status]
                      properties:
                        orderId: { type: string }
                        status:
                          type: string
                          enum: [processing, completed]
                """;
    }

    private Path writeSpec(String name, String version, String paths, String schemas)
            throws Exception {
        return writeSpec(tempDir.resolve(name), version, paths, schemas);
    }

    private Path writeSpec(Path target, String version, String paths, String schemas)
            throws Exception {
        Files.createDirectories(target.getParent());
        Files.writeString(
                target,
                """
                openapi: 3.0.3
                info:
                  title: orders-api
                  version: '%s'
                paths:
                %s
                components:
                  schemas:
                %s
                """
                        .formatted(version, paths.indent(2), schemas.indent(4)));
        return target;
    }
}
