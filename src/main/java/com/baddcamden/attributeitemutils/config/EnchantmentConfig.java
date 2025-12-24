package com.baddcamden.attributeitemutils.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

public record EnchantmentConfig(double baseChance,
                                double nightlyIncrease,
                                int levelBonus,
                                double maxChance) {

    /**
     * Reads enchantment configuration values from the plugin configuration file.
     */
    public static EnchantmentConfig fromConfig(FileConfiguration config) {
        ConfigurationSection section = config.getConfigurationSection("enchants");
        return fromSection(section, new EnchantmentConfig(0.02, 0.0, 1, 0.95));
    }

    /**
     * Parses enchantment configuration values from the provided section, defaulting to supplied values when absent.
     */
    public static EnchantmentConfig fromSection(ConfigurationSection section, EnchantmentConfig defaultConfig) {
        if (section == null) {
            return defaultConfig;
        }
        return new EnchantmentConfig(
                section.getDouble("base-chance", defaultConfig.baseChance),
                section.getDouble("nightly-increase", defaultConfig.nightlyIncrease),
                section.getInt("level-bonus", defaultConfig.levelBonus),
                section.getDouble("max-chance", defaultConfig.maxChance)
        );
    }
}
