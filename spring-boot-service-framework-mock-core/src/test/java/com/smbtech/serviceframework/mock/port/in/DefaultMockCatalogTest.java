package com.smbtech.serviceframework.mock.port.in;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.smbtech.serviceframework.mock.domain.MockDefinition;
import com.smbtech.serviceframework.mock.exception.MockException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DefaultMockCatalogTest {

    @Test
    void loadsDefinitionsAndNormalizesFallbackKeys() {
        Map<String, MockDefinition> definitions = new LinkedHashMap<>();
        definitions.put(
                " payments-success ",
                new MockDefinition(
                        "", true, "classpath:mocks/payments-success.json", Duration.ofMillis(10)));
        definitions.put(
                "ignored-map-key",
                new MockDefinition(
                        "payments-error",
                        true,
                        "classpath:mocks/payments-error.json",
                        Duration.ZERO));

        DefaultMockCatalog catalog = new DefaultMockCatalog(() -> definitions);

        assertEquals(2, catalog.all().size());
        assertTrue(catalog.findByKey("payments-success").isPresent());
        assertTrue(catalog.findByKey(" payments-error ").isPresent());
        assertEquals("payments-success", catalog.requireByKey("payments-success").key());
        assertEquals("payments-error", catalog.requireByKey("payments-error").key());
    }

    @Test
    void returnsEmptyWhenDefinitionDoesNotExist() {
        DefaultMockCatalog catalog = new DefaultMockCatalog(Map::of);

        assertTrue(catalog.findByKey("missing").isEmpty());
        assertThrows(MockException.class, () -> catalog.requireByKey("missing"));
    }

    @Test
    void exposesImmutableCollections() {
        DefaultMockCatalog catalog =
                new DefaultMockCatalog(
                        () ->
                                Map.of(
                                        "payments",
                                        new MockDefinition(
                                                "payments",
                                                true,
                                                "classpath:mocks/payments.json",
                                                Duration.ZERO)));

        assertThrows(UnsupportedOperationException.class, () -> catalog.all().clear());
        assertThrows(UnsupportedOperationException.class, () -> catalog.keys().clear());
        assertFalse(catalog.keys().isEmpty());
    }
}
