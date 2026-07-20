package com.smbtech.serviceframework.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.smbtech.serviceframework.commons.notification.Notification;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ThrowableErrorResolutionPipelineTest {

    @Test
    void ordersResolversAndSelectsTheFirstSupportingResolver() {
        TestResolver later = new TestResolver(100, true, "E_LATER");
        TestResolver first = new TestResolver(-100, true, "E_FIRST");
        TestResolver unsupported = new TestResolver(-200, false, "E_UNSUPPORTED");
        List<ThrowableErrorResolver> source = new ArrayList<>(List.of(later, first, unsupported));

        ThrowableErrorResolutionPipeline pipeline = new ThrowableErrorResolutionPipeline(source);
        source.clear();

        ResolvedError resolvedError = pipeline.resolve(new IllegalArgumentException("invalid"));

        assertEquals(List.of(unsupported, first, later), pipeline.resolvers());
        assertEquals("E_FIRST", resolvedError.notification().code());
        assertEquals(1, unsupported.supportCalls());
        assertEquals(1, first.resolveCalls());
        assertEquals(0, later.supportCalls());
        assertThrows(UnsupportedOperationException.class, () -> pipeline.resolvers().add(first));
    }

    @Test
    void preservesRegistrationOrderWhenResolversHaveTheSameOrder() {
        TestResolver first = new TestResolver(10, true, "E_FIRST");
        TestResolver second = new TestResolver(10, true, "E_SECOND");
        ThrowableErrorResolutionPipeline pipeline =
                new ThrowableErrorResolutionPipeline(List.of(first, second));

        assertEquals("E_FIRST", pipeline.resolve(new RuntimeException()).notification().code());
    }

    @Test
    void delegatesToDefaultFallbackWhenNoResolverSupportsTheFailure() {
        ThrowableErrorResolutionPipeline pipeline =
                new ThrowableErrorResolutionPipeline(
                        List.of(new TestResolver(0, false, "E_UNUSED")));

        ResolvedError resolvedError =
                pipeline.resolve(new IllegalStateException("database unavailable"));

        assertEquals(
                FallbackThrowableErrorResolver.DEFAULT_ERROR_CODE,
                resolvedError.notification().code());
        assertEquals(
                FallbackThrowableErrorResolver.DEFAULT_PUBLIC_MESSAGE,
                resolvedError.notification().message());
        assertEquals(ErrorCategory.INTERNAL, resolvedError.category());
        assertEquals(ErrorExposure.INTERNAL, resolvedError.exposure());
        assertEquals(
                "java.lang.IllegalStateException: database unavailable",
                resolvedError.diagnosticMessage());
        assertInstanceOf(FallbackThrowableErrorResolver.class, pipeline.fallbackResolver());
    }

    @Test
    void supportsAReplaceableFallback() {
        TestResolver fallback = new TestResolver(0, true, "E_CUSTOM_FALLBACK");
        ThrowableErrorResolutionPipeline pipeline =
                new ThrowableErrorResolutionPipeline(List.of(), fallback);

        assertEquals(
                "E_CUSTOM_FALLBACK",
                pipeline.resolve(new RuntimeException()).notification().code());
        assertSame(fallback, pipeline.fallbackResolver());
    }

    @Test
    void propagatesFailureFromTheSelectedResolverWithoutTryingLaterResolvers() {
        IllegalStateException resolverFailure = new IllegalStateException("resolver failed");
        AtomicInteger laterCalls = new AtomicInteger();
        ThrowableErrorResolver failing =
                new ThrowableErrorResolver() {
                    @Override
                    public boolean supports(Throwable throwable) {
                        return true;
                    }

                    @Override
                    public ResolvedError resolve(Throwable throwable) {
                        throw resolverFailure;
                    }

                    @Override
                    public int order() {
                        return -100;
                    }
                };
        ThrowableErrorResolver later =
                new ThrowableErrorResolver() {
                    @Override
                    public boolean supports(Throwable throwable) {
                        laterCalls.incrementAndGet();
                        return true;
                    }

                    @Override
                    public ResolvedError resolve(Throwable throwable) {
                        throw new AssertionError("later resolver must not execute");
                    }
                };
        ThrowableErrorResolutionPipeline pipeline =
                new ThrowableErrorResolutionPipeline(List.of(later, failing));

        IllegalStateException thrown =
                assertThrows(
                        IllegalStateException.class,
                        () -> pipeline.resolve(new RuntimeException("source")));

        assertSame(resolverFailure, thrown);
        assertEquals(0, laterCalls.get());
    }

    @Test
    void rejectsInvalidPipelineResultsAndArguments() {
        ThrowableErrorResolver nullResolver =
                new ThrowableErrorResolver() {
                    @Override
                    public boolean supports(Throwable throwable) {
                        return true;
                    }

                    @Override
                    public ResolvedError resolve(Throwable throwable) {
                        return null;
                    }
                };

        assertThrows(
                NullPointerException.class,
                () ->
                        new ThrowableErrorResolutionPipeline(List.of(nullResolver))
                                .resolve(new RuntimeException()));
        assertThrows(
                NullPointerException.class,
                () -> new ThrowableErrorResolutionPipeline(List.of()).resolve(null));
        assertThrows(
                NullPointerException.class,
                () -> new ThrowableErrorResolutionPipeline(List.of(), null));
    }

    private static final class TestResolver implements ThrowableErrorResolver {
        private final int order;
        private final boolean supported;
        private final String code;
        private final AtomicInteger supportCalls = new AtomicInteger();
        private final AtomicInteger resolveCalls = new AtomicInteger();

        private TestResolver(int order, boolean supported, String code) {
            this.order = order;
            this.supported = supported;
            this.code = code;
        }

        @Override
        public boolean supports(Throwable throwable) {
            supportCalls.incrementAndGet();
            return supported;
        }

        @Override
        public ResolvedError resolve(Throwable throwable) {
            resolveCalls.incrementAndGet();
            return new ResolvedError(
                    Notification.error(code, code),
                    ErrorCategory.INTERNAL,
                    ErrorExposure.PUBLIC,
                    throwable.getMessage());
        }

        @Override
        public int order() {
            return order;
        }

        private int supportCalls() {
            return supportCalls.get();
        }

        private int resolveCalls() {
            return resolveCalls.get();
        }
    }
}
