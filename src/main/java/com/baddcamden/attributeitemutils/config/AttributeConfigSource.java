package com.baddcamden.attributeitemutils.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

public class AttributeConfigSource {
    private final AttributeConfig defaultConfig;

    /**
     * Creates a configuration source that supplies a default attribute configuration.
     */
    public AttributeConfigSource(AttributeConfig defaultConfig) {
        this.defaultConfig = defaultConfig;
    }

    /**
     * Returns the default attribute configuration derived from disk.
     */
    public AttributeConfig defaultConfig() {
        return defaultConfig;
    }

    /**
     * Builds an {@link AttributeConfigSource} by reading the attribute configuration section with fallbacks.
     */
    public static AttributeConfigSource fromConfig(FileConfiguration config) {
        ConfigurationSection section = config.getConfigurationSection("attributes");
        AttributeConfig defaultConfig = AttributeConfig.fromSection(section, new AttributeConfig(0.02, 0.0, 0.05, 0.95));

        return new AttributeConfigSource(defaultConfig);
    }
}
