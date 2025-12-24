package com.baddcamdne.attributeitemutils.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

public class EnchantmentConfigSource {
    private final EnchantmentConfig defaultConfig;

    public EnchantmentConfigSource(EnchantmentConfig defaultConfig) {
        this.defaultConfig = defaultConfig;
    }

    public EnchantmentConfig defaultConfig() {
        return defaultConfig;
    }

    public static EnchantmentConfigSource fromConfig(FileConfiguration config) {
        ConfigurationSection section = config.getConfigurationSection("enchants");
        EnchantmentConfig defaultConfig = EnchantmentConfig.fromSection(section, new EnchantmentConfig(0.02, 0.0, 1, 0.95));
        return new EnchantmentConfigSource(defaultConfig);
    }
}
