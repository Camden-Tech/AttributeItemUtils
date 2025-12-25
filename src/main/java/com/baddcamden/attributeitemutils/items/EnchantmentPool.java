package com.baddcamden.attributeitemutils.items;

import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Loads enchantments from a configured pool and exposes them for random selection.
 */
public class EnchantmentPool {

    private final Plugin plugin;
    private final Logger logger;
    private final File enchantmentPoolFile;

    private List<Enchantment> enchantments = new ArrayList<>();

    public EnchantmentPool(Plugin plugin, Logger logger) {
        this.plugin = plugin;
        this.logger = logger;
        this.enchantmentPoolFile = new File(plugin.getDataFolder(), "EnchantmentPool.yml");
    }

    /**
     * Reloads enchantment entries from disk, merging bundled defaults when present.
     */
    public void reload() {
        enchantments = new ArrayList<>();

        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(enchantmentPoolFile);
        loadDefaults(configuration);

        List<?> entries = configuration.getList("enchants");
        if (entries == null) {
            return;
        }

        entries.stream()
                .map(this::parseEnchantment)
                .flatMap(Optional::stream)
                .forEach(enchantments::add);
    }

    /**
     * Provides the configured enchantments in the pool.
     */
    public List<Enchantment> enchantments() {
        return enchantments;
    }

    private Optional<Enchantment> parseEnchantment(Object raw) {
        if (raw instanceof String rawString) {
            return resolveEnchantment(rawString);
        }
        if (raw instanceof Map<?, ?> rawMap) {
            Object enchantment = rawMap.get("enchantment");
            if (enchantment instanceof String enchantmentString) {
                return resolveEnchantment(enchantmentString);
            }
        }

        return Optional.empty();
    }

    private Optional<Enchantment> resolveEnchantment(String rawKey) {
        NamespacedKey key = NamespacedKey.fromString(rawKey.toLowerCase(Locale.ROOT));
        if (key == null) {
            logger.warning("Invalid enchantment key in EnchantmentPool.yml: " + rawKey);
            return Optional.empty();
        }

        Enchantment enchantment = Enchantment.getByKey(key);
        if (enchantment == null) {
            logger.warning("Unknown enchantment in EnchantmentPool.yml: " + rawKey);
            return Optional.empty();
        }

        return Optional.of(enchantment);
    }

    private void loadDefaults(YamlConfiguration configuration) {
        try (InputStream stream = plugin.getResource("EnchantmentPool.yml")) {
            if (stream == null) {
                return;
            }
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
            configuration.setDefaults(defaults);
            configuration.options().copyDefaults(true);
        } catch (IOException ex) {
            logger.warning("Unable to load default EnchantmentPool.yml: " + ex.getMessage());
        }
    }
}
