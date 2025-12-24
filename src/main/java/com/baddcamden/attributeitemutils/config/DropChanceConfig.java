package com.baddcamden.attributeitemutils.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

public record DropChanceConfig(double dropChance) {

    /**
     * Reads drop chance settings from the plugin configuration.
     */
    public static DropChanceConfig fromConfig(FileConfiguration config) {
        ConfigurationSection section = config.getConfigurationSection("drops");
        return fromSection(section, new DropChanceConfig(0.015));
    }

    /**
     * Parses drop chance settings from a configuration section, defaulting to the provided values when absent.
     */
    public static DropChanceConfig fromSection(ConfigurationSection section, DropChanceConfig defaultConfig) {
        if (section == null) {
            return defaultConfig;
        }
        return new DropChanceConfig(section.getDouble("chance", defaultConfig.dropChance));
    }
}
