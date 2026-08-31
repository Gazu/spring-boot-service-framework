package com.smbtech.serviceframework.project.generator;

import java.util.List;

record ContractDescriptor(
        String title,
        String id,
        String version,
        String groupId,
        String artifactId,
        byte[] document,
        List<String> delegateTypes) {

    ContractDescriptor {
        document = document.clone();
        delegateTypes = List.copyOf(delegateTypes);
    }

    @Override
    public byte[] document() {
        return document.clone();
    }

    String coordinate() {
        return groupId + ":" + artifactId + ":" + version;
    }

    List<String> apiPackages() {
        return delegateTypes.stream()
                .map(type -> type.substring(0, type.lastIndexOf('.')))
                .distinct()
                .sorted()
                .toList();
    }
}
