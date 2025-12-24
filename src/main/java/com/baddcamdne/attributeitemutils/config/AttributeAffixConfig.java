package com.baddcamdne.attributeitemutils.config;

import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;
import java.util.logging.Logger;

public class AttributeAffixConfig {
    private final Map<Set<Attribute>, String> prefixes;
    private final Map<Set<Attribute>, String> suffixes;

    public AttributeAffixConfig(Map<Set<Attribute>, String> prefixes, Map<Set<Attribute>, String> suffixes) {
        this.prefixes = prefixes;
        this.suffixes = suffixes;
    }

    public Optional<String> prefixFor(Set<Attribute> attributes) {
        return Optional.ofNullable(prefixes.get(attributes));
    }

    public Optional<String> suffixFor(Set<Attribute> attributes) {
        return Optional.ofNullable(suffixes.get(attributes));
    }

    public static AttributeAffixConfig load(JavaPlugin plugin) {
        File file = new File(plugin.getDataFolder(), "AttributeUffixes.yml");
        if (!file.exists()) {
            plugin.saveResource("AttributeUffixes.yml", false);
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        Logger logger = plugin.getLogger();
        Map<Set<Attribute>, String> prefixes = loadSection(config, "prefixes", logger);
        Map<Set<Attribute>, String> suffixes = loadSection(config, "suffixes", logger);
        return new AttributeAffixConfig(prefixes, suffixes);
    }

    private static Map<Set<Attribute>, String> loadSection(YamlConfiguration config, String key, Logger logger) {
        Map<Set<Attribute>, String> values = new LinkedHashMap<>();
        List<Map<?, ?>> entries = config.getMapList(key);
        for (Map<?, ?> entry : entries) {
            Set<Attribute> attributes = parseAttributes(entry.get("attributes"), logger);
            String value = parseValue(entry.get("value"), logger);
            if (!attributes.isEmpty() && value != null) {
                values.put(attributes, value);
            }
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

    private static Set<Attribute> parseAttributes(Object rawAttributes, Logger logger) {
        if (!(rawAttributes instanceof List<?> attributeList)) {
            return Set.of();
        }
        Set<Attribute> attributes = new LinkedHashSet<>();
        for (Object value : attributeList) {
            if (value instanceof String key) {
                Attribute attribute = parseAttribute(key, logger);
                if (attribute != null) {
                    attributes.add(attribute);
                }
            }
        }
        return attributes;
    }

    private static String parseValue(Object rawValue, Logger logger) {
        if (rawValue == null) {
            return null;
        }
        if (rawValue instanceof String string) {
            return string;
        }
        logger.warning("Ignoring affix with non-string value in AttributeUffixes.yml");
        return null;
    }
}
