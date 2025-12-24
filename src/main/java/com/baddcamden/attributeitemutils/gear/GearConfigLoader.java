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

/**
 * Loads, caches, and exposes gear kit configurations from {@code Gear.yml} for other services to
 * consume. This loader transparently layers default resources with user-provided overrides to
 * provide predictable kit data for commands and attribute application.
 */
public class GearConfigLoader {

    /**
     * Owning plugin instance used to resolve the data folder and to log parse failures.
     */
    private final JavaPlugin plugin;

    /**
     * Location of the mutable {@code Gear.yml} file inside the plugin data directory.
     */
    private final File gearConfigFile;

    /**
     * In-memory cache of kit configurations keyed by their configuration section names.
     */
    private final Map<String, KitConfig> kits = new HashMap<>();

    /**
     * YAML configuration backing the loaded kit definitions. Initialized during reload.
     */
    private YamlConfiguration gearConfig;

    /**
     * Creates a loader bound to the supplied plugin and prepares the {@code Gear.yml} file handle.
     *
     * @param plugin the plugin that owns the configuration directory and bundled defaults
     */
    public GearConfigLoader(JavaPlugin plugin) {
        this.plugin = plugin;
        this.gearConfigFile = new File(plugin.getDataFolder(), "Gear.yml");
    }

    /**
     * Reloads the gear configuration, combining user-supplied values with packaged defaults and
     * rebuilding the cached kit entries. Invocations discard previous state so callers always see
     * the latest disk values.
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
            if (kitSection == null) {
                //VAGUE/IMPROVEMENT NEEDED Clarify whether missing kit sections should trigger an error or be silently skipped.
                continue;
            }
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
     * Ensures the runtime configuration has access to the default {@code Gear.yml} entries bundled
     * with the plugin. This protects against missing keys when servers upgrade without merging new
     * config values.
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
     * Parses a single weighted item entry in the format {@code MATERIAL:weight} into a usable
     * {@link WeightedItem}. Entries that cannot be parsed are logged and omitted from the
     * resulting collection.
     *
     * @param raw raw string entry from the configuration list
     * @return a populated weighted item if the string can be parsed, otherwise {@link Optional#empty()}
     */
    private Optional<WeightedItem> parseWeightedItem(String raw) {
        String[] parts = raw.split(":");
        if (parts.length != 2) {
            return Optional.empty();
        }
        try {
            Material mat = Material.matchMaterial(parts[0].trim());
            double weight = Double.parseDouble(parts[1]);
            if (mat == null) {
                return Optional.empty();
            }
            return Optional.of(new WeightedItem(mat, weight));
        } catch (NumberFormatException ex) {
            plugin.getLogger().warning("Unable to parse weight entry: " + raw);
            return Optional.empty();
        }
    }

    /**
     * Retrieves a kit by name if it exists in the current cache.
     *
     * @param name configuration key for the desired kit
     * @return an {@link Optional} containing the kit configuration when present
     */
    public Optional<KitConfig> getKit(String name) {
        return Optional.ofNullable(kits.get(name));
    }

    /**
     * Provides a live view of all cached kit configurations. Callers should treat the returned map
     * as read-only to avoid surprising mutations.
     *
     * @return map of kit name to configuration
     */
    public Map<String, KitConfig> getKits() {
        return kits;
    }
}
