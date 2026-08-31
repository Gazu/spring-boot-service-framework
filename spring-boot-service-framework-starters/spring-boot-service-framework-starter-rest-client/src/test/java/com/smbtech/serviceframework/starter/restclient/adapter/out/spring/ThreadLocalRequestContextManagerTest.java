package com.smbtech.serviceframework.starter.restclient.adapter.out.spring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.smbtech.serviceframework.starter.restclient.api.RequestContext;
import com.smbtech.serviceframework.starter.restclient.api.RequestContextScope;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

class ThreadLocalRequestContextManagerTest {

    private final ThreadLocalRequestContextManager manager = new ThreadLocalRequestContextManager();

    @Test
    void currentReturnsEmptyContextWhenNoScopeIsOpen() {
        assertThat(manager.current()).isEqualTo(RequestContext.empty());
        assertThat(manager.current().isEmpty()).isTrue();
    }

    @Test
    void openMakesContextCurrentUntilScopeIsClosed() {
        RequestContext context =
                RequestContext.builder()
                        .header("X-Customer-Id", "17952397-3")
                        .jwtBearerClaim("customer_id", "17952397-3")
                        .build();

        try (RequestContextScope scope = manager.open(context)) {
            assertThat(scope.context()).isSameAs(context);
            assertThat(scope.isClosed()).isFalse();
            assertThat(manager.current()).isEqualTo(context);
        }

        assertThat(manager.current()).isEqualTo(RequestContext.empty());
    }

    @Test
    void openConsumerBuildsContextForTheScope() {
        try (RequestContextScope scope =
                manager.open(
                        context ->
                                context.header("X-Channel", "mobile")
                                        .jwtBearerClaim("channel", "mobile"))) {

            assertThat(scope.context().headers()).containsExactly(Map.entry("X-Channel", "mobile"));
            assertThat(scope.context().jwtBearerClaims())
                    .containsExactly(Map.entry("channel", "mobile"));
            assertThat(manager.current()).isEqualTo(scope.context());
        }
    }

    @Test
    void ergonomicOpenMethodsCreateFocusedScopes() {
        try (RequestContextScope ignored = manager.openHeader("X-Customer-Id", "17952397-3")) {
            assertThat(manager.currentHeaders())
                    .containsExactly(Map.entry("X-Customer-Id", "17952397-3"));
            assertThat(manager.currentJwtBearerClaims()).isEmpty();
        }

        try (RequestContextScope ignored = manager.openHeaders(Map.of("X-Channel", "mobile"))) {
            assertThat(manager.currentHeaders()).containsExactly(Map.entry("X-Channel", "mobile"));
            assertThat(manager.currentJwtBearerClaims()).isEmpty();
        }

        try (RequestContextScope ignored =
                manager.openJwtBearerClaim("customer_id", "17952397-3")) {
            assertThat(manager.currentHeaders()).isEmpty();
            assertThat(manager.currentJwtBearerClaims())
                    .containsExactly(Map.entry("customer_id", "17952397-3"));
        }

        try (RequestContextScope ignored =
                manager.openJwtBearerClaims(Map.of("channel", "mobile"))) {
            assertThat(manager.currentHeaders()).isEmpty();
            assertThat(manager.currentJwtBearerClaims())
                    .containsExactly(Map.entry("channel", "mobile"));
        }

        try (RequestContextScope ignored =
                manager.open(
                        Map.of("X-Operation-Id", "op-123"), Map.of("operation_id", "op-123"))) {
            assertThat(manager.currentHeaders())
                    .containsExactly(Map.entry("X-Operation-Id", "op-123"));
            assertThat(manager.currentJwtBearerClaims())
                    .containsExactly(Map.entry("operation_id", "op-123"));
        }
    }

    @Test
    void nestedScopesMergeWithInnerValuesTakingPrecedence() {
        RequestContext outer =
                RequestContext.builder()
                        .header("X-Customer-Id", "outer-customer")
                        .header("X-Channel", "web")
                        .jwtBearerClaim("customer_id", "outer-customer")
                        .jwtBearerClaim("channel", "web")
                        .build();
        RequestContext inner =
                RequestContext.builder()
                        .header("X-Channel", "mobile")
                        .header("X-Operation-Id", "op-123")
                        .jwtBearerClaim("channel", "mobile")
                        .jwtBearerClaim("operation_id", "op-123")
                        .build();

        try (RequestContextScope outerScope = manager.open(outer)) {
            assertThat(manager.current()).isEqualTo(outer);

            try (RequestContextScope ignored = manager.open(inner)) {
                assertThat(manager.current().headers())
                        .containsExactly(
                                Map.entry("X-Customer-Id", "outer-customer"),
                                Map.entry("X-Channel", "mobile"),
                                Map.entry("X-Operation-Id", "op-123"));
                assertThat(manager.current().jwtBearerClaims())
                        .containsExactly(
                                Map.entry("customer_id", "outer-customer"),
                                Map.entry("channel", "mobile"),
                                Map.entry("operation_id", "op-123"));
            }

            assertThat(outerScope.isClosed()).isFalse();
            assertThat(manager.current()).isEqualTo(outer);
        }

        assertThat(manager.current()).isEqualTo(RequestContext.empty());
    }

    @Test
    void nestedScopesCanOverrideHeadersAndClaimsIndependently() {
        RequestContext outer =
                RequestContext.builder()
                        .header("X-Customer-Id", "outer-customer")
                        .jwtBearerClaim("customer_id", "outer-customer")
                        .build();
        RequestContext inner =
                RequestContext.builder()
                        .header("X-Customer-Id", "inner-header-customer")
                        .jwtBearerClaim("channel", "mobile")
                        .build();

        try (RequestContextScope ignored = manager.open(outer)) {
            try (RequestContextScope nested = manager.open(inner)) {
                assertThat(nested.context()).isEqualTo(inner);
                assertThat(manager.current().headers())
                        .containsExactly(Map.entry("X-Customer-Id", "inner-header-customer"));
                assertThat(manager.current().jwtBearerClaims())
                        .containsExactly(
                                Map.entry("customer_id", "outer-customer"),
                                Map.entry("channel", "mobile"));
            }
        }
    }

    @Test
    void closeIsIdempotent() {
        RequestContextScope scope =
                manager.open(
                        RequestContext.builder().header("X-Customer-Id", "17952397-3").build());

        scope.close();
        scope.close();

        assertThat(scope.isClosed()).isTrue();
        assertThat(manager.current()).isEqualTo(RequestContext.empty());
    }

    @Test
    void scopesMustBeClosedInLifoOrder() {
        RequestContextScope outer =
                manager.open(RequestContext.builder().header("X-Scope", "outer").build());
        RequestContextScope inner =
                manager.open(RequestContext.builder().header("X-Scope", "inner").build());

        assertThatThrownBy(outer::close)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("RequestContext scopes must be closed in LIFO order");
        assertThat(outer.isClosed()).isFalse();

        inner.close();
        outer.close();

        assertThat(manager.current()).isEqualTo(RequestContext.empty());
    }

    @Test
    void contextIsIsolatedPerThread() throws Exception {
        try (RequestContextScope ignored =
                manager.open(
                        RequestContext.builder().header("X-Customer-Id", "17952397-3").build())) {
            AtomicReference<RequestContext> threadContext = new AtomicReference<>();

            Thread thread = new Thread(() -> threadContext.set(manager.current()));
            thread.start();
            thread.join();

            assertThat(threadContext.get()).isEqualTo(RequestContext.empty());
            assertThat(manager.current().headers())
                    .containsExactly(Map.entry("X-Customer-Id", "17952397-3"));
        }
    }

    @Test
    void rejectsNullContextAndCustomizer() {
        assertThatThrownBy(() -> manager.open((RequestContext) null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("context must not be null");
        assertThatThrownBy(() -> manager.open((Consumer<RequestContext.Builder>) null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("customizer must not be null");
    }
}
