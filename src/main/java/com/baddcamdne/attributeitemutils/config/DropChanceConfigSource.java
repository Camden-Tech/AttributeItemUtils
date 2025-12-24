package com.baddcamdne.attributeitemutils.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

public class DropChanceConfigSource {
    private final double defaultChance;

    public DropChanceConfigSource(double defaultChance) {
        this.defaultChance = defaultChance;
    }

    public double defaultChance() {
        return defaultChance;
    }

    public static DropChanceConfigSource fromConfig(FileConfiguration config) {
        ConfigurationSection section = config.getConfigurationSection("drops");
        DropChanceConfig defaultConfig = DropChanceConfig.fromSection(section, new DropChanceConfig(0.015));

        return new DropChanceConfigSource(defaultConfig.dropChance());
    }
}
