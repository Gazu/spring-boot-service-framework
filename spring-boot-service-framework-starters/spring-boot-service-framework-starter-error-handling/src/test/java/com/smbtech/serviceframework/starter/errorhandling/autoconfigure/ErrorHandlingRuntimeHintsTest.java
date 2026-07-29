package com.smbtech.serviceframework.starter.errorhandling.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

class ErrorHandlingRuntimeHintsTest {

    @Test
    void registersOptionalOAuth2DiagnosticMethods() throws Exception {
        RuntimeHints hints = new RuntimeHints();

        new ErrorHandlingRuntimeHints().registerHints(hints, getClass().getClassLoader());

        assertThat(
                        RuntimeHintsPredicates.reflection()
                                .onMethodInvocation(
                                        OAuth2AuthenticationException.class.getMethod("getError")))
                .accepts(hints);
        assertThat(
                        RuntimeHintsPredicates.reflection()
                                .onMethodInvocation(OAuth2Error.class.getMethod("getErrorCode")))
                .accepts(hints);
    }
}
