package com.baddcamdne.attributeitemutils.config;

import com.google.common.collect.Multimap;
import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

public class AttributeLoreConfig {
    private static final DecimalFormat PERCENT_FORMAT = new DecimalFormat("0.##");

    private final Map<Attribute, AttributeLore> attributes;
    private final String headerFormat;
    private final String anySlotFormat;
    private final String mainHandFormat;
    private final String offHandFormat;
    private final String otherSlotFormat;
    private final Map<String, String> slotNames;

    public AttributeLoreConfig(Map<Attribute, AttributeLore> attributes,
                               String headerFormat,
                               String anySlotFormat,
                               String mainHandFormat,
                               String offHandFormat,
                               String otherSlotFormat,
                               Map<String, String> slotNames) {
        this.attributes = attributes;
        this.headerFormat = translate(headerFormat);
        this.anySlotFormat = translate(anySlotFormat);
        this.mainHandFormat = translate(mainHandFormat);
        this.offHandFormat = translate(offHandFormat);
        this.otherSlotFormat = translate(otherSlotFormat);
        this.slotNames = slotNames;
    }

    public static AttributeLoreConfig load(JavaPlugin plugin, Logger logger) {
        File file = new File(plugin.getDataFolder(), "AttributeLore.yml");
        if (!file.exists()) {
            plugin.saveResource("AttributeLore.yml", false);
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection formats = config.getConfigurationSection("formats");
        String headerFormat = formats == null ? "&6{name}" : formats.getString("header", "&6{name}");
        String anySlotFormat = formats == null ? "&7+{amount}% {color}{name}" : formats.getString("any-slot", "&7+{amount}% {color}{name}");
        String mainHandFormat = formats == null ? "&7+{amount}% {color}{name} &8(Main Hand)" : formats.getString("main-hand", "&7+{amount}% {color}{name} &8(Main Hand)");
        String offHandFormat = formats == null ? "&7+{amount}% {color}{name} &8(Off Hand)" : formats.getString("off-hand", "&7+{amount}% {color}{name} &8(Off Hand)");
        String otherSlotFormat = formats == null ? "&7+{amount}% {color}{name} &8({slot})" : formats.getString("other-slot", "&7+{amount}% {color}{name} &8({slot})");
        Map<String, String> slotNames = loadSlotNames(formats);

        ConfigurationSection attributeSection = config.getConfigurationSection("attributes");
        Map<Attribute, AttributeLore> attributes = new EnumMap<>(Attribute.class);
        if (attributeSection != null) {
            for (String key : attributeSection.getKeys(false)) {
                Attribute attribute = parseAttribute(key, logger);
                if (attribute == null) {
                    continue;
                }
                String name = attributeSection.getString(key + ".name", attribute.name());
                String color = attributeSection.getString(key + ".color", "&f");
                attributes.put(attribute, new AttributeLore(name, color));
            }
        }
        return new AttributeLoreConfig(attributes, headerFormat, anySlotFormat, mainHandFormat, offHandFormat, otherSlotFormat, slotNames);
    }

    private static Map<String, String> loadSlotNames(ConfigurationSection formats) {
        Map<String, String> slotNames = new LinkedHashMap<>();
        if (formats == null) {
            slotNames.put(EquipmentSlot.HAND.name(), "Main Hand");
            slotNames.put(EquipmentSlot.OFF_HAND.name(), "Off Hand");
            return slotNames;
        }
        ConfigurationSection slotNameSection = formats.getConfigurationSection("slot-names");
        if (slotNameSection != null) {
            for (String key : slotNameSection.getKeys(false)) {
                String value = slotNameSection.getString(key);
                if (value != null) {
                    slotNames.put(key.toUpperCase(Locale.ROOT), value);
                }
            }
        }
        if (!slotNames.containsKey(EquipmentSlot.HAND.name())) {
            slotNames.put(EquipmentSlot.HAND.name(), "Main Hand");
        }
        if (!slotNames.containsKey(EquipmentSlot.OFF_HAND.name())) {
            slotNames.put(EquipmentSlot.OFF_HAND.name(), "Off Hand");
        }
        return slotNames;
    }

    private static Attribute parseAttribute(String key, Logger logger) {
        try {
            return Attribute.valueOf(key);
        } catch (IllegalArgumentException ex) {
            logger.warning("Unknown attribute in AttributeLore.yml: " + key);
            return null;
        }
    }

    public List<String> buildLore(ItemMeta meta) {
        Multimap<Attribute, AttributeModifier> modifiers = meta.getAttributeModifiers();
        if (modifiers == null || modifiers.isEmpty()) {
            return List.of();
        }

        Map<Attribute, List<AttributeModifier>> byAttribute = new LinkedHashMap<>();
        for (Map.Entry<Attribute, AttributeModifier> entry : modifiers.entries()) {
            if (entry.getValue().getAmount() == 0.0) {
                continue;
            }
            byAttribute.computeIfAbsent(entry.getKey(), ignored -> new ArrayList<>()).add(entry.getValue());
        }

        List<String> lore = new ArrayList<>();
        for (Map.Entry<Attribute, List<AttributeModifier>> entry : byAttribute.entrySet()) {
            List<String> lines = formatLines(entry.getKey(), entry.getValue());
            if (lines.isEmpty()) {
                continue;
            }
            if (!lore.isEmpty()) {
                lore.add("");
            }
            lore.add(formatHeader(entry.getKey()));
            lore.addAll(lines);
        }
        return lore;
    }

    private List<String> formatLines(Attribute attribute, Collection<AttributeModifier> modifiers) {
        List<String> lines = new ArrayList<>();
        for (AttributeModifier modifier : modifiers) {
            String formatted = formatLine(attribute, modifier);
            if (formatted != null && !formatted.isBlank()) {
                lines.add(formatted);
            }
        }
        return lines;
    }

    private String formatLine(Attribute attribute, AttributeModifier modifier) {
        double percentValue = modifier.getAmount() * 100.0;
        String slotFormat = formatForSlot(modifier.getSlot());
        String slotName = slotName(modifier.getSlot());
        AttributeLore lore = attributes.get(attribute);
        String color = lore == null ? ChatColor.WHITE.toString() : lore.color();
        String name = lore == null ? attribute.name() : lore.name();
        return slotFormat
                .replace("{amount}", PERCENT_FORMAT.format(percentValue))
                .replace("{slot}", slotName)
                .replace("{name}", name)
                .replace("{color}", color);
    }

    private String formatHeader(Attribute attribute) {
        AttributeLore lore = attributes.get(attribute);
        String color = lore == null ? ChatColor.GOLD.toString() : lore.color();
        String name = lore == null ? attribute.name() : lore.name();
        return headerFormat
                .replace("{name}", name)
                .replace("{color}", color);
    }

    private String formatForSlot(EquipmentSlot slot) {
        if (slot == EquipmentSlot.HAND) {
            return mainHandFormat;
        }
        if (slot == EquipmentSlot.OFF_HAND) {
            return offHandFormat;
        }
        if (slot == null) {
            return anySlotFormat;
        }
        return otherSlotFormat;
    }

    private String slotName(EquipmentSlot slot) {
        if (slot == null) {
            return "";
        }
        return Optional.ofNullable(slotNames.get(slot.name()))
                .orElseGet(() -> formatSlotName(slot));
    }

    private String formatSlotName(EquipmentSlot slot) {
        String name = slot.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    private static String translate(String value) {
        return ChatColor.translateAlternateColorCodes('&', Objects.requireNonNullElse(value, ""));
    }

    public record AttributeLore(String name, String color) {
        public AttributeLore {
            color = translate(color);
        }
    }
}
