package com.smbtech.serviceframework.starter.logging;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.smbtech.serviceframework.logging.port.in.StructuredLogger;
import com.smbtech.serviceframework.logging.port.in.StructuredLoggerFactory;
import com.smbtech.serviceframework.logging.port.out.CorrelationContext;
import com.smbtech.serviceframework.logging.port.out.LogEventSink;
import com.smbtech.serviceframework.starter.logging.adapter.out.logback.PolicyAwareAsyncAppender;
import com.smbtech.serviceframework.starter.logging.adapter.out.logback.ServiceFrameworkStructuredLogFormatter;
import java.lang.reflect.Modifier;
import java.util.List;
import org.junit.jupiter.api.Test;

class LoggingStarterEncapsulationTest {

    @Test
    void keepsConsumerAndTechnicalContractsPublic() {
        assertTrue(Modifier.isPublic(StructuredLoggers.class.getModifiers()));
        assertTrue(Modifier.isPublic(StructuredLogger.class.getModifiers()));
        assertTrue(Modifier.isPublic(StructuredLoggerFactory.class.getModifiers()));
        assertTrue(Modifier.isPublic(CorrelationContext.class.getModifiers()));
        assertTrue(Modifier.isPublic(LogEventSink.class.getModifiers()));
        assertTrue(Modifier.isPublic(PolicyAwareAsyncAppender.class.getModifiers()));
        assertTrue(Modifier.isPublic(ServiceFrameworkStructuredLogFormatter.class.getModifiers()));
    }

    @Test
    void hidesRuntimeImplementations() throws ClassNotFoundException {
        for (String className : internalImplementations()) {
            assertFalse(Modifier.isPublic(Class.forName(className).getModifiers()), className);
        }
    }

    @Test
    void removesFormerPublicAdapterTypes() {
        for (String className : removedImplementations()) {
            assertThrows(ClassNotFoundException.class, () -> Class.forName(className), className);
        }
    }

    private List<String> internalImplementations() {
        return List.of(
                "com.smbtech.serviceframework.starter.logging.autoconfigure.AsyncLoggingMetrics",
                "com.smbtech.serviceframework.starter.logging.autoconfigure.MdcCorrelationContext",
                "com.smbtech.serviceframework.starter.logging.autoconfigure.TransactionIdFilter");
    }

    private List<String> removedImplementations() {
        return List.of(
                "com.smbtech.serviceframework.starter.logging.adapter.in.metrics.AsyncLoggingMetrics",
                "com.smbtech.serviceframework.starter.logging.adapter.in.servlet.TransactionIdFilter",
                "com.smbtech.serviceframework.starter.logging.adapter.out.context.MdcCorrelationContext",
                "com.smbtech.serviceframework.starter.logging.adapter.out.slf4j.Slf4jLogEventSink",
                "com.smbtech.serviceframework.starter.logging.adapter.out.slf4j.Slf4jStructuredLoggerFactory");
    }
}
