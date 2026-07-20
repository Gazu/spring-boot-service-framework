package com.smbtech.serviceframework.error;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Selects the first supporting resolver by precedence and delegates to a fallback when no
 * specialized resolver matches.
 */
public final class ThrowableErrorResolutionPipeline {

    private static final Comparator<ThrowableErrorResolver> BY_ORDER =
            Comparator.comparingInt(ThrowableErrorResolver::order);

    private final List<ThrowableErrorResolver> resolvers;

    private final ThrowableErrorResolver fallbackResolver;

    /**
     * Creates a pipeline with the default safe fallback.
     *
     * @param resolvers specialized resolvers
     */
    public ThrowableErrorResolutionPipeline(List<? extends ThrowableErrorResolver> resolvers) {
        this(resolvers, new FallbackThrowableErrorResolver());
    }

    /**
     * Creates a pipeline with a custom fallback.
     *
     * @param resolvers specialized resolvers
     * @param fallbackResolver resolver invoked when no specialized resolver matches
     */
    public ThrowableErrorResolutionPipeline(
            List<? extends ThrowableErrorResolver> resolvers,
            ThrowableErrorResolver fallbackResolver) {
        this.resolvers = orderedCopy(resolvers);
        this.fallbackResolver =
                Objects.requireNonNull(fallbackResolver, "fallbackResolver must not be null");
    }

    /**
     * Resolves a failure using the first supporting resolver or the fallback.
     *
     * @param throwable failure to resolve
     * @return resolved error
     */
    public ResolvedError resolve(Throwable throwable) {
        Throwable safeThrowable = Objects.requireNonNull(throwable, "throwable must not be null");
        for (ThrowableErrorResolver resolver : resolvers) {
            if (resolver.supports(safeThrowable)) {
                return requireResolvedError(resolver.resolve(safeThrowable), resolver);
            }
        }
        return requireResolvedError(fallbackResolver.resolve(safeThrowable), fallbackResolver);
    }

    /**
     * Returns the immutable resolver list in effective execution order.
     *
     * @return ordered resolvers
     */
    public List<ThrowableErrorResolver> resolvers() {
        return resolvers;
    }

    /**
     * Returns the configured fallback resolver.
     *
     * @return fallback resolver
     */
    public ThrowableErrorResolver fallbackResolver() {
        return fallbackResolver;
    }

    private static List<ThrowableErrorResolver> orderedCopy(
            List<? extends ThrowableErrorResolver> resolvers) {
        if (resolvers == null || resolvers.isEmpty()) {
            return List.of();
        }
        List<ThrowableErrorResolver> ordered = new ArrayList<>(resolvers.size());
        for (ThrowableErrorResolver resolver : resolvers) {
            ordered.add(Objects.requireNonNull(resolver, "resolver must not be null"));
        }
        ordered.sort(BY_ORDER);
        return List.copyOf(ordered);
    }

    private static ResolvedError requireResolvedError(
            ResolvedError resolvedError, ThrowableErrorResolver resolver) {
        return Objects.requireNonNull(
                resolvedError,
                () -> "Resolver " + resolver.getClass().getName() + " returned null");
    }
}
