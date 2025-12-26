package com.baddcamden.attributeitemutils.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Provides drop chance defaults sourced from configuration for use when applying kits.
 */
public class DropChanceConfigSource {
    private final double defaultChance;

    /**
     * Creates a configuration source with the given default drop chance.
     */
    public DropChanceConfigSource(double defaultChance) {
        this.defaultChance = defaultChance;
    }

    /**
     * Returns the default chance used when no drop configuration is available.
     */
    public double defaultChance() {
        return defaultChance;
    }

    /**
     * Builds a configuration source from the plugin configuration, applying defaults when necessary.
     */
    public static DropChanceConfigSource fromConfig(FileConfiguration config) {
        ConfigurationSection section = config.getConfigurationSection("drops");
        DropChanceConfig defaultConfig = DropChanceConfig.fromSection(section, new DropChanceConfig(0.015));

        return new DropChanceConfigSource(defaultConfig.dropChance());
    }
}
