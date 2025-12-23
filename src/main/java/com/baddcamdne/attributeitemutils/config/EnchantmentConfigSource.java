package com.baddcamdne.attributeitemutils.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;

import java.util.EnumMap;
import java.util.Map;
import java.util.logging.Logger;

public class EnchantmentConfigSource {
    private final EnchantmentConfig defaultConfig;
    private final Map<EntityType, EnchantmentConfig> entityConfigs = new EnumMap<>(EntityType.class);

    public EnchantmentConfigSource(EnchantmentConfig defaultConfig) {
        this.defaultConfig = defaultConfig;
    }

    public EnchantmentConfigSource(EnchantmentConfig defaultConfig, Map<EntityType, EnchantmentConfig> entityConfigs) {
        this.defaultConfig = defaultConfig;
        this.entityConfigs.putAll(entityConfigs);
    }

    public EnchantmentConfig configFor(EntityType type) {
        return entityConfigs.getOrDefault(type, defaultConfig);
    }

    public static EnchantmentConfigSource fromConfig(FileConfiguration config, Logger logger) {
        ConfigurationSection section = config.getConfigurationSection("enchants");
        EnchantmentConfig defaultConfig = EnchantmentConfig.fromSection(section, new EnchantmentConfig(0.02, 0.0, 1, 0.95));

        Map<EntityType, EnchantmentConfig> entityConfigs = new EnumMap<>(EntityType.class);
        ConfigurationSection entitySection = section == null ? null : section.getConfigurationSection("entities");
        if (entitySection != null) {
            for (String key : entitySection.getKeys(false)) {
                EntityType type = resolveEntityType(key);
                if (type == null) {
                    logger.warning("Unknown entity type in enchants.entities: " + key);
                    continue;
                }
                EnchantmentConfig baseConfig = EnchantmentConfig.fromSection(entitySection.getConfigurationSection(key), defaultConfig);
                entityConfigs.put(type, baseConfig);
            }
        }

        return new EnchantmentConfigSource(defaultConfig, entityConfigs);
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
