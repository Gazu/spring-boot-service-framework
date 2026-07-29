package com.smbtech.serviceframework.starter.restclient.api;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.aop.SpringProxy;
import org.springframework.aop.framework.Advised;
import org.springframework.aot.hint.BindingReflectionHintsRegistrar;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.core.DecoratingProxy;

/** Registers native-image hints for declarative interfaces created by {@link ApiClientFactory}. */
public final class HttpApiClientRuntimeHints {

    private HttpApiClientRuntimeHints() {}

    /**
     * Registers proxy, method, request, and response binding hints for HTTP API interfaces.
     *
     * @param hints target runtime hints
     * @param apiTypes HTTP API interfaces used by the application
     */
    public static void register(RuntimeHints hints, Class<?>... apiTypes) {
        RuntimeHints target = Objects.requireNonNull(hints, "hints must not be null");
        Objects.requireNonNull(apiTypes, "apiTypes must not be null");
        BindingReflectionHintsRegistrar bindingRegistrar = new BindingReflectionHintsRegistrar();
        for (Class<?> apiType : apiTypes) {
            register(target, bindingRegistrar, requireApiType(apiType));
        }
    }

    private static void register(
            RuntimeHints hints,
            BindingReflectionHintsRegistrar bindingRegistrar,
            Class<?> apiType) {
        hints.proxies()
                .registerJdkProxy(apiType, SpringProxy.class, Advised.class, DecoratingProxy.class);
        hints.reflection().registerType(apiType, MemberCategory.INVOKE_PUBLIC_METHODS);
        for (Method method : apiType.getMethods()) {
            bindingRegistrar.registerReflectionHints(
                    hints.reflection(), bindingTypes(method).toArray(Type[]::new));
        }
    }

    private static List<Type> bindingTypes(Method method) {
        List<Type> types = new ArrayList<>();
        types.add(method.getGenericReturnType());
        types.addAll(List.of(method.getGenericParameterTypes()));
        return types;
    }

    private static Class<?> requireApiType(Class<?> apiType) {
        Class<?> type = Objects.requireNonNull(apiType, "apiType must not be null");
        if (!type.isInterface()) {
            throw new IllegalArgumentException("apiType must be an interface: " + type.getName());
        }
        return type;
    }
}
