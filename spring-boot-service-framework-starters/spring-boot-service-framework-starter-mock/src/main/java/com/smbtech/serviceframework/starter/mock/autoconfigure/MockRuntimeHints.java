package com.smbtech.serviceframework.starter.mock.autoconfigure;

import com.smbtech.serviceframework.starter.mock.adapter.in.openapi.OpenApiMockEndpoint;
import java.lang.reflect.Method;
import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.context.request.NativeWebRequest;

/** Native-image hints for dynamically registered mock endpoints and conventional resources. */
final class MockRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        Method handler =
                ReflectionUtils.findMethod(
                        OpenApiMockEndpoint.class, "handle", NativeWebRequest.class);
        if (handler != null) {
            hints.reflection().registerMethod(handler, ExecutableMode.INVOKE);
        }
        hints.resources().registerPattern("mock/**");
        hints.resources().registerPattern("mocks/**");
        hints.resources().registerPattern("openapi/**");
    }
}
