package com.baddcamden.attributeitemutils.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

public class EnchantmentConfigSource {
    private final EnchantmentConfig defaultConfig;

    /**
     * Creates a configuration source that supplies the default enchantment settings.
     */
    public EnchantmentConfigSource(EnchantmentConfig defaultConfig) {
        this.defaultConfig = defaultConfig;
    }

    /**
     * Returns the default enchantment configuration parsed from disk.
     */
    public EnchantmentConfig defaultConfig() {
        return defaultConfig;
    }

    /**
     * Builds an {@link EnchantmentConfigSource} from the plugin configuration, applying defaults when needed.
     */
    public static EnchantmentConfigSource fromConfig(FileConfiguration config) {
        ConfigurationSection section = config.getConfigurationSection("enchants");
        EnchantmentConfig defaultConfig = EnchantmentConfig.fromSection(section, new EnchantmentConfig(0.02, 0.0, 1, 0.95));
        return new EnchantmentConfigSource(defaultConfig);
    }
}
