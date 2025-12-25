package com.baddcamden.attributeitemutils.config;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

public class AttributePoolConfig {

    private final List<AttributeBonus> attributes;

    /**
     * Creates a pool configuration containing the provided attribute bonuses.
     */
    public AttributePoolConfig(List<AttributeBonus> attributes) {
        this.attributes = List.copyOf(attributes);
    }

    /**
     * Returns the immutable list of configured attribute bonuses.
     */
    public List<AttributeBonus> attributes() {
        return attributes;
    }

    /**
     * Selects a random attribute bonus from the pool if any are available.
     */
    public Optional<AttributeBonus> random(java.util.Random random) {
        if (attributes.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(attributes.get(random.nextInt(attributes.size())));
    }

    /**
     * Loads attribute pool entries from the AttributePool.yml file, creating it if necessary.
     */
    public static AttributePoolConfig load(JavaPlugin plugin, Logger logger) {
        File file = new File(plugin.getDataFolder(), "AttributePool.yml");
        if (!file.exists()) {
            plugin.saveResource("AttributePool.yml", false);
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        List<Map<?, ?>> entries = config.getMapList("attributes");
        List<AttributeBonus> bonuses = new ArrayList<>();
        for (Map<?, ?> entry : entries) {
            Attribute attribute = parseAttribute(entry.get("attribute"), logger);
            if (attribute == null) {
                continue;
            }
            double bonusPercent = parseBonus(entry.get("bonus-percent"), logger);
            AttributeModifier.Operation operation = parseOperation(entry.get("operation"), logger);
            double baseline = parseBaseline(entry.get("baseline"), logger);
            bonuses.add(new AttributeBonus(attribute, bonusPercent, operation, baseline));
        }

        return new AttributePoolConfig(Collections.unmodifiableList(bonuses));
    }

    /**
     * Converts a raw configuration value into an {@link Attribute}, logging any invalid entries.
     */
    private static Attribute parseAttribute(Object rawAttribute, Logger logger) {
        if (!(rawAttribute instanceof String value)) {
            return null;
        }
        try {
            return Attribute.valueOf(value);
        } catch (IllegalArgumentException ex) {
            logger.warning("Unknown attribute in AttributePool.yml: " + value);
            return null;
        }
    }

    /**
     * Parses the bonus percentage from a configuration entry, logging invalid values.
     */
    private static double parseBonus(Object rawBonus, Logger logger) {
        if (rawBonus instanceof Number number) {
            return number.doubleValue();
        }
        if (rawBonus instanceof String value) {
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException ex) {
                logger.warning("Invalid bonus-percent in AttributePool.yml: " + value);
            }
        }
        return 0.0;
    }

    /**
     * Parses the attribute modifier operation from configuration, defaulting to MULTIPLY_SCALAR_1 on failure.
     */
    private static AttributeModifier.Operation parseOperation(Object rawOperation, Logger logger) {
        if (rawOperation instanceof String value) {
            try {
                return AttributeModifier.Operation.valueOf(value);
            } catch (IllegalArgumentException ex) {
                logger.warning("Invalid operation in AttributePool.yml: " + value);
            }
        }
        return AttributeModifier.Operation.MULTIPLY_SCALAR_1;
    }

    /**
     * Parses a baseline modifier amount, returning zero for invalid or missing values.
     */
    private static double parseBaseline(Object rawBaseline, Logger logger) {
        if (rawBaseline instanceof Number number) {
            return number.doubleValue();
        }
        if (rawBaseline instanceof String value) {
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException ex) {
                logger.warning("Invalid baseline in AttributePool.yml: " + value);
            }
        }
        return 0.0;
    }
}
