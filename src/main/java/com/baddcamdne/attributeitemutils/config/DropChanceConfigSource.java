package com.baddcamdne.attributeitemutils.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;

import java.util.EnumMap;
import java.util.Map;
import java.util.logging.Logger;

public class DropChanceConfigSource {
    private final double defaultChance;
    private final Map<EntityType, Double> entityChances = new EnumMap<>(EntityType.class);

    public DropChanceConfigSource(double defaultChance) {
        this.defaultChance = defaultChance;
    }

    public DropChanceConfigSource(double defaultChance, Map<EntityType, Double> entityChances) {
        this.defaultChance = defaultChance;
        this.entityChances.putAll(entityChances);
    }

    public double dropChanceFor(EntityType type) {
        return entityChances.getOrDefault(type, defaultChance);
    }

    public static DropChanceConfigSource fromConfig(FileConfiguration config, Logger logger) {
        ConfigurationSection section = config.getConfigurationSection("drops");
        DropChanceConfig defaultConfig = DropChanceConfig.fromSection(section, new DropChanceConfig(0.015));

        Map<EntityType, Double> entityChances = new EnumMap<>(EntityType.class);
        ConfigurationSection entitySection = section == null ? null : section.getConfigurationSection("entities");
        if (entitySection != null) {
            for (String key : entitySection.getKeys(false)) {
                EntityType type = resolveEntityType(key);
                if (type == null) {
                    logger.warning("Unknown entity type in drops.entities: " + key);
                    continue;
                }
                DropChanceConfig overrideConfig = DropChanceConfig.fromSection(entitySection.getConfigurationSection(key), defaultConfig);
                entityChances.put(type, overrideConfig.dropChance());
            }
        }

        return new DropChanceConfigSource(defaultConfig.dropChance(), entityChances);
    }

    private static EntityType resolveEntityType(String key) {
        EntityType type = EntityType.fromName(key);
        if (type != null) {
            return type;
        }
        try {
            return EntityType.valueOf(key.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
