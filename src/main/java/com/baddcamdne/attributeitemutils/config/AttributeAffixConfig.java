package com.baddcamdne.attributeitemutils.config;

import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.EnumMap;
import java.util.Map;
import java.util.logging.Logger;

public class AttributeAffixConfig {
    private final Map<Attribute, String> prefixes;
    private final Map<Attribute, String> suffixes;

    public AttributeAffixConfig(Map<Attribute, String> prefixes, Map<Attribute, String> suffixes) {
        this.prefixes = prefixes;
        this.suffixes = suffixes;
    }

    public Map<Attribute, String> prefixes() {
        return prefixes;
    }

    public Map<Attribute, String> suffixes() {
        return suffixes;
    }

    public static AttributeAffixConfig load(JavaPlugin plugin) {
        File file = new File(plugin.getDataFolder(), "AttributeUffixes.yml");
        if (!file.exists()) {
            plugin.saveResource("AttributeUffixes.yml", false);
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        Logger logger = plugin.getLogger();
        Map<Attribute, String> prefixes = loadSection(config.getConfigurationSection("prefixes"), logger);
        Map<Attribute, String> suffixes = loadSection(config.getConfigurationSection("suffixes"), logger);
        return new AttributeAffixConfig(prefixes, suffixes);
    }

    private static Map<Attribute, String> loadSection(ConfigurationSection section, Logger logger) {
        Map<Attribute, String> values = new EnumMap<>(Attribute.class);
        if (section == null) {
            return values;
        }
        for (String key : section.getKeys(false)) {
            Attribute attribute = parseAttribute(key, logger);
            if (attribute == null) {
                continue;
            }
            String value = section.getString(key, "");
            values.put(attribute, value);
        }
        return values;
    }

    private static Attribute parseAttribute(String key, Logger logger) {
        try {
            return Attribute.valueOf(key);
        } catch (IllegalArgumentException ex) {
            logger.warning("Unknown attribute in AttributeUffixes.yml: " + key);
            return null;
        }
    }
}
