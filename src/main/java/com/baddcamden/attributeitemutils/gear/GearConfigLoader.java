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
 * Loads the Gear.yml configuration file and exposes parsed kit definitions for use when generating
 * items. Instances of this class should be reloaded when the plugin configuration changes.
 */
public class GearConfigLoader {

    /** Plugin handle used for resolving resources and logging errors. */
    private final JavaPlugin plugin;
    /** File handle pointing to the user-modifiable Gear.yml. */
    private final File gearConfigFile;
    /** Parsed kits keyed by configuration name. */
    private final Map<String, KitConfig> kits = new HashMap<>();
    /** Live configuration instance that may include defaults from the plugin jar. */
    private YamlConfiguration gearConfig;

    /**
     * Creates a loader for the plugin's Gear.yml file.
     *
     * @param plugin active plugin instance providing access to resources and the data folder
     */
    public GearConfigLoader(JavaPlugin plugin) {
        this.plugin = plugin;
        this.gearConfigFile = new File(plugin.getDataFolder(), "Gear.yml");
    }

    /**
     * Reloads the Gear.yml file, copying defaults from the packaged resource and rebuilding all
     * cached kit definitions.
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
            if (kitSection != null) {
                loadKit(key, kitSection);
            }
        }
    }

    /**
     * Copies default kit configuration from the bundled Gear.yml resource when present so missing
     * values are filled with sensible defaults.
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
     * Parses an individual kit section into a {@link KitConfig} and stores it in the internal
     * cache.
     *
     * @param kitName    name of the kit to register
     * @param kitSection configuration section containing weights per gear slot
     */
    private void loadKit(String kitName, ConfigurationSection kitSection) {
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
        kits.put(kitName, new KitConfig(kitName, target, steepness, range, map));
    }

    /**
     * Attempts to parse a weighted item entry from its raw string form in the configuration.
     *
     * @param raw string in the format MATERIAL:weight
     * @return a parsed {@link WeightedItem} if the entry is valid
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
     * Retrieves a parsed kit definition by name.
     *
     * @param name kit identifier found in the configuration
     * @return an {@link Optional} containing the kit when present
     */
    public Optional<KitConfig> getKit(String name) {
        return Optional.ofNullable(kits.get(name));
    }

    /**
     * Exposes the cached kit definitions keyed by their configuration name.
     *
     * @return map containing the cached kit entries
     */
    public Map<String, KitConfig> getKits() {
        return kits;
    }
}
