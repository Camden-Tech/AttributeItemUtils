package com.baddcamden.attributeitemutils.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Configuration describing how enchant rolls scale over time.
 */
public record EnchantChanceConfig(double baseChance,
                                  int levelBonus,
                                  double maxChance,
                                  double nightBonusMultiplier) {

    /**
     * Loads enchantment chance tuning from the plugin configuration.
     */
    public static EnchantChanceConfig fromConfig(FileConfiguration configuration) {
        ConfigurationSection section = configuration.getConfigurationSection("enchants");
        return fromSection(section, new EnchantChanceConfig(0.02d, 1, 0.95d, 0d));
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
                section.getInt("level-bonus", defaults.levelBonus),
                section.getDouble("max-chance", defaults.maxChance),
                section.getDouble("night-bonus-multiplier", defaults.nightBonusMultiplier)
        );
    }

    /**
     * Computes the enchant chance using the configured base value.
     */
    public double chance() {
        return clampChance(baseChance);
    }

    /**
     * Computes enchant chance after applying a configurable bonus scaled by nights passed.
     */
    public double chance(long nightsPassed) {
        return chance(nightsPassed, nightBonusMultiplier);
    }

    /**
     * Computes enchant chance using a caller-provided nightly multiplier override.
     */
    public double chance(long nightsPassed, double nightlyMultiplier) {
        long safeNightsPassed = Math.max(0L, nightsPassed);
        double safeNightlyMultiplier = Math.max(0d, nightlyMultiplier);
        double chance = baseChance + (safeNightsPassed * safeNightlyMultiplier);
        return clampChance(chance);
    }

    /**
     * Returns the configured bonus applied when rolling enchantment levels.
     */
    public int levelBonus() {
        return (int) Math.max(0, Math.round(levelBonus));
    }

    /**
     * Restricts an enchantment probability to the configured bounds to prevent invalid rolls.
     */
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
