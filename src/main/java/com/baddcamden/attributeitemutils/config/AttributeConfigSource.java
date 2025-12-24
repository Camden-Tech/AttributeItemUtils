package com.baddcamden.attributeitemutils.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

public class AttributeConfigSource {
    private final AttributeConfig defaultConfig;

    /**
     * Initializes the source with a default attribute configuration to fall back on when needed.
     */
    public AttributeConfigSource(AttributeConfig defaultConfig) {
        this.defaultConfig = defaultConfig;
    }

    /**
     * Returns the default configuration values for attribute generation.
     */
    public AttributeConfig defaultConfig() {
        return defaultConfig;
    }

    /**
     * Builds a config source from the provided file configuration, applying safe defaults when sections are missing.
     */
    public static AttributeConfigSource fromConfig(FileConfiguration config) {
        ConfigurationSection section = config.getConfigurationSection("attributes");
        AttributeConfig defaultConfig = AttributeConfig.fromSection(section, new AttributeConfig(0.02, 0.0, 0.05, 0.95));

        return new AttributeConfigSource(defaultConfig);
    }
}
