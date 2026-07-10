package com.smbtech.serviceframework.starter.logging.adapter.in.servlet;

import com.smbtech.serviceframework.starter.logging.adapter.out.context.MdcCorrelationContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionalIdFilterTest {

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void propagatesIncomingTransactionIdInsideRequestAndRestoresContext() throws Exception {
        TransactionalIdFilter filter = filter(true, 128);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("X-Transaction-Id", "tx-from-client");
        MDC.put(TransactionalIdFilter.TRANSACTION_ID, "outer");
        AtomicReference<String> insideRequest = new AtomicReference<>();

        filter.doFilter(
                request,
                response,
                (ignoredRequest, ignoredResponse) ->
                        insideRequest.set(MDC.get(TransactionalIdFilter.TRANSACTION_ID))
        );

        assertThat(insideRequest).hasValue("tx-from-client");
        assertThat(response.getHeader("X-Transaction-Id")).isEqualTo("tx-from-client");
        assertThat(MDC.get(TransactionalIdFilter.TRANSACTION_ID)).isEqualTo("outer");
    }

    @Test
    void rejectsInvalidOrUntrustedIncomingId() throws Exception {
        TransactionalIdFilter filter = filter(false, 16);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("X-Transaction-Id", "not trusted!");

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> {
        });

        assertThat(response.getHeader("X-Transaction-Id"))
                .isNotBlank()
                .isNotEqualTo("not trusted!");
        assertThat(MDC.get(TransactionalIdFilter.TRANSACTION_ID)).isNull();
    }

    private TransactionalIdFilter filter(boolean acceptIncoming, int maxLength) {
        return new TransactionalIdFilter(
                "X-Transaction-Id",
                new MdcCorrelationContext(),
                acceptIncoming,
                maxLength
        );
    }
}
