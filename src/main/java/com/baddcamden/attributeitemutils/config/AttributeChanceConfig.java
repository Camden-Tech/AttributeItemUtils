package com.baddcamden.attributeitemutils.config;

import com.baddcamden.attributeitemutils.util.NightCalculator;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Configuration describing how attribute roll chances scale over time.
 */
public record AttributeChanceConfig(double baseChance,
                                    double nightlyIncrease,
                                    double bonusPercent,
                                    double maxChance) {

    /**
     * Loads attribute chance tuning from the plugin configuration.
     */
    public static AttributeChanceConfig fromConfig(FileConfiguration configuration) {
        ConfigurationSection section = configuration.getConfigurationSection("attributes");
        return fromSection(section, new AttributeChanceConfig(0.02d, 0.01d, 0.05d, 0.95d));
    }

    /**
     * Parses chance settings from the provided configuration section.
     */
    public static AttributeChanceConfig fromSection(ConfigurationSection section, AttributeChanceConfig defaults) {
        if (section == null) {
            return defaults;
        }

        return new AttributeChanceConfig(
                section.getDouble("base-chance", defaults.baseChance),
                section.getDouble("nightly-increase", defaults.nightlyIncrease),
                section.getDouble("bonus-percent", defaults.bonusPercent),
                section.getDouble("max-chance", defaults.maxChance)
        );
    }

    /**
     * Computes the attribute chance for the supplied number of nights, clamping to sensible bounds.
     */
    public double chanceForNights(long nights) {
        long clampedNights = NightCalculator.clampNights(nights);
        double chance = baseChance + (nightlyIncrease * clampedNights) + bonusPercent;
        return clampChance(chance);
    }

    private double clampChance(double chance) {
        if (chance < 0d) {
            return 0d;
        }
        if (chance > maxChance) {
            return maxChance;
        }
        return chance;
    }
}
