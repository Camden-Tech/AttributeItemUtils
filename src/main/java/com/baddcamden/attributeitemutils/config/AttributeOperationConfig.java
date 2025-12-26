package com.baddcamden.attributeitemutils.config;

import me.baddcamden.attributeutils.model.ModifierOperation;
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

    private final Map<String, ModifierOperation> attributeOperations;
    private final Logger logger;

    /**
     * Wraps configured attribute operations, defaulting to an empty map when no configuration is
     * provided.
     */
    public AttributeOperationConfig(Map<String, ModifierOperation> attributeOperations, Logger logger) {
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
        Map<String, ModifierOperation> parsed = parseOperations(section, logger);
        return new AttributeOperationConfig(parsed, logger);
    }

    /**
     * Parses configured attribute operations from a YAML section while logging unknown values.
     */
    private static Map<String, ModifierOperation> parseOperations(ConfigurationSection section, Logger logger) {
        if (section == null) {
            return Map.of();
        }

        Map<String, ModifierOperation> operations = new LinkedHashMap<>();
        for (String rawKey : section.getKeys(false)) {
            ModifierOperation operation = parseOperation(section.getString(rawKey));
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

    /**
     * Converts a raw string to a {@link ModifierOperation}, accepting common aliases and ignoring
     * invalid entries.
     */
    private static ModifierOperation parseOperation(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            String normalized = raw.toUpperCase(Locale.ROOT);
            if (normalized.equals("ADD") || normalized.equals("ADD_NUMBER")) {
                return ModifierOperation.ADD;
            }
            if (normalized.equals("MULTIPLY")
                    || normalized.equals("ADD_SCALAR")
                    || normalized.equals("MULTIPLY_SCALAR_1")) {
                return ModifierOperation.MULTIPLY;
            }
            return ModifierOperation.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    /**
     * Normalizes attribute identifiers to lower-case and consistent separators for lookups.
     */
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
    public ModifierOperation operationFor(String attributeId) {
        String normalized = normalizeKey(attributeId);
        if (normalized.isBlank()) {
            return ModifierOperation.ADD;
        }
        return Objects.requireNonNullElse(attributeOperations.get(normalized), ModifierOperation.ADD);
    }

    /**
     * Exposes the configured default operations map for external reference.
     */
    public Map<String, ModifierOperation> defaults() {
        return attributeOperations;
    }
}
