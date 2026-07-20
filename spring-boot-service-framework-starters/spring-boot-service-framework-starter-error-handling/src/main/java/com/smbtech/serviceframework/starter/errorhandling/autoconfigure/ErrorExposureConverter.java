package com.smbtech.serviceframework.starter.errorhandling.autoconfigure;

import com.smbtech.serviceframework.error.ErrorExposure;
import java.util.Locale;
import org.springframework.core.convert.converter.Converter;

final class ErrorExposureConverter implements Converter<String, ErrorExposure> {

    static final String ERROR_MESSAGE =
            "Property 'smbtech.error-handling.response.exposure' must be PUBLIC or INTERNAL";

    @Override
    public ErrorExposure convert(String source) {
        String value = source == null ? "" : source.trim().toUpperCase(Locale.ROOT);
        return switch (value) {
            case "PUBLIC" -> ErrorExposure.PUBLIC;
            case "INTERNAL" -> ErrorExposure.INTERNAL;
            default -> throw new IllegalArgumentException(ERROR_MESSAGE);
        };
    }
}
