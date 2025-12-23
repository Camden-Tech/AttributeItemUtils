package com.baddcamdne.attributeitemutils.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;

import java.util.EnumMap;
import java.util.Map;
import java.util.logging.Logger;

public class AttributeConfigSource {
    private final AttributeConfig defaultConfig;
    private final Map<EntityType, AttributeConfig> entityConfigs = new EnumMap<>(EntityType.class);

    public AttributeConfigSource(AttributeConfig defaultConfig) {
        this.defaultConfig = defaultConfig;
    }

    public AttributeConfigSource(AttributeConfig defaultConfig, Map<EntityType, AttributeConfig> entityConfigs) {
        this.defaultConfig = defaultConfig;
        this.entityConfigs.putAll(entityConfigs);
    }

    public AttributeConfig configFor(EntityType type) {
        return entityConfigs.getOrDefault(type, defaultConfig);
    }

    public static AttributeConfigSource fromConfig(FileConfiguration config, Logger logger) {
        ConfigurationSection section = config.getConfigurationSection("attributes");
        AttributeConfig defaultConfig = AttributeConfig.fromSection(section, new AttributeConfig(0.02, 0.0, 0.05, 0.95));

        Map<EntityType, AttributeConfig> entityConfigs = new EnumMap<>(EntityType.class);
        ConfigurationSection entitySection = section == null ? null : section.getConfigurationSection("entities");
        if (entitySection != null) {
            for (String key : entitySection.getKeys(false)) {
                EntityType type = resolveEntityType(key);
                if (type == null) {
                    logger.warning("Unknown entity type in attributes.entities: " + key);
                    continue;
                }
                AttributeConfig baseConfig = AttributeConfig.fromSection(entitySection.getConfigurationSection(key), defaultConfig);
                entityConfigs.put(type, baseConfig);
            }
        }

        return new AttributeConfigSource(defaultConfig, entityConfigs);
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
