package com.smbtech.serviceframework.starter.mock.adapter.in.openapi;

import com.smbtech.serviceframework.starter.mock.autoconfigure.MockProperties;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/** Provides open api mock server registrar behavior. */
public final class OpenApiMockServerRegistrar
        implements SmartInitializingSingleton, DisposableBean {

    private static final Log LOGGER = LogFactory.getLog(OpenApiMockServerRegistrar.class);
    private static final Method HANDLER_METHOD = handlerMethod();

    private final MockProperties.OpenApi properties;
    private final OpenApiMockContractLoader contractLoader;
    private final RequestMappingHandlerMapping handlerMapping;
    private final OpenApiMockEnvironmentGuard environmentGuard;
    private final List<RequestMappingInfo> registeredMappings = new ArrayList<>();

    /**
     * Creates an OpenAPI mock server registrar instance.
     *
     * @param properties properties value
     * @param contractLoader contract loader value
     * @param handlerMapping handler mapping value
     */
    public OpenApiMockServerRegistrar(
            MockProperties.OpenApi properties,
            OpenApiMockContractLoader contractLoader,
            RequestMappingHandlerMapping handlerMapping) {
        this(properties, contractLoader, handlerMapping, new StandardEnvironment());
    }

    /**
     * Creates an OpenAPI mock server registrar with environment protection.
     *
     * @param properties properties value
     * @param contractLoader contract loader value
     * @param handlerMapping handler mapping value
     * @param environment current Spring environment
     */
    public OpenApiMockServerRegistrar(
            MockProperties.OpenApi properties,
            OpenApiMockContractLoader contractLoader,
            RequestMappingHandlerMapping handlerMapping,
            Environment environment) {
        this.properties = Objects.requireNonNull(properties, "properties");
        this.contractLoader = Objects.requireNonNull(contractLoader, "contractLoader");
        this.handlerMapping = Objects.requireNonNull(handlerMapping, "handlerMapping");
        this.environmentGuard = new OpenApiMockEnvironmentGuard(environment);
    }

    @Override
    public void afterSingletonsInstantiated() {
        environmentGuard.validate(properties);
        Map<String, MockProperties.Contract> contracts =
                Objects.requireNonNullElse(properties.getContracts(), Map.of());
        if (contracts.isEmpty()) {
            handleFailure("OpenAPI mock server requires at least one contract", null);
            return;
        }
        Set<String> routes = new HashSet<>();
        contracts.forEach((name, contract) -> registerContract(name, contract, routes));
    }

    private void registerContract(
            String name, MockProperties.Contract contract, Set<String> routes) {
        if (contract == null || !contract.isEnabled()) {
            return;
        }
        List<RequestMappingInfo> contractMappings = new ArrayList<>();
        Set<String> contractRoutes = new HashSet<>();
        try {
            OpenApiMockContract loaded =
                    contractLoader.load(
                            contract.getLocation(),
                            properties.isIncludeOptionalProperties(),
                            contract.getDelay());
            for (OpenApiMockOperation operation : loaded.operations()) {
                String path = joinPaths(contract.getBasePath(), operation.path());
                String route = operation.method() + " " + path;
                if (!routes.add(route)) {
                    throw new IllegalStateException("duplicate OpenAPI mock route " + route);
                }
                contractRoutes.add(route);
                RequestMappingInfo mapping =
                        RequestMappingInfo.paths(path).methods(operation.method()).build();
                handlerMapping.registerMapping(
                        mapping,
                        new OpenApiMockEndpoint(operation, statusHeader()),
                        HANDLER_METHOD);
                registeredMappings.add(mapping);
                contractMappings.add(mapping);
            }
            LOGGER.info(
                    "Registered OpenAPI mock contract "
                            + loaded.title()
                            + " "
                            + loaded.version()
                            + " as "
                            + name);
        } catch (Exception exception) {
            contractMappings.forEach(
                    mapping -> {
                        handlerMapping.unregisterMapping(mapping);
                        registeredMappings.remove(mapping);
                    });
            routes.removeAll(contractRoutes);
            handleFailure("Failed to register OpenAPI mock contract " + name, exception);
        }
    }

    private void handleFailure(String message, Exception exception) {
        if (properties.isFailFast()) {
            throw new IllegalStateException(message, exception);
        }
        if (exception == null) {
            LOGGER.warn(message);
        } else {
            LOGGER.warn(message, exception);
        }
    }

    private String statusHeader() {
        if (!properties.isStatusOverrideEnabled()) {
            return "";
        }
        String value = properties.getStatusHeader();
        return value == null || value.isBlank() ? "X-Mock-Status" : value.trim();
    }

    private static String joinPaths(String basePath, String operationPath) {
        String base = Objects.requireNonNullElse(basePath, "").trim();
        if (base.equals("/")) {
            base = "";
        } else if (!base.isEmpty() && !base.startsWith("/")) {
            base = "/" + base;
        }
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        String path = operationPath.startsWith("/") ? operationPath : "/" + operationPath;
        return base + path;
    }

    private static Method handlerMethod() {
        try {
            return OpenApiMockEndpoint.class.getMethod(
                    "handle", org.springframework.web.context.request.NativeWebRequest.class);
        } catch (NoSuchMethodException exception) {
            throw new IllegalStateException(exception);
        }
    }

    @Override
    public void destroy() {
        registeredMappings.forEach(handlerMapping::unregisterMapping);
        registeredMappings.clear();
    }
}
