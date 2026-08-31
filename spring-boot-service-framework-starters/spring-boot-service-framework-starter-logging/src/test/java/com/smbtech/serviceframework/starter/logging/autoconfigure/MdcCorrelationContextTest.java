package com.smbtech.serviceframework.starter.logging.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import com.smbtech.serviceframework.logging.port.out.CorrelationContext;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class MdcCorrelationContextTest {

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void mergesValuesAndRestoresPreviousContext() {
        MDC.put("traceId", "trace-1");
        MdcCorrelationContext context = new MdcCorrelationContext();

        try (CorrelationContext.Scope ignored = context.open(Map.of("transactionId", "tx-1"))) {
            assertThat(context.snapshot())
                    .containsExactlyInAnyOrderEntriesOf(
                            Map.of(
                                    "traceId", "trace-1",
                                    "transactionId", "tx-1"));
        }

        assertThat(context.snapshot()).containsExactly(Map.entry("traceId", "trace-1"));
    }
}
