package com.baddcamden.attributeitemutils.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

public class AttributeConfigSource {
    private final AttributeConfig defaultConfig;

    public AttributeConfigSource(AttributeConfig defaultConfig) {
        this.defaultConfig = defaultConfig;
    }

    public AttributeConfig defaultConfig() {
        return defaultConfig;
    }

    public static AttributeConfigSource fromConfig(FileConfiguration config) {
        ConfigurationSection section = config.getConfigurationSection("attributes");
        AttributeConfig defaultConfig = AttributeConfig.fromSection(section, new AttributeConfig(0.02, 0.0, 0.05, 0.95));

        return new AttributeConfigSource(defaultConfig);
    }
}
