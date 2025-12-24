package com.baddcamden.attributeitemutils.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

public class DropChanceConfigSource {
    private final double defaultChance;

    /**
     * Creates a configuration source that exposes the default drop chance value.
     */
    public DropChanceConfigSource(double defaultChance) {
        this.defaultChance = defaultChance;
    }

    /**
     * Returns the default drop chance configured for loot generation.
     */
    public double defaultChance() {
        return defaultChance;
    }

    /**
     * Loads the drop chance configuration from the plugin configuration file with sensible defaults.
     */
    public static DropChanceConfigSource fromConfig(FileConfiguration config) {
        ConfigurationSection section = config.getConfigurationSection("drops");
        DropChanceConfig defaultConfig = DropChanceConfig.fromSection(section, new DropChanceConfig(0.015));

        return new DropChanceConfigSource(defaultConfig.dropChance());
    }
}
