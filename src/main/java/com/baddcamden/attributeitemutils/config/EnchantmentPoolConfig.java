package com.baddcamden.attributeitemutils.config;

import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

public class EnchantmentPoolConfig {

    private final List<Enchantment> enchantments;

    /**
     * Creates a pool configuration with the supplied enchantments.
     */
    public EnchantmentPoolConfig(List<Enchantment> enchantments) {
        this.enchantments = List.copyOf(enchantments);
    }

    /**
     * Selects a random enchantment from the pool, if any are configured.
     */
    public Optional<Enchantment> random(java.util.Random random) {
        if (enchantments.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(enchantments.get(random.nextInt(enchantments.size())));
    }

    /**
     * Returns the configured list of enchantments.
     */
    public List<Enchantment> enchantments() {
        return enchantments;
    }

    /**
     * Loads enchantments from EnchantmentPool.yml, creating the file when it does not exist.
     */
    public static EnchantmentPoolConfig load(JavaPlugin plugin, Logger logger) {
        File file = new File(plugin.getDataFolder(), "EnchantmentPool.yml");
        if (!file.exists()) {
            plugin.saveResource("EnchantmentPool.yml", false);
        }

        org.bukkit.configuration.file.YamlConfiguration config = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);
        List<Map<?, ?>> entries = config.getMapList("enchants");
        List<Enchantment> enchantments = new ArrayList<>();
        for (Map<?, ?> entry : entries) {
            Enchantment enchantment = parseEnchantment(entry.get("enchantment"), logger);
            if (enchantment != null) {
                enchantments.add(enchantment);
            }
        }

        return new EnchantmentPoolConfig(Collections.unmodifiableList(enchantments));
    }

    /**
     * Resolves an enchantment name or key to a Bukkit {@link Enchantment}, logging a warning for unknown values.
     */
    private static Enchantment parseEnchantment(Object rawEnchantment, Logger logger) {
        if (!(rawEnchantment instanceof String value)) {
            return null;
        }
        NamespacedKey key = NamespacedKey.fromString(value.toLowerCase());
        Enchantment enchantment = key == null ? null : Enchantment.getByKey(key);
        if (enchantment == null) {
            enchantment = Enchantment.getByName(value.toUpperCase());
        }
        if (enchantment == null) {
            logger.warning("Unknown enchantment in EnchantmentPool.yml: " + value);
        }
        return enchantment;
    }
}
