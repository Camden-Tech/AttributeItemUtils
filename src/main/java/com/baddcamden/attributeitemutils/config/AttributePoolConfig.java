package com.baddcamden.attributeitemutils.config;

import org.bukkit.attribute.Attribute;
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
     * Creates a pool configuration with the provided attribute bonuses.
     */
    public AttributePoolConfig(List<AttributeBonus> attributes) {
        this.attributes = List.copyOf(attributes);
    }

    /**
     * Returns an immutable view of all configured attribute bonuses.
     */
    public List<AttributeBonus> attributes() {
        return attributes;
    }

    /**
     * Picks a random attribute bonus from the pool when available.
     */
    public Optional<AttributeBonus> random(java.util.Random random) {
        if (attributes.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(attributes.get(random.nextInt(attributes.size())));
    }

    /**
     * Loads attribute bonuses from the AttributePool.yml file, creating the file when absent.
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
            bonuses.add(new AttributeBonus(attribute, bonusPercent));
        }

        return new AttributePoolConfig(Collections.unmodifiableList(bonuses));
    }

    /**
     * Parses a string or namespaced attribute key into a Bukkit {@link Attribute} while logging invalid values.
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
     * Attempts to parse a numeric bonus percent value from configuration input.
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
}
