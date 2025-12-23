package com.baddcamdne.attributeitemutils.gear;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class GearConfigLoader {

    private final JavaPlugin plugin;
    private final File gearConfigFile;
    private final Map<String, KitConfig> kits = new HashMap<>();
    private YamlConfiguration gearConfig;

    public GearConfigLoader(JavaPlugin plugin) {
        this.plugin = plugin;
        this.gearConfigFile = new File(plugin.getDataFolder(), "Gear.yml");
    }

    public void reload() {
        kits.clear();
        gearConfig = YamlConfiguration.loadConfiguration(gearConfigFile);
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

    public Optional<KitConfig> getKit(String name) {
        return Optional.ofNullable(kits.get(name));
    }

    public Map<String, KitConfig> getKits() {
        return kits;
    }
}
