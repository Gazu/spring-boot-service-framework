package com.smbtech.serviceframework.httpclient.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class HttpErrorResponseTest {

    @Test
    void normalizesNullableValuesAndKeepsMetadata() {
        HttpErrorResponse response =
                new HttpErrorResponse(
                        null, null, null, 503, null, null, null, null, null, null, false);

        assertEquals("", response.clientName());
        assertEquals("", response.method());
        assertEquals("", response.uri());
        assertEquals("", response.reasonPhrase());
        assertEquals(HttpErrorCategory.UNKNOWN, response.category());
        assertEquals(Map.of(), response.headers());
        assertEquals("", response.body());
        assertEquals("", response.contentType());
        assertEquals("", response.charset());
        assertFalse(response.bodyTruncated());
    }

    @Test
    void headersAreDefensivelyCopied() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Content-Type", "application/json");

        HttpErrorResponse response =
                new HttpErrorResponse(
                        "payments",
                        "GET",
                        "https://payments.example/v1/orders",
                        400,
                        "Bad Request",
                        HttpErrorCategory.CLIENT_ERROR,
                        headers,
                        "{\"message\":\"invalid\"}",
                        "application/json",
                        "UTF-8",
                        false);

        headers.put("Content-Type", "text/plain");

        assertEquals("application/json", response.headers().get("Content-Type"));
        assertEquals("application/json", response.contentType());
        assertEquals("UTF-8", response.charset());
        assertEquals("{\"message\":\"invalid\"}", response.body());
        assertThrows(
                UnsupportedOperationException.class,
                () -> response.headers().put("Other", "value"));
    }

    @Test
    void keepsBackwardCompatibleConstructor() {
        HttpErrorResponse response =
                new HttpErrorResponse(
                        "payments",
                        "POST",
                        "https://payments.example/v1/orders",
                        500,
                        "Internal Server Error",
                        HttpErrorCategory.SERVER_ERROR,
                        Map.of(),
                        "full response body");

        assertEquals("full response body", response.body());
        assertEquals("", response.contentType());
        assertEquals("", response.charset());
        assertFalse(response.bodyTruncated());
    }
}
