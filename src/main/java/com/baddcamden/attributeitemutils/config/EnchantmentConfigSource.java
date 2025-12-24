package com.baddcamden.attributeitemutils.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

public class EnchantmentConfigSource {
    private final EnchantmentConfig defaultConfig;

    /**
     * Initializes the source with a default enchantment configuration.
     */
    public EnchantmentConfigSource(EnchantmentConfig defaultConfig) {
        this.defaultConfig = defaultConfig;
    }

    /**
     * Provides the default configuration used when no enchant-specific settings are present.
     */
    public EnchantmentConfig defaultConfig() {
        return defaultConfig;
    }

    /**
     * Creates a configuration source from the supplied file data, falling back to defaults when necessary.
     */
    public static EnchantmentConfigSource fromConfig(FileConfiguration config) {
        ConfigurationSection section = config.getConfigurationSection("enchants");
        EnchantmentConfig defaultConfig = EnchantmentConfig.fromSection(section, new EnchantmentConfig(0.02, 0.0, 1, 0.95));
        return new EnchantmentConfigSource(defaultConfig);
    }
}
