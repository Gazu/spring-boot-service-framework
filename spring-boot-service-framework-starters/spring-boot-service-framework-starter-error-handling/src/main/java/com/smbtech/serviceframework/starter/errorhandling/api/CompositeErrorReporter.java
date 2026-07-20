package com.smbtech.serviceframework.starter.errorhandling.api;

import com.smbtech.serviceframework.error.ResolvedError;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/** Invokes multiple error reporters in deterministic order. */
public final class CompositeErrorReporter implements ErrorReporter {

    private final List<ErrorReporter> reporters;

    /**
     * Creates an ordered reporter composition.
     *
     * @param reporters reporters to invoke
     */
    public CompositeErrorReporter(List<? extends ErrorReporter> reporters) {
        if (reporters == null || reporters.isEmpty()) {
            this.reporters = List.of();
            return;
        }
        List<ErrorReporter> ordered = new ArrayList<>(reporters.size());
        for (ErrorReporter reporter : reporters) {
            ordered.add(Objects.requireNonNull(reporter, "reporter must not be null"));
        }
        ordered.sort(Comparator.comparingInt(ErrorReporter::order));
        this.reporters = List.copyOf(ordered);
    }

    @Override
    public void report(Throwable cause, ResolvedError resolvedError, HttpServletRequest request) {
        reportEach(reporter -> reporter.report(cause, resolvedError, request));
    }

    @Override
    public void report(
            Throwable cause,
            ResolvedError resolvedError,
            HttpServletRequest request,
            int statusCode) {
        reportEach(reporter -> reporter.report(cause, resolvedError, request, statusCode));
    }

    private void reportEach(Consumer<ErrorReporter> invocation) {
        RuntimeException firstFailure = null;
        for (ErrorReporter reporter : reporters) {
            try {
                invocation.accept(reporter);
            } catch (RuntimeException failure) {
                if (firstFailure == null) {
                    firstFailure = failure;
                } else {
                    firstFailure.addSuppressed(failure);
                }
            }
        }
        if (firstFailure != null) {
            throw firstFailure;
        }
    }

    /**
     * Returns reporters in effective execution order.
     *
     * @return ordered reporters
     */
    public List<ErrorReporter> reporters() {
        return reporters;
    }
}
