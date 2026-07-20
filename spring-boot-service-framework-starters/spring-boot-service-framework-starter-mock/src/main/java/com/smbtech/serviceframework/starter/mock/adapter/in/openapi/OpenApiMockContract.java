package com.smbtech.serviceframework.starter.mock.adapter.in.openapi;

import java.util.List;

record OpenApiMockContract(String title, String version, List<OpenApiMockOperation> operations) {

    OpenApiMockContract {
        operations = List.copyOf(operations);
    }
}
