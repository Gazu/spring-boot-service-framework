package com.smbtech.serviceframework.gradle.openapi;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import org.gradle.api.Project;
import org.gradle.api.file.FileTree;

final class OpenApiSpecDiscovery {

    private static final List<String> INCLUDES =
            List.of(
                    "**/src/main/openapi/*.yaml",
                    "**/src/main/openapi/*.yml",
                    "**/src/main/openapi/*.json",
                    "**/openapi/*.yaml",
                    "**/openapi/*.yml",
                    "**/openapi/*.json",
                    "**/swagger/*.yaml",
                    "**/swagger/*.yml",
                    "**/swagger/*.json");
    private static final List<String> EXCLUDES =
            List.of("**/.git/**", "**/.gradle/**", "**/build/**");

    private OpenApiSpecDiscovery() {}

    static FileTree candidates(Project project) {
        return project.fileTree(
                project.getRootDir(),
                patterns -> {
                    patterns.include(INCLUDES);
                    patterns.exclude(EXCLUDES);
                });
    }

    static List<File> discover(Project project) {
        return candidates(project).getFiles().stream()
                .sorted(Comparator.comparing(OpenApiSpecDiscovery::normalizedPath))
                .toList();
    }

    static String registrationName(Project project, File source) {
        Path root = normalizedPath(project.getRootDir());
        Path path = normalizedPath(source);
        String relativePath =
                (path.startsWith(root) ? root.relativize(path) : path)
                        .toString()
                        .replace(File.separatorChar, '/');
        int extension = relativePath.lastIndexOf('.');
        String withoutExtension =
                extension > relativePath.lastIndexOf('/')
                        ? relativePath.substring(0, extension)
                        : relativePath;
        StringBuilder name = new StringBuilder("discovered");
        for (String part : withoutExtension.split("[^A-Za-z0-9]+")) {
            if (!part.isEmpty()) {
                name.append(part.substring(0, 1).toUpperCase(Locale.ROOT));
                name.append(part.substring(1));
            }
        }
        return name.append(shortDigest(relativePath)).toString();
    }

    static Path normalizedPath(File file) {
        try {
            return file.getCanonicalFile().toPath();
        } catch (IOException exception) {
            return file.toPath().toAbsolutePath().normalize();
        }
    }

    private static String shortDigest(String value) {
        try {
            byte[] digest =
                    MessageDigest.getInstance("SHA-256")
                            .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 6);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
