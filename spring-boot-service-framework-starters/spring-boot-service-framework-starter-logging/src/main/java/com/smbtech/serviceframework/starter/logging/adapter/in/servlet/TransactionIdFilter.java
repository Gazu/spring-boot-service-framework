package com.smbtech.serviceframework.starter.logging.adapter.in.servlet;

import com.smbtech.serviceframework.logging.port.out.CorrelationContext;
import com.smbtech.serviceframework.starter.logging.adapter.out.context.MdcCorrelationContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;
import java.util.UUID;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

/** Provides transaction id filter behavior. */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TransactionIdFilter extends OncePerRequestFilter {
    /** MDC key populated with the request transaction identifier. */
    public static final String TRANSACTION_ID = "transactionId";

    private final String headerName;
    private final CorrelationContext correlationContext;
    private final boolean acceptIncoming;
    private final int maxLength;

    /**
     * Creates a transaction id filter instance.
     *
     * @param headerName header name value
     */
    public TransactionIdFilter(String headerName) {
        this(headerName, new MdcCorrelationContext(), true, 128);
    }

    /**
     * Creates a transaction id filter instance.
     *
     * @param headerName header name value
     * @param correlationContext correlation context value
     * @param acceptIncoming accept incoming value
     * @param maxLength max length value
     */
    public TransactionIdFilter(
            String headerName,
            CorrelationContext correlationContext,
            boolean acceptIncoming,
            int maxLength) {
        if (headerName == null || headerName.isBlank()) {
            throw new IllegalArgumentException("headerName may not be null or blank");
        }
        if (maxLength < 1) {
            throw new IllegalArgumentException("maxLength must be greater than zero");
        }
        this.headerName = headerName;
        this.correlationContext =
                Objects.requireNonNull(correlationContext, "correlationContext must not be null");
        this.acceptIncoming = acceptIncoming;
        this.maxLength = maxLength;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String transactionId = acceptIncoming ? request.getHeader(headerName) : null;
        if (!isValid(transactionId)) {
            transactionId = UUID.randomUUID().toString();
        }

        try (CorrelationContext.Scope ignored =
                correlationContext.open(java.util.Map.of(TRANSACTION_ID, transactionId))) {
            response.setHeader(headerName, transactionId);
            filterChain.doFilter(request, response);
        }
    }

    private boolean isValid(String transactionId) {
        if (transactionId == null
                || transactionId.isBlank()
                || transactionId.length() > maxLength) {
            return false;
        }
        for (int index = 0; index < transactionId.length(); index++) {
            char character = transactionId.charAt(index);
            if (!Character.isLetterOrDigit(character)
                    && character != '-'
                    && character != '_'
                    && character != '.'
                    && character != ':') {
                return false;
            }
        }
        return true;
    }
}
