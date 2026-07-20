package com.smbtech.serviceframework.starter.restclient.adapter.out.context;

import com.smbtech.serviceframework.starter.restclient.api.RequestContext;
import com.smbtech.serviceframework.starter.restclient.api.RequestContextManager;
import com.smbtech.serviceframework.starter.restclient.api.RequestContextScope;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Provides thread local request context manager behavior. */
public final class ThreadLocalRequestContextManager implements RequestContextManager {
    /** Creates a thread local request context manager instance. */
    public ThreadLocalRequestContextManager() {}

    private final ThreadLocal<Deque<RequestContext>> contexts = new ThreadLocal<>();

    @Override
    public RequestContext current() {
        Deque<RequestContext> stack = contexts.get();
        if (stack == null || stack.isEmpty()) {
            return RequestContext.empty();
        }

        Map<String, String> headers = new LinkedHashMap<>();
        Map<String, Object> jwtBearerClaims = new LinkedHashMap<>();
        stack.descendingIterator()
                .forEachRemaining(
                        context -> {
                            headers.putAll(context.headers());
                            jwtBearerClaims.putAll(context.jwtBearerClaims());
                        });
        return new RequestContext(headers, jwtBearerClaims);
    }

    @Override
    public RequestContextScope open(RequestContext context) {
        RequestContext safeContext = Objects.requireNonNull(context, "context must not be null");
        Deque<RequestContext> stack = contexts.get();
        if (stack == null) {
            stack = new ArrayDeque<>();
            contexts.set(stack);
        }
        stack.push(safeContext);
        return new ThreadLocalRequestContextScope(safeContext);
    }

    private void closeScope(RequestContext context) {
        Deque<RequestContext> stack = contexts.get();
        if (stack == null || stack.isEmpty() || stack.peek() != context) {
            throw new IllegalStateException("RequestContext scopes must be closed in LIFO order");
        }

        stack.pop();
        if (stack.isEmpty()) {
            contexts.remove();
        }
    }

    private final class ThreadLocalRequestContextScope implements RequestContextScope {
        private final RequestContext context;
        private boolean closed;

        private ThreadLocalRequestContextScope(RequestContext context) {
            this.context = context;
        }

        @Override
        public RequestContext context() {
            return context;
        }

        @Override
        public boolean isClosed() {
            return closed;
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }

            closeScope(context);
            closed = true;
        }
    }
}
