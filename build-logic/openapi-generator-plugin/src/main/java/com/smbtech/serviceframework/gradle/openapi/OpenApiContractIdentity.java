package com.smbtech.serviceframework.gradle.openapi;

/**
 * Validated identity read from an OpenAPI document.
 *
 * @param title contract title
 * @param version contract version
 * @param artifactBaseName normalized artifact base name
 */
record OpenApiContractIdentity(String title, String version, String artifactBaseName) {}
