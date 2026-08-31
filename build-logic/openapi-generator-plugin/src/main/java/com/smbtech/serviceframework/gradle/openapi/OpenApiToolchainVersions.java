package com.smbtech.serviceframework.gradle.openapi;

import java.io.IOException;
import java.util.Properties;
import org.gradle.api.GradleException;

record OpenApiToolchainVersions(
        String frameworkVersion, String openApiGeneratorVersion, String springBootVersion) {

    static OpenApiToolchainVersions load() {
        Properties properties = new Properties();
        try (var input =
                OpenApiToolchainVersions.class.getResourceAsStream(
                        "/com/smbtech/serviceframework/gradle/openapi/openapi-toolchain.properties")) {
            if (input == null) {
                throw new GradleException("OpenAPI toolchain version metadata is missing");
            }
            properties.load(input);
        } catch (IOException exception) {
            throw new GradleException("Cannot load OpenAPI toolchain versions", exception);
        }
        return new OpenApiToolchainVersions(
                required(properties, "framework.version"),
                required(properties, "openapi-generator.version"),
                required(properties, "spring-boot.version"));
    }

    private static String required(Properties properties, String name) {
        String value = properties.getProperty(name);
        if (value == null || value.isBlank()) {
            throw new GradleException("OpenAPI toolchain property is missing: " + name);
        }
        return value;
    }
}
