package com.baddcamden.attributeitemutils.config;

import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Provides configured default modifier operations for attributes, allowing gear rolls to request
 * additive or multiplicative application explicitly.
 */
public class AttributeOperationConfig {

    private final Map<String, AttributeModifier.Operation> attributeOperations;
    private final Logger logger;

    public AttributeOperationConfig(Map<String, AttributeModifier.Operation> attributeOperations, Logger logger) {
        this.attributeOperations = attributeOperations == null
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(attributeOperations);
        this.logger = logger;
    }

    /**
     * Loads configured operations from the plugin configuration.
     */
    public static AttributeOperationConfig fromConfig(FileConfiguration configuration, Logger logger) {
        ConfigurationSection section = configuration.getConfigurationSection("attribute-operations");
        Map<String, AttributeModifier.Operation> parsed = parseOperations(section, logger);
        return new AttributeOperationConfig(parsed, logger);
    }

    private static Map<String, AttributeModifier.Operation> parseOperations(ConfigurationSection section, Logger logger) {
        if (section == null) {
            return Map.of();
        }

        Map<String, AttributeModifier.Operation> operations = new LinkedHashMap<>();
        for (String rawKey : section.getKeys(false)) {
            AttributeModifier.Operation operation = parseOperation(section.getString(rawKey));
            if (operation == null) {
                if (logger != null) {
                    logger.warning("Unknown attribute operation for " + rawKey + ": " + section.getString(rawKey));
                }
                continue;
            }
            operations.put(normalizeKey(rawKey), operation);
        }
        return operations;
    }

    private static AttributeModifier.Operation parseOperation(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return AttributeModifier.Operation.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static String normalizeKey(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim()
                .toLowerCase(Locale.ROOT)
                .replace(':', '.')
                .replace('-', '_')
                .replace(' ', '_');
    }

    /**
     * Looks up the configured operation for the provided attribute id, defaulting to additive when
     * no explicit mapping exists.
     */
    public AttributeModifier.Operation operationFor(String attributeId) {
        String normalized = normalizeKey(attributeId);
        if (normalized.isBlank()) {
            return AttributeModifier.Operation.ADD_NUMBER;
        }
        return Objects.requireNonNullElse(attributeOperations.get(normalized), AttributeModifier.Operation.ADD_NUMBER);
    }

    public Map<String, AttributeModifier.Operation> defaults() {
        return attributeOperations;
    }
}
