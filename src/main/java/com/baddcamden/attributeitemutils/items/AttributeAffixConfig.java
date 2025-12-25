package com.baddcamden.attributeitemutils.items;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Loads prefix and suffix configurations used when naming attributed items and exposes helpers for matching entries
 * against sets of attribute ids.
 */
public class AttributeAffixConfig {

    private static final Comparator<AffixEntry> SPECIFICITY = Comparator
            .comparingInt((AffixEntry entry) -> entry.attributes().size())
            .reversed()
            .thenComparingInt(AffixEntry::order);

    private final JavaPlugin plugin;
    private final File affixFile;
    private final Logger logger;
    private String defaultPrefix = "";
    private final List<AffixEntry> prefixes = new ArrayList<>();
    private final List<AffixEntry> suffixes = new ArrayList<>();

    public AttributeAffixConfig(JavaPlugin plugin, Logger logger) {
        this.plugin = plugin;
        this.logger = logger;
        this.affixFile = new File(plugin.getDataFolder(), "AttributeUffixes.yml");
    }

    /**
     * Reloads the affix configuration from disk, merging any bundled defaults before parsing entries.
     */
    public void reload() {
        prefixes.clear();
        suffixes.clear();

        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(affixFile);
        loadDefaults(configuration);

        defaultPrefix = configuration.getString("default-prefix", "");
        prefixes.addAll(loadEntries(configuration, "prefixes"));
        suffixes.addAll(loadEntries(configuration, "suffixes"));
    }

    private void loadDefaults(YamlConfiguration configuration) {
        try (InputStream stream = plugin.getResource("AttributeUffixes.yml")) {
            if (stream == null) {
                return;
            }
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
            configuration.setDefaults(defaults);
            configuration.options().copyDefaults(true);
        } catch (IOException ex) {
            logger.warning("Unable to load default AttributeUffixes.yml: " + ex.getMessage());
        }
    }

    private List<AffixEntry> loadEntries(YamlConfiguration configuration, String path) {
        List<Map<?, ?>> rawEntries = configuration.getMapList(path);
        List<AffixEntry> entries = new ArrayList<>();
        for (int i = 0; i < rawEntries.size(); i++) {
            Map<?, ?> raw = rawEntries.get(i);
            Object valueObject = raw.get("value");
            if (!(valueObject instanceof String value) || value.isBlank()) {
                logger.warning("Skipping affix entry without a value under " + path + " at index " + i);
                continue;
            }
            Set<String> attributes = normalizeAttributes(raw.get("attributes"));
            entries.add(new AffixEntry(attributes, value, i));
        }
        return entries;
    }

    private Set<String> normalizeAttributes(Object raw) {
        if (!(raw instanceof Collection<?> collection)) {
            return Set.of();
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (Object element : collection) {
            if (element == null) {
                continue;
            }
            String attribute = normalizeAttributeKey(element.toString());
            if (!attribute.isBlank()) {
                normalized.add(attribute);
            }
        }
        return normalized;
    }

    /**
     * Normalizes an attribute key by trimming whitespace, lower-casing, and aligning separators to the dot-delimited
     * format used by attribute definitions.
     */
    public String normalizeAttributeKey(String raw) {
        if (raw == null) {
            return "";
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        return trimmed
                .toLowerCase(Locale.ROOT)
                .replace(':', '.')
                .replace('-', '_')
                .replace(' ', '_');
    }

    /**
     * Returns the configured default prefix string when no specific prefix matches an item's attributes.
     */
    public String defaultPrefix() {
        return defaultPrefix;
    }

    /**
     * Returns prefix entries whose attribute requirements are satisfied by the provided attribute keys.
     */
    public List<AffixEntry> matchingPrefixes(Set<String> attributeKeys) {
        Set<String> normalized = normalizeInput(attributeKeys);
        return prefixes.stream()
                .filter(entry -> normalized.containsAll(entry.attributes()))
                .toList();
    }

    /**
     * Returns the most specific suffix entry satisfied by the provided attribute keys when available.
     */
    public AffixEntry bestSuffix(Set<String> attributeKeys) {
        Set<String> normalized = normalizeInput(attributeKeys);
        return suffixes.stream()
                .filter(entry -> normalized.containsAll(entry.attributes()))
                .sorted(SPECIFICITY)
                .findFirst()
                .orElse(null);
    }

    private Set<String> normalizeInput(Collection<String> keys) {
        Set<String> normalized = new LinkedHashSet<>();
        for (String key : keys) {
            String cleaned = normalizeAttributeKey(key);
            if (!cleaned.isBlank()) {
                normalized.add(cleaned);
            }
        }
        return normalized;
    }

    /**
     * Represents a configured affix entry and its attribute requirements.
     */
    public record AffixEntry(Set<String> attributes, String value, int order) {
    }
}
