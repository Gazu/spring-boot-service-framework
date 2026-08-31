package com.smbtech.serviceframework.mock.port.in;

import com.smbtech.serviceframework.mock.domain.MockDefinition;
import com.smbtech.serviceframework.mock.exception.MockException;
import com.smbtech.serviceframework.mock.port.out.MockDefinitionSource;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

final class DefaultMockCatalog implements MockCatalog {

    private final Map<String, MockDefinition> definitions;

    DefaultMockCatalog(MockDefinitionSource source) {
        Map<String, MockDefinition> loaded = new LinkedHashMap<>();
        Objects.requireNonNullElseGet(source.loadDefinitions(), Map::<String, MockDefinition>of)
                .forEach((key, definition) -> register(loaded, key, definition));
        this.definitions = Collections.unmodifiableMap(loaded);
    }

    @Override
    public Optional<MockDefinition> findByKey(String key) {
        return Optional.ofNullable(definitions.get(normalizeKey(key)));
    }

    @Override
    public MockDefinition requireByKey(String key) {
        return findByKey(key)
                .orElseThrow(() -> new MockException("Mock not configured: " + normalizeKey(key)));
    }

    @Override
    public Set<String> keys() {
        return definitions.keySet();
    }

    @Override
    public Map<String, MockDefinition> all() {
        return definitions;
    }

    private void register(
            Map<String, MockDefinition> loaded, String key, MockDefinition definition) {
        if (definition == null) {
            return;
        }

        String normalizedKey = definition.key().isBlank() ? normalizeKey(key) : definition.key();
        if (normalizedKey.isBlank()) {
            return;
        }

        loaded.put(normalizedKey, withKey(normalizedKey, definition));
    }

    private MockDefinition withKey(String key, MockDefinition definition) {
        if (key.equals(definition.key())) {
            return definition;
        }
        return new MockDefinition(key, definition.enabled(), definition.file(), definition.delay());
    }

    private String normalizeKey(String key) {
        return Objects.requireNonNullElse(key, "").trim();
    }
}
