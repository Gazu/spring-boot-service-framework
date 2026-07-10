package com.smbtech.examples.logging.adapter.in.http;

import com.smbtech.examples.logging.application.ProjectApplicationService;
import com.smbtech.serviceframework.logging.port.out.CorrelationContext;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dummy")
public class DummyController {
    private static final String TRANSACTION_ID = "transactionId";

    private final ProjectApplicationService projectService;
    private final CorrelationContext correlationContext;
    private final Tracer tracer;

    public DummyController(
            ProjectApplicationService projectService,
            CorrelationContext correlationContext,
            Tracer tracer
    ) {
        this.projectService = projectService;
        this.correlationContext = correlationContext;
        this.tracer = tracer;
    }

    @GetMapping
    public DummyResponse getDummy() {
        Span currentSpan = tracer.currentSpan();
        if (currentSpan == null) {
            throw new IllegalStateException("No active tracing span for HTTP request");
        }

        DummyResponse response = new DummyResponse(
                "ok",
                correlationContext.find(TRANSACTION_ID).orElseThrow(),
                currentSpan.context().traceId(),
                currentSpan.context().spanId()
        );
        projectService.logDummyRequest(
                response.transactionId(),
                response.traceId(),
                response.spanId()
        );
        return response;
    }

    public record DummyResponse(
            String status,
            String transactionId,
            String traceId,
            String spanId
    ) {
    }
}
