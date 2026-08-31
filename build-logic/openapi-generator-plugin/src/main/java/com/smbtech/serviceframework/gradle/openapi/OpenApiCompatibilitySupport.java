package com.smbtech.serviceframework.gradle.openapi;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class OpenApiCompatibilitySupport {

    private static final Pattern SEMANTIC_VERSION =
            Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)(?:[-+].*)?$");

    private OpenApiCompatibilitySupport() {}

    static Optional<Path> exactBaseline(Path root, OpenApiContractIdentity current)
            throws IOException {
        Path directory = root.resolve(current.artifactBaseName());
        if (!Files.isDirectory(directory)) {
            return Optional.empty();
        }
        try (var paths = Files.list(directory)) {
            return paths.filter(Files::isRegularFile)
                    .filter(OpenApiCompatibilitySupport::isContract)
                    .filter(path -> sameIdentity(path, current))
                    .findFirst();
        }
    }

    static Optional<Path> previousBaseline(Path root, OpenApiContractIdentity current)
            throws IOException {
        Path directory = root.resolve(current.artifactBaseName());
        if (!Files.isDirectory(directory)) {
            return Optional.empty();
        }
        SemanticVersion currentVersion = SemanticVersion.parse(current.version());
        try (var paths = Files.list(directory)) {
            return paths.filter(Files::isRegularFile)
                    .filter(OpenApiCompatibilitySupport::isContract)
                    .map(OpenApiCompatibilitySupport::baseline)
                    .flatMap(Optional::stream)
                    .filter(
                            baseline ->
                                    baseline.identity()
                                            .artifactBaseName()
                                            .equals(current.artifactBaseName()))
                    .filter(baseline -> baseline.version().compareTo(currentVersion) < 0)
                    .max(Comparator.comparing(Baseline::version))
                    .map(Baseline::path);
        }
    }

    static String sha256(Path source) {
        try (InputStream input = Files.newInputStream(source)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                digest.update(buffer, 0, read);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (IOException | NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Cannot calculate SHA-256 for " + source, exception);
        }
    }

    static SemanticVersion version(String value) {
        return SemanticVersion.parse(value);
    }

    private static boolean sameIdentity(Path path, OpenApiContractIdentity current) {
        try {
            OpenApiContractIdentity candidate = OpenApiContractReader.read(path.toFile());
            return candidate.artifactBaseName().equals(current.artifactBaseName())
                    && candidate.version().equals(current.version());
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private static Optional<Baseline> baseline(Path path) {
        try {
            OpenApiContractIdentity identity = OpenApiContractReader.read(path.toFile());
            return Optional.of(
                    new Baseline(path, identity, SemanticVersion.parse(identity.version())));
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    private static boolean isContract(Path path) {
        String name = path.getFileName().toString().toLowerCase(java.util.Locale.ROOT);
        return name.endsWith(".yaml") || name.endsWith(".yml") || name.endsWith(".json");
    }

    private record Baseline(Path path, OpenApiContractIdentity identity, SemanticVersion version) {}

    record SemanticVersion(int major, int minor, int patch) implements Comparable<SemanticVersion> {

        static SemanticVersion parse(String value) {
            Matcher matcher = SEMANTIC_VERSION.matcher(value);
            if (!matcher.matches()) {
                throw new IllegalArgumentException("Invalid semantic version: " + value);
            }
            return new SemanticVersion(
                    Integer.parseInt(matcher.group(1)),
                    Integer.parseInt(matcher.group(2)),
                    Integer.parseInt(matcher.group(3)));
        }

        boolean isMajorIncreaseFrom(SemanticVersion previous) {
            return major > previous.major;
        }

        boolean isMinorOrMajorIncreaseFrom(SemanticVersion previous) {
            return major > previous.major || (major == previous.major && minor > previous.minor);
        }

        @Override
        public int compareTo(SemanticVersion other) {
            int majorResult = Integer.compare(major, other.major);
            if (majorResult != 0) {
                return majorResult;
            }
            int minorResult = Integer.compare(minor, other.minor);
            return minorResult != 0 ? minorResult : Integer.compare(patch, other.patch);
        }
    }
}
