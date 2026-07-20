package com.smbtech.serviceframework.starter.errorhandling.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.smbtech.serviceframework.error.ErrorExposure;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ErrorExposureConverterTest {

    private final ErrorExposureConverter converter = new ErrorExposureConverter();

    @ParameterizedTest
    @CsvSource({"PUBLIC, PUBLIC", "public, PUBLIC", "INTERNAL, INTERNAL", "internal, INTERNAL"})
    void convertsSupportedValues(String source, ErrorExposure expected) {
        assertThat(converter.convert(source)).isEqualTo(expected);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"UNKNOWN", "EXTERNAL", "PUBLIC_INTERNAL"})
    void rejectsUnsupportedValues(String source) {
        assertThatThrownBy(() -> converter.convert(source))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(ErrorExposureConverter.ERROR_MESSAGE);
    }
}
