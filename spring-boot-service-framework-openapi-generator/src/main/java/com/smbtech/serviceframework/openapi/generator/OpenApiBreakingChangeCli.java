package com.smbtech.serviceframework.openapi.generator;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Provides open api breaking change cli behavior. */
public final class OpenApiBreakingChangeCli {

    private OpenApiBreakingChangeCli() {}

    /**
     * Performs the main operation.
     *
     * @param args args value
     * @throws Exception when the operation cannot be completed
     */
    public static void main(String[] args) throws Exception {
        int exitCode = run(args, System.out, System.err);
        if (exitCode != 0) {
            throw new IllegalStateException("OpenAPI breaking change check failed");
        }
    }

    /**
     * Performs the run operation.
     *
     * @param args args value
     * @param output output value
     * @param error error value
     * @return run result
     * @throws Exception when the operation cannot be completed
     */
    public static int run(String[] args, PrintStream output, PrintStream error) throws Exception {
        Path baselineRoot = null;
        boolean failOnBreaking = false;
        List<Path> currentSpecs = new ArrayList<>();
        for (String argument : args) {
            if (argument.startsWith("--baseline-root=")) {
                baselineRoot = Path.of(argument.substring("--baseline-root=".length()));
            } else if (argument.equals("--fail-on-breaking")) {
                failOnBreaking = true;
            } else {
                currentSpecs.add(Path.of(argument));
            }
        }
        if (baselineRoot == null || currentSpecs.isEmpty()) {
            error.println(
                    "Usage: OpenApiBreakingChangeCli --baseline-root=<directory> "
                            + "[--fail-on-breaking] <current-spec>...");
            return 2;
        }

        OpenApiSpecReader reader = new OpenApiSpecReader();
        OpenApiBaselineResolver resolver = new OpenApiBaselineResolver(reader);
        OpenApiBreakingChangeDetector detector = new OpenApiBreakingChangeDetector();
        List<String> failures = new ArrayList<>();

        for (Path currentSource : currentSpecs.stream().sorted().toList()) {
            OpenApiSpecInfo current = reader.read(currentSource);
            Path exactBaseline = resolver.findExact(baselineRoot, current).orElse(null);
            if (exactBaseline == null) {
                failures.add(
                        current.artifactBaseName()
                                + ": missing baseline snapshot for current version "
                                + current.version()
                                + " under "
                                + baselineRoot.resolve(current.artifactBaseName()));
            } else if (!reader.sha256(exactBaseline).equals(reader.sha256(currentSource))) {
                failures.add(
                        current.artifactBaseName()
                                + ": baseline snapshot "
                                + exactBaseline
                                + " does not match the current "
                                + current.version()
                                + " contract");
            }
            Path baseline = resolver.findLatest(baselineRoot, current).orElse(null);
            if (baseline == null) {
                output.println(
                        "SKIPPED "
                                + current.artifactBaseName()
                                + " "
                                + current.version()
                                + ": no earlier baseline found");
                continue;
            }

            OpenApiCompatibilityReport report = detector.compare(baseline, currentSource);
            output.println(
                    "OpenAPI compatibility "
                            + report.current().artifactBaseName()
                            + ": "
                            + report.baseline().version()
                            + " -> "
                            + report.current().version());
            if (report.changes().isEmpty()) {
                output.println("  NO_CHANGE contract structure is unchanged");
            } else {
                report.changes()
                        .forEach(
                                change ->
                                        output.println(
                                                "  "
                                                        + change.severity()
                                                        + " ["
                                                        + change.code()
                                                        + "] "
                                                        + change.location()
                                                        + ": "
                                                        + change.message()));
            }
            report.versionPolicyViolations()
                    .forEach(
                            violation ->
                                    failures.add(
                                            report.current().artifactBaseName()
                                                    + ": "
                                                    + violation));
            if (report.hasBreakingChanges() && failOnBreaking) {
                failures.add(
                        report.current().artifactBaseName()
                                + ": strict mode rejects every breaking change");
            }
        }

        if (!failures.isEmpty()) {
            error.println("OpenAPI breaking change issues found:");
            failures.forEach(failure -> error.println("- " + failure));
            return 1;
        }
        return 0;
    }
}
