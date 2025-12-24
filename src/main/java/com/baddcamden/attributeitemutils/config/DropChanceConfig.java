package com.baddcamden.attributeitemutils.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

public record DropChanceConfig(double dropChance) {

    /**
     * Reads the drop chance configuration from the plugin configuration, supplying defaults when missing.
     */
    public static DropChanceConfig fromConfig(FileConfiguration config) {
        ConfigurationSection section = config.getConfigurationSection("drops");
        return fromSection(section, new DropChanceConfig(0.015));
    }

    /**
     * Creates a {@link DropChanceConfig} from a configuration section or returns the provided default when absent.
     */
    public static DropChanceConfig fromSection(ConfigurationSection section, DropChanceConfig defaultConfig) {
        if (section == null) {
            return defaultConfig;
        }
        return new DropChanceConfig(section.getDouble("chance", defaultConfig.dropChance));
    }
}
