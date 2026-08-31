package com.smbtech.serviceframework.project.generator;

import java.nio.file.Path;
import java.util.List;

/**
 * Result of a project generation operation.
 *
 * @param directory generated project directory
 * @param contractId normalized contract identifier
 * @param contractVersion contract version
 * @param serverApiCoordinate server API Maven coordinate used by the project
 * @param delegateTypes generated delegate interfaces implemented by inbound adapters
 */
public record GeneratedProject(
        Path directory,
        String contractId,
        String contractVersion,
        String serverApiCoordinate,
        List<String> delegateTypes) {

    /** Makes collection state immutable. */
    public GeneratedProject {
        delegateTypes = List.copyOf(delegateTypes);
    }
}
