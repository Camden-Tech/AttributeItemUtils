package com.baddcamdne.attributeitemutils.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

public record EnchantmentConfig(double baseChance,
                                double nightlyIncrease,
                                int levelBonus,
                                double maxChance) {

    public static EnchantmentConfig fromConfig(FileConfiguration config) {
        ConfigurationSection section = config.getConfigurationSection("enchants");
        if (section == null) {
            return new EnchantmentConfig(0.02, 0.0, 1, 0.95);
        }
        return new EnchantmentConfig(
                section.getDouble("base-chance", 0.02),
                section.getDouble("nightly-increase", 0.0),
                section.getInt("level-bonus", 1),
                section.getDouble("max-chance", 0.95)
        );
    }
}
