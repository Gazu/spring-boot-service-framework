package com.smbtech.serviceframework.project.generator;

import java.io.IOException;
import java.util.Properties;

final class ProjectGeneratorVersions {

    private static final Properties VERSIONS = load();

    private ProjectGeneratorVersions() {}

    static String frameworkVersion() {
        return required("framework.version");
    }

    static String springBootVersion() {
        return required("spring-boot.version");
    }

    static String archUnitVersion() {
        return required("archunit.version");
    }

    private static Properties load() {
        Properties properties = new Properties();
        try (var input =
                ProjectGeneratorVersions.class.getResourceAsStream(
                        "/com/smbtech/serviceframework/project/generator/project-generator.properties")) {
            if (input == null) {
                throw new IllegalStateException("Project generator version metadata is missing");
            }
            properties.load(input);
            return properties;
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Cannot read project generator version metadata", exception);
        }
    }

    private static String required(String key) {
        String value = VERSIONS.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Project generator version metadata is missing " + key);
        }
        return value;
    }
}
