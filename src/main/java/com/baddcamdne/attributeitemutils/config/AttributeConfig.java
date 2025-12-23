package com.baddcamdne.attributeitemutils.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

public record AttributeConfig(double baseChance,
                              double nightlyIncrease,
                              double bonusPercent,
                              double maxChance) {

    public static AttributeConfig fromConfig(FileConfiguration config) {
        ConfigurationSection section = config.getConfigurationSection("attributes");
        if (section == null) {
            return new AttributeConfig(0.02, 0.0, 0.05, 0.95);
        }
        return new AttributeConfig(
                section.getDouble("base-chance", 0.02),
                section.getDouble("nightly-increase", 0.0),
                section.getDouble("bonus-percent", 0.05),
                section.getDouble("max-chance", 0.95)
        );
    }
}
