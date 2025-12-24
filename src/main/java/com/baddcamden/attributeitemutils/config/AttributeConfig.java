package com.baddcamden.attributeitemutils.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

public record AttributeConfig(double baseChance,
                              double nightlyIncrease,
                              double bonusPercent,
                              double maxChance) {

    /**
     * Loads the attribute configuration from the plugin configuration, providing defaults when the section is absent.
     */
    public static AttributeConfig fromConfig(FileConfiguration config) {
        ConfigurationSection section = config.getConfigurationSection("attributes");
        return fromSection(section, new AttributeConfig(0.02, 0.0, 0.05, 0.95));
    }

    /**
     * Builds an {@link AttributeConfig} from the provided configuration section, falling back to the supplied default when
     * values are missing.
     */
    public static AttributeConfig fromSection(ConfigurationSection section, AttributeConfig defaultConfig) {
        if (section == null) {
            return defaultConfig;
        }
        return new AttributeConfig(
                section.getDouble("base-chance", defaultConfig.baseChance),
                section.getDouble("nightly-increase", defaultConfig.nightlyIncrease),
                section.getDouble("bonus-percent", defaultConfig.bonusPercent),
                section.getDouble("max-chance", defaultConfig.maxChance)
        );
    }
}
