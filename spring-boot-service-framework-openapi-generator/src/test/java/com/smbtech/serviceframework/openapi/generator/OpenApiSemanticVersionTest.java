package com.smbtech.serviceframework.openapi.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class OpenApiSemanticVersionTest {

    @Test
    void comparesCoreAndPreReleaseVersionsUsingSemVerPrecedence() {
        assertTrue(version("2.0.0").compareTo(version("1.99.99")) > 0);
        assertTrue(version("1.0.0").compareTo(version("1.0.0-rc.1")) > 0);
        assertTrue(version("1.0.0-rc.10").compareTo(version("1.0.0-rc.2")) > 0);
        assertTrue(version("1.0.0-beta").compareTo(version("1.0.0-1")) > 0);
    }

    @Test
    void ignoresBuildMetadataForPrecedence() {
        assertEquals(0, version("1.0.0+build.1").compareTo(version("1.0.0+build.2")));
    }

    private OpenApiSemanticVersion version(String value) {
        return OpenApiSemanticVersion.parse(value);
    }
}
