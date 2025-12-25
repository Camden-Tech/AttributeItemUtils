package com.baddcamden.attributeitemutils.config;

import com.baddcamden.attributeitemutils.util.NightCalculator;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Configuration describing how enchant rolls scale over time.
 */
public record EnchantChanceConfig(double baseChance,
                                  double nightlyIncrease,
                                  int levelBonus,
                                  double maxChance) {

    /**
     * Loads enchantment chance tuning from the plugin configuration.
     */
    public static EnchantChanceConfig fromConfig(FileConfiguration configuration) {
        ConfigurationSection section = configuration.getConfigurationSection("enchants");
        return fromSection(section, new EnchantChanceConfig(0.02d, 0.01d, 1, 0.95d));
    }

    /**
     * Parses chance settings from the provided configuration section.
     */
    public static EnchantChanceConfig fromSection(ConfigurationSection section, EnchantChanceConfig defaults) {
        if (section == null) {
            return defaults;
        }

        return new EnchantChanceConfig(
                section.getDouble("base-chance", defaults.baseChance),
                section.getDouble("nightly-increase", defaults.nightlyIncrease),
                section.getInt("level-bonus", defaults.levelBonus),
                section.getDouble("max-chance", defaults.maxChance)
        );
    }

    /**
     * Computes the enchant chance for the supplied number of nights, clamping to sensible bounds.
     */
    public double chanceForNights(long nights) {
        long clampedNights = NightCalculator.clampNights(nights);
        double chance = baseChance + (nightlyIncrease * clampedNights);
        return clampChance(chance);
    }

    /**
     * Returns the configured bonus applied when rolling enchantment levels.
     */
    public int levelBonus() {
        return (int) Math.max(0, Math.round(levelBonus));
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
