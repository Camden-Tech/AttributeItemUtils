package com.baddcamdne.attributeitemutils.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

public record DropChanceConfig(double dropChance) {

    public static DropChanceConfig fromConfig(FileConfiguration config) {
        ConfigurationSection section = config.getConfigurationSection("drops");
        return fromSection(section, new DropChanceConfig(0.015));
    }

    public static DropChanceConfig fromSection(ConfigurationSection section, DropChanceConfig defaultConfig) {
        if (section == null) {
            return defaultConfig;
        }
        return new DropChanceConfig(section.getDouble("chance", defaultConfig.dropChance));
    }
}
