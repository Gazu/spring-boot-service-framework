package com.smbtech.serviceframework.starter.logging.adapter.out.logback;

import com.smbtech.serviceframework.logging.domain.StructuredEvent;
import java.util.Arrays;
import java.util.Optional;

final class StructuredEventExtractor {

    private StructuredEventExtractor() {}

    static Optional<StructuredEvent> from(Object[] arguments) {
        if (arguments == null) {
            return Optional.empty();
        }
        return Arrays.stream(arguments)
                .filter(argument -> argument instanceof StructuredEvent)
                .findFirst()
                .map(StructuredEventExtractor::convert);
    }

    private static StructuredEvent convert(Object argument) {
        return ((StructuredEvent) argument);
    }
}
