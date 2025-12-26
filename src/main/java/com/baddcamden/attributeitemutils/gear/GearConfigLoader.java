package com.baddcamden.attributeitemutils.gear;

import me.baddcamden.attributeutils.model.ModifierOperation;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Loads gear kits from configuration files and exposes parsed kit definitions.
 */
public class GearConfigLoader {

    /** Owning plugin used to access resources and log warnings. */
    private final JavaPlugin plugin;
    /** Physical config file path resolved from the plugin data folder. */
    private final File gearConfigFile;
    /** Parsed kit definitions keyed by name. */
    private final Map<String, KitConfig> kits = new HashMap<>();
    /** In-memory YAML configuration loaded from disk and defaults. */
    private YamlConfiguration gearConfig;

    /**
     * Prepares a loader for the plugin's Gear.yml configuration file.
     */
    public GearConfigLoader(JavaPlugin plugin) {
        this.plugin = plugin;
        this.gearConfigFile = new File(plugin.getDataFolder(), "Gear.yml");
    }

    /**
     * Reloads all kit definitions from disk, merging defaults before parsing entries.
     */
    public void reload() {
        kits.clear();
        gearConfig = YamlConfiguration.loadConfiguration(gearConfigFile);
        loadDefaults();
        ConfigurationSection section = gearConfig.getConfigurationSection("kits");
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            ConfigurationSection kitSection = section.getConfigurationSection(key);
            if (kitSection == null) continue;
            double target = kitSection.getDouble("target-weight", 5.0);
            double steepness = kitSection.getDouble("steepness", 1.0);
            double range = kitSection.getDouble("range", 4.0);
            Map<GearSlot, List<WeightedItem>> map = new EnumMap<GearSlot, List<WeightedItem>>(GearSlot.class);
            for (GearSlot slot : GearSlot.values()) {
                List<WeightedItem> entries = kitSection.getStringList(slot.name().toLowerCase())
                        .stream()
                        .map(this::parseWeightedItem)
                        .flatMap(Optional::stream)
                        .toList();
                map.put(slot, entries);
            }
            Map<String, ModifierOperation> operations = parseOperations(
                    kitSection.getConfigurationSection("attribute-operations"));
            kits.put(key, new KitConfig(key, target, steepness, range, map, operations));
        }
    }

    /**
     * Copies bundled Gear.yml defaults into the active configuration so optional values resolve correctly.
     */
    private void loadDefaults() {
        try (InputStream stream = plugin.getResource("Gear.yml")) {
            if (stream == null) {
                return;
            }
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
            gearConfig.setDefaults(defaults);
            gearConfig.options().copyDefaults(true);
        } catch (IOException ex) {
            plugin.getLogger().warning("Unable to load default Gear.yml: " + ex.getMessage());
        }
    }

    /**
     * Parses a weighted material entry in the form "MATERIAL:weight", logging malformed data.
     */
    private Optional<WeightedItem> parseWeightedItem(String raw) {
        String[] parts = raw.split(":");
        if (parts.length != 2) return Optional.empty();
        try {
            Material mat = Material.matchMaterial(parts[0].trim());
            double weight = Double.parseDouble(parts[1]);
            if (mat == null) return Optional.empty();
            return Optional.of(new WeightedItem(mat, weight));
        } catch (NumberFormatException ex) {
            plugin.getLogger().warning("Unable to parse weight entry: " + raw);
            return Optional.empty();
        }
    }

    /**
     * Parses attribute operation overrides from the provided configuration section, normalizing keys
     * and skipping unknown entries.
     */
    private Map<String, ModifierOperation> parseOperations(ConfigurationSection section) {
        if (section == null) {
            return Map.of();
        }

        Map<String, ModifierOperation> operations = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            ModifierOperation operation = parseOperation(section.getString(key));
            if (operation != null) {
                operations.put(normalizeAttributeKey(key), operation);
            }
        }
        return operations;
    }

    /**
     * Attempts to interpret a textual operation value into a {@link ModifierOperation}, accepting
     * legacy aliases like ADD/MULTIPLY when possible.
     */
    private ModifierOperation parseOperation(String raw) {
        if (raw == null) {
            return null;
        }
        String normalized = raw.toUpperCase(Locale.ROOT);
        try {
            if (normalized.equals("ADD") || normalized.equals("ADD_NUMBER")) {
                return ModifierOperation.ADD;
            }
            if (normalized.equals("MULTIPLY")
                    || normalized.equals("ADD_SCALAR")
                    || normalized.equals("MULTIPLY_SCALAR_1")) {
                return ModifierOperation.MULTIPLY;
            }
            return ModifierOperation.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("Unknown attribute operation '" + raw + "' in Gear.yml");
            return null;
        }
    }

    /**
     * Normalizes attribute ids into the lowercased, dot-delimited format used for lookups.
     */
    private String normalizeAttributeKey(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim()
                .toLowerCase(Locale.ROOT)
                .replace(':', '.')
                .replace('-', '_')
                .replace(' ', '_');
    }

    /**
     * Retrieves a kit definition by name when it exists in the current configuration.
     */
    public Optional<KitConfig> getKit(String name) {
        return Optional.ofNullable(kits.get(name));
    }

    /**
     * Exposes all loaded kit definitions keyed by their configuration name.
     */
    public Map<String, KitConfig> getKits() {
        return kits;
    }
}
