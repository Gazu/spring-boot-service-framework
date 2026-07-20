package com.smbtech.serviceframework.openapi.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class OpenApiNameNormalizerTest {

    private final OpenApiNameNormalizer normalizer = new OpenApiNameNormalizer();

    @Test
    void normalizesSupportedTitleShapes() {
        assertEquals("merchant-order-status", normalizer.normalize("Merchant Order Status"));
        assertEquals("merchant-order-status", normalizer.normalize("merchant_order_status"));
        assertEquals("merchant-order-status", normalizer.normalize("Merchant.Order.Status"));
        assertEquals("payments-2-api", normalizer.normalize("Payments 2 API"));
    }

    @Test
    void validatesArtifactBaseNames() {
        assertTrue(normalizer.isValidArtifactBaseName("merchant-order-status"));
        assertFalse(normalizer.isValidArtifactBaseName("-merchant-order-status"));
        assertFalse(normalizer.isValidArtifactBaseName("merchant-order-status-"));
        assertFalse(normalizer.isValidArtifactBaseName(""));
    }

    @Test
    void rejectsInvalidNormalizedTitles() {
        assertThrows(IllegalArgumentException.class, () -> normalizer.normalizeAndValidate("---"));
    }
}
