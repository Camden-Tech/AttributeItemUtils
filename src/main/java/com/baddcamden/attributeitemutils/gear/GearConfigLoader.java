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
 * Loads kit definitions and their weighted gear options from {@code Gear.yml}.
 * The loader keeps an in-memory cache of parsed {@link KitConfig} objects to
 * avoid repeatedly reading disk for frequent lookups during gameplay.
 */
public class GearConfigLoader {

    /** Plugin instance used to resolve the data folder and emit log messages. */
    private final JavaPlugin plugin;
    /** Physical file that stores kit definitions. */
    private final File gearConfigFile;
    /** Cached mapping of kit name to its parsed configuration. */
    private final Map<String, KitConfig> kits = new HashMap<>();
    private YamlConfiguration gearConfig;

    public GearConfigLoader(JavaPlugin plugin) {
        this.plugin = plugin;
        this.gearConfigFile = new File(plugin.getDataFolder(), "Gear.yml");
    }

    /**
     * Reloads {@code Gear.yml} into memory and rebuilds the kit cache.
     * Safe defaults are loaded first so missing fields are backfilled before
     * the plugin reads user-defined overrides.
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
                continue;
            }
            loadKitConfig(key, kitSection);
        }
    }

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

    private void loadKitConfig(String kitName, ConfigurationSection kitSection) {
        double target = kitSection.getDouble("target-weight", 5.0);
        double steepness = kitSection.getDouble("steepness", 1.0);
        double range = kitSection.getDouble("range", 4.0);
        Map<GearSlot, List<WeightedItem>> items = loadKitItems(kitSection);
        kits.put(kitName, new KitConfig(kitName, target, steepness, range, items));
    }

    private Map<GearSlot, List<WeightedItem>> loadKitItems(ConfigurationSection kitSection) {
        Map<GearSlot, List<WeightedItem>> map = new EnumMap<>(GearSlot.class);
        for (GearSlot slot : GearSlot.values()) {
            List<WeightedItem> entries = kitSection.getStringList(slot.name().toLowerCase())
                    .stream()
                    .map(this::parseWeightedItem)
                    .flatMap(Optional::stream)
                    .toList();
            map.put(slot, entries);
        }
        return map;
    }

    /**
     * Attempts to parse a weighted material entry in the format {@code MATERIAL:weight}.
     * @param raw raw configuration value.
     * @return optional weighted item when parsing succeeds, otherwise empty.
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
     * Retrieves a specific kit configuration if it exists.
     * @param name kit key from the configuration file.
     * @return optional kit definition for the requested name.
     */
    public Optional<KitConfig> getKit(String name) {
        return Optional.ofNullable(kits.get(name));
    }

    /**
     * Exposes all loaded kit configurations.
     * @return live mapping of kit name to configuration.
     */
    public Map<String, KitConfig> getKits() {
        return kits;
    }
}
