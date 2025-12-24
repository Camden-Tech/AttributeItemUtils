package com.baddcamden.attributeitemutils.gear;

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
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class GearConfigLoader {

    /** Parent plugin used to resolve configuration paths and log warnings. */
    private final JavaPlugin plugin;
    /** Gear configuration file on disk that backs the in-memory representation. */
    private final File gearConfigFile;
    /** Cached kit definitions keyed by kit name to avoid reparsing. */
    private final Map<String, KitConfig> kits = new HashMap<>();
    /** Parsed YAML configuration including defaults from the bundled resource. */
    private YamlConfiguration gearConfig;

    /**
     * Creates a loader bound to the given plugin's data directory.
     */
    public GearConfigLoader(JavaPlugin plugin) {
        this.plugin = plugin;
        this.gearConfigFile = new File(plugin.getDataFolder(), "Gear.yml");
    }

    /**
     * Refreshes the cached kit definitions from disk, merging bundled defaults when present.
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
            Map<GearSlot, List<WeightedItem>> map = new EnumMap<>(GearSlot.class);
            for (GearSlot slot : GearSlot.values()) {
                List<WeightedItem> entries = kitSection.getStringList(slot.name().toLowerCase())
                        .stream()
                        .map(this::parseWeightedItem)
                        .flatMap(Optional::stream)
                        .toList();
                map.put(slot, entries);
            }
            kits.put(key, new KitConfig(key, target, steepness, range, map));
        }
    }

    /**
     * Loads default values from the packaged Gear.yml and applies them as configuration defaults.
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
     * Parses a colon-delimited material:weight string into a weighted item entry.
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
     * Retrieves a kit definition by name when available.
     */
    public Optional<KitConfig> getKit(String name) {
        return Optional.ofNullable(kits.get(name));
    }

    /**
     * Exposes all loaded kit definitions keyed by name.
     */
    public Map<String, KitConfig> getKits() {
        return kits;
    }
}
