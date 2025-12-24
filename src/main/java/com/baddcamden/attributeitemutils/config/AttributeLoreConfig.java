package com.baddcamden.attributeitemutils.config;

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
    private final String valueFormat;
    private final String anySlotCondition;
    private final String mainHandCondition;
    private final String offHandCondition;
    private final String otherSlotCondition;
    private final Map<String, String> slotNames;

    public AttributeLoreConfig(Map<Attribute, AttributeLore> attributes,
                               String headerFormat,
                               String valueFormat,
                               String anySlotCondition,
                               String mainHandCondition,
                               String offHandCondition,
                               String otherSlotCondition,
                               Map<String, String> slotNames) {
        this.attributes = attributes;
        this.headerFormat = translate(headerFormat);
        this.valueFormat = translate(valueFormat);
        this.anySlotCondition = translate(anySlotCondition);
        this.mainHandCondition = translate(mainHandCondition);
        this.offHandCondition = translate(offHandCondition);
        this.otherSlotCondition = translate(otherSlotCondition);
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
        String valueFormat = formats == null ? "&7+{amount}% {color}{name}" : formats.getString("value", "&7+{amount}% {color}{name}");
        ConfigurationSection fulfillment = formats == null ? null : formats.getConfigurationSection("fulfillment");
        String anySlotCondition = fulfillment == null ? "&8Applies in any slot." : fulfillment.getString("any-slot", "&8Applies in any slot.");
        String mainHandCondition = fulfillment == null ? "&8Only when held in main hand." : fulfillment.getString("main-hand", "&8Only when held in main hand.");
        String offHandCondition = fulfillment == null ? "&8Only when held in off hand." : fulfillment.getString("off-hand", "&8Only when held in off hand.");
        String otherSlotCondition = fulfillment == null ? "&8Only when in {slot}." : fulfillment.getString("other-slot", "&8Only when in {slot}.");
        Map<String, String> slotNames = loadSlotNames(formats);

        ConfigurationSection attributeSection = config.getConfigurationSection("attributes");
        Map<Attribute, AttributeLore> attributes = new EnumMap<Attribute, AttributeLore>(Attribute.class);
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
        return new AttributeLoreConfig(attributes, headerFormat, valueFormat, anySlotCondition, mainHandCondition, offHandCondition, otherSlotCondition, slotNames);
    }

    private static Map<String, String> loadSlotNames(ConfigurationSection formats) {
        Map<String, String> slotNames = new LinkedHashMap<>();
        if (formats == null) {
            slotNames.put(EquipmentSlot.HAND.name(), "Main hand");
            slotNames.put(EquipmentSlot.OFF_HAND.name(), "Off hand");
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
            slotNames.put(EquipmentSlot.HAND.name(), "Main hand");
        }
        if (!slotNames.containsKey(EquipmentSlot.OFF_HAND.name())) {
            slotNames.put(EquipmentSlot.OFF_HAND.name(), "Off hand");
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
            List<String> formatted = formatLine(attribute, modifier);
            if (!formatted.isEmpty()) {
                lines.addAll(formatted);
            }
        }
        return lines;
    }

    private List<String> formatLine(Attribute attribute, AttributeModifier modifier) {
        double percentValue = modifier.getAmount() * 100.0;
        String slotName = slotName(modifier.getSlot());
        AttributeLore lore = attributes.get(attribute);
        String color = lore == null ? ChatColor.WHITE.toString() : lore.color();
        String name = lore == null ? attribute.name() : lore.name();
        List<String> lines = new ArrayList<>();
        lines.add(valueFormat
                .replace("{amount}", PERCENT_FORMAT.format(percentValue))
                .replace("{name}", name)
                .replace("{color}", color));

        String condition = formatCondition(modifier.getSlot(), slotName);
        if (!condition.isBlank()) {
            lines.add(condition.replace("{name}", name).replace("{color}", color));
        }
        return lines;
    }

    private String formatHeader(Attribute attribute) {
        AttributeLore lore = attributes.get(attribute);
        String color = lore == null ? ChatColor.GOLD.toString() : lore.color();
        String name = lore == null ? attribute.name() : lore.name();
        return headerFormat
                .replace("{name}", name)
                .replace("{color}", color);
    }

    private String formatCondition(EquipmentSlot slot, String slotName) {
        if (slot == EquipmentSlot.HAND) {
            return mainHandCondition;
        }
        if (slot == EquipmentSlot.OFF_HAND) {
            return offHandCondition;
        }
        if (slot == null) {
            return anySlotCondition;
        }
        return otherSlotCondition.replace("{slot}", slotName);
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
