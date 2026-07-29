package com.smbtech.serviceframework.starter.errorhandling.autoconfigure;

import java.util.List;
import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/** Native-image hints for optional OAuth2 diagnostics accessed without a hard dependency. */
final class ErrorHandlingRuntimeHints implements RuntimeHintsRegistrar {

    private static final String OAUTH2_AUTHENTICATION_EXCEPTION =
            "org.springframework.security.oauth2.core.OAuth2AuthenticationException";
    private static final String OAUTH2_ERROR =
            "org.springframework.security.oauth2.core.OAuth2Error";

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        hints.reflection()
                .registerTypeIfPresent(
                        classLoader,
                        OAUTH2_AUTHENTICATION_EXCEPTION,
                        type -> type.withMethod("getError", List.of(), ExecutableMode.INVOKE));
        hints.reflection()
                .registerTypeIfPresent(
                        classLoader,
                        OAUTH2_ERROR,
                        type -> type.withMethod("getErrorCode", List.of(), ExecutableMode.INVOKE));
    }
}
