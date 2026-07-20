package com.smbtech.serviceframework.openapi.generator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;

/** Provides open api baseline resolver behavior. */
public final class OpenApiBaselineResolver {

    private final OpenApiSpecReader specReader;

    /** Creates an OpenAPI baseline resolver instance. */
    public OpenApiBaselineResolver() {
        this(new OpenApiSpecReader());
    }

    /**
     * Creates an OpenAPI baseline resolver instance.
     *
     * @param specReader spec reader value
     */
    public OpenApiBaselineResolver(OpenApiSpecReader specReader) {
        this.specReader = Objects.requireNonNull(specReader, "specReader");
    }

    /**
     * Finds latest.
     *
     * @param baselineRoot baseline root value
     * @param current current value
     * @return find latest result
     * @throws IOException when the operation cannot be completed
     */
    public Optional<Path> findLatest(Path baselineRoot, OpenApiSpecInfo current)
            throws IOException {
        Path contractDirectory =
                Objects.requireNonNull(baselineRoot, "baselineRoot")
                        .resolve(current.artifactBaseName());
        if (!Files.isDirectory(contractDirectory)) {
            return Optional.empty();
        }
        OpenApiSemanticVersion currentVersion = OpenApiSemanticVersion.parse(current.version());
        try (var files = Files.list(contractDirectory)) {
            return files.filter(Files::isRegularFile)
                    .filter(OpenApiBaselineResolver::isOpenApiFile)
                    .map(path -> readCandidate(path, current, currentVersion))
                    .filter(Optional::isPresent)
                    .map(Optional::orElseThrow)
                    .max(
                            Comparator.comparing(
                                    candidate ->
                                            OpenApiSemanticVersion.parse(
                                                    candidate.info().version())))
                    .map(BaselineCandidate::source);
        }
    }

    /**
     * Finds exact.
     *
     * @param baselineRoot baseline root value
     * @param current current value
     * @return find exact result
     * @throws IOException when the operation cannot be completed
     */
    public Optional<Path> findExact(Path baselineRoot, OpenApiSpecInfo current) throws IOException {
        Path contractDirectory =
                Objects.requireNonNull(baselineRoot, "baselineRoot")
                        .resolve(current.artifactBaseName());
        if (!Files.isDirectory(contractDirectory)) {
            return Optional.empty();
        }
        try (var files = Files.list(contractDirectory)) {
            return files.filter(Files::isRegularFile)
                    .filter(OpenApiBaselineResolver::isOpenApiFile)
                    .filter(path -> hasExactIdentity(path, current))
                    .findFirst();
        }
    }

    private Optional<BaselineCandidate> readCandidate(
            Path source, OpenApiSpecInfo current, OpenApiSemanticVersion currentVersion) {
        try {
            OpenApiSpecInfo candidate = specReader.read(source);
            if (!candidate.artifactBaseName().equals(current.artifactBaseName())) {
                return Optional.empty();
            }
            OpenApiSemanticVersion candidateVersion =
                    OpenApiSemanticVersion.parse(candidate.version());
            return candidateVersion.compareTo(currentVersion) < 0
                    ? Optional.of(new BaselineCandidate(source, candidate))
                    : Optional.empty();
        } catch (IOException exception) {
            throw new IllegalStateException("cannot read OpenAPI baseline " + source, exception);
        }
    }

    private boolean hasExactIdentity(Path source, OpenApiSpecInfo current) {
        try {
            OpenApiSpecInfo candidate = specReader.read(source);
            return candidate.artifactBaseName().equals(current.artifactBaseName())
                    && candidate.version().equals(current.version());
        } catch (IOException exception) {
            throw new IllegalStateException("cannot read OpenAPI baseline " + source, exception);
        }
    }

    private static boolean isOpenApiFile(Path path) {
        String name = path.getFileName().toString().toLowerCase(java.util.Locale.ROOT);
        return name.endsWith(".yaml") || name.endsWith(".yml") || name.endsWith(".json");
    }

    private record BaselineCandidate(Path source, OpenApiSpecInfo info) {}
}
