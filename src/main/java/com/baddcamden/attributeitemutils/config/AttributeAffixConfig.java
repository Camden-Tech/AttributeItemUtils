package com.baddcamden.attributeitemutils.config;

import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;
import java.util.logging.Logger;

public class AttributeAffixConfig {
    private final List<AttributeAffix> prefixes;
    private final List<AttributeAffix> suffixes;
    private final String defaultPrefix;

    /**
     * Creates a new affix configuration with defined prefixes, suffixes, and fallback prefix text.
     */
    public AttributeAffixConfig(List<AttributeAffix> prefixes, List<AttributeAffix> suffixes, String defaultPrefix) {
        this.prefixes = prefixes;
        this.suffixes = suffixes;
        this.defaultPrefix = defaultPrefix;
    }

    /**
     * Selects prefixes that apply to the provided attributes while attempting to cover multi-attribute options first.
     */
    public PrefixSelection matchingPrefixes(Set<Attribute> attributes) {
        List<AttributeAffix> applicable = applicableAffixes(prefixes, attributes);
        if (applicable.isEmpty()) {
            return new PrefixSelection(List.of(), false);
        }

        List<AttributeAffix> selected = new ArrayList<>();
        Set<Attribute> multiAttributeCoverage = new HashSet<>();
        Set<Attribute> coveredAttributes = new HashSet<>();
        for (AttributeAffix affix : applicable) {
            if (affix.requirementCount() > 1) {
                selected.add(affix);
                multiAttributeCoverage.addAll(affix.attributes());
                coveredAttributes.addAll(affix.attributes());
            } else if (!multiAttributeCoverage.containsAll(affix.attributes())
                    && coveredAttributes.addAll(affix.attributes())) {
                selected.add(affix);
            }
        }

        boolean overflowed = selected.size() > 5;
        if (overflowed) {
            selected = selected.subList(0, 5);
        }
        return new PrefixSelection(selected, overflowed);
    }

    /**
     * Finds the first suffix that satisfies the supplied attributes, if one exists.
     */
    public Optional<AttributeAffix> matchingSuffix(Set<Attribute> attributes) {
        List<AttributeAffix> applicable = applicableAffixes(suffixes, attributes);
        if (applicable.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(applicable.get(0));
    }

    /**
     * Filters and sorts affixes that match the provided attribute set, prioritizing those with more requirements.
     */
    private List<AttributeAffix> applicableAffixes(List<AttributeAffix> candidates, Set<Attribute> attributes) {
        return candidates.stream()
                .filter(affix -> !affix.attributes().isEmpty())
                .filter(affix -> attributes.containsAll(affix.attributes()))
                .sorted(Comparator.comparingInt(AttributeAffix::requirementCount).reversed())
                .toList();
    }

    /**
     * Loads affix data from the AttributeUffixes.yml file, creating it if it does not already exist.
     */
    public static AttributeAffixConfig load(JavaPlugin plugin) {
        File file = new File(plugin.getDataFolder(), "AttributeUffixes.yml");
        if (!file.exists()) {
            plugin.saveResource("AttributeUffixes.yml", false);
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        Logger logger = plugin.getLogger();
        List<AttributeAffix> prefixes = loadSection(config, "prefixes", logger);
        List<AttributeAffix> suffixes = loadSection(config, "suffixes", logger);
        String defaultPrefix = ChatColor.translateAlternateColorCodes('&', config.getString("default-prefix", "Unique "));
        return new AttributeAffixConfig(prefixes, suffixes, defaultPrefix);
    }

    /**
     * Reads and parses a list of affix entries from the specified configuration section.
     */
    private static List<AttributeAffix> loadSection(YamlConfiguration config, String key, Logger logger) {
        List<AttributeAffix> values = new ArrayList<>();
        List<Map<?, ?>> entries = config.getMapList(key);
        for (Map<?, ?> entry : entries) {
            Set<Attribute> attributes = parseAttributes(entry.get("attributes"), logger);
            String value = parseValue(entry.get("value"), logger);
            if (!attributes.isEmpty() && value != null) {
                values.add(new AttributeAffix(attributes, value));
            }
        }
        return values;
    }

    /**
     * Attempts to convert a configuration key to an {@link Attribute}, logging when the value is invalid.
     */
    private static Attribute parseAttribute(String key, Logger logger) {
        try {
            return Attribute.valueOf(key);
        } catch (IllegalArgumentException ex) {
            logger.warning("Unknown attribute in AttributeUffixes.yml: " + key);
            return null;
        }
    }

    /**
     * Extracts attribute requirements from a configuration entry.
     */
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

    /**
     * Parses affix display text while applying color codes and ignoring invalid types.
     */
    private static String parseValue(Object rawValue, Logger logger) {
        if (rawValue == null) {
            return null;
        }
        if (rawValue instanceof String string) {
            return ChatColor.translateAlternateColorCodes('&', string);
        }
        logger.warning("Ignoring affix with non-string value in AttributeUffixes.yml");
        return null;
    }

    /**
     * Retrieves the fallback prefix used when no matching affixes are available.
     */
    public String defaultPrefix() {
        return defaultPrefix;
    }

    public record AttributeAffix(Set<Attribute> attributes, String value) {
        /**
         * Reports how many attribute requirements this affix has.
         */
        public int requirementCount() {
            return attributes.size();
        }
    }

    public record PrefixSelection(List<AttributeAffix> prefixes, boolean overflowed) {
    }
}
