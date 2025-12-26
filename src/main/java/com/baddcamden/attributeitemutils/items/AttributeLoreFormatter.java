package com.baddcamden.attributeitemutils.items;

import me.baddcamden.attributeutils.api.AttributeFacade;
import me.baddcamden.attributeutils.command.CommandParsingUtils;
import me.baddcamden.attributeutils.handler.item.TriggerCriterion;
import me.baddcamden.attributeutils.model.AttributeDefinition;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Formats lore for attribute items using configurable colors, names, and fulfillment text.
 */
public class AttributeLoreFormatter {

    private final Plugin plugin;
    private final Logger logger;
    private final File loreFile;
    private final DecimalFormat decimalFormat = new DecimalFormat("0.##");

    private String headerFormat = "{color}{name}";
    private String valueFormat = ChatColor.GRAY + "{sign}{amount}{unit} {name}";
    private String anySlotMessage = ChatColor.DARK_GRAY + "Applies in any slot.";
    private String mainHandMessage = ChatColor.DARK_GRAY + "Only when held in main hand.";
    private String offHandMessage = ChatColor.DARK_GRAY + "Only when held in off hand.";
    private String otherSlotMessage = ChatColor.DARK_GRAY + "Only when in {slot}.";
    private final Map<EquipmentSlot, String> slotNames = new EnumMap<>(EquipmentSlot.class);
    private final Map<String, AttributeMeta> attributes = new LinkedHashMap<>();

    public AttributeLoreFormatter(Plugin plugin, Logger logger) {
        this.plugin = plugin;
        this.logger = logger;
        this.loreFile = new File(plugin.getDataFolder(), "AttributeLore.yml");
    }

    /**
     * Reloads formatting configuration from disk, merging the bundled defaults when present.
     */
    public void reload() {
        attributes.clear();
        slotNames.clear();

        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(loreFile);
        loadDefaults(configuration);

        headerFormat = configuration.getString("formats.header", headerFormat);
        valueFormat = configuration.getString("formats.value", valueFormat);
        anySlotMessage = configuration.getString("formats.fulfillment.any-slot", anySlotMessage);
        mainHandMessage = configuration.getString("formats.fulfillment.main-hand", mainHandMessage);
        offHandMessage = configuration.getString("formats.fulfillment.off-hand", offHandMessage);
        otherSlotMessage = configuration.getString("formats.fulfillment.other-slot", otherSlotMessage);

        ConfigurationSection slotSection = configuration.getConfigurationSection("formats.slot-names");
        if (slotSection != null) {
            for (String key : slotSection.getKeys(false)) {
                EquipmentSlot slot = parseSlot(key);
                if (slot == null) {
                    continue;
                }
                String value = slotSection.getString(key);
                if (value != null) {
                    slotNames.put(slot, translateColors(value));
                }
            }
        }

        ConfigurationSection attributeSection = configuration.getConfigurationSection("attributes");
        if (attributeSection != null) {
            for (String key : attributeSection.getKeys(false)) {
                String normalized = normalizeAttributeId(key);
                if (normalized.isEmpty()) {
                    continue;
                }
                String name = attributeSection.getString(key + ".name", key);
                String color = attributeSection.getString(key + ".color", "");
                attributes.put(normalized, new AttributeMeta(translateColors(name), translateColors(color)));
            }
        }
    }

    private void loadDefaults(YamlConfiguration configuration) {
        try (InputStream stream = plugin.getResource("AttributeLore.yml")) {
            if (stream == null) {
                return;
            }
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
            configuration.setDefaults(defaults);
            configuration.options().copyDefaults(true);
        } catch (IOException ex) {
            logger.warning("Unable to load default AttributeLore.yml: " + ex.getMessage());
        }
    }

    /**
     * Rebuilds the lore on the provided item using the supplied attribute definitions.
     */
    public ItemStack rebuildLore(ItemStack itemStack,
                                 List<CommandParsingUtils.AttributeDefinition> definitions,
                                 AttributeFacade attributeFacade,
                                 EquipmentSlot slot) {
        if (itemStack == null || definitions == null) {
            return itemStack;
        }

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return itemStack;
        }

        List<String> lore = formatLore(definitions, attributeFacade, slot);
        meta.setLore(lore);
        itemStack.setItemMeta(meta);
        return itemStack;
    }

    private List<String> formatLore(List<CommandParsingUtils.AttributeDefinition> definitions,
                                    AttributeFacade attributeFacade,
                                    EquipmentSlot slot) {
        Map<TriggerCriterion, List<String>> grouped = new LinkedHashMap<>();
        for (CommandParsingUtils.AttributeDefinition definition : definitions) {
            AttributeDefinition attributeDefinition = attributeFacade.getDefinition(definition.getKey().key()).orElse(null);
            if (attributeDefinition == null) {
                continue;
            }

            double clampedValue = attributeDefinition.capConfig().clamp(definition.getValue());
            double effectiveValue = definition.getCapOverride()
                    .map(attributeDefinition.capConfig()::clamp)
                    .map(cap -> Math.min(cap, clampedValue))
                    .orElse(clampedValue);
            boolean isPercent = isMultiplierStyle(attributeDefinition);
            String normalizedId = normalizeAttributeId(attributeDefinition.id());
            double displayValue = isPercent
                    ? computeMultiplierPercent(normalizedId, effectiveValue)
                    : effectiveValue;
            AttributeMeta attributeMeta = attributes.getOrDefault(
                    normalizedId,
                    new AttributeMeta(translateColors(attributeDefinition.displayName()), ""));

            TriggerCriterion criterion = definition.getCriterion()
                    .flatMap(TriggerCriterion::fromRaw)
                    .orElse(TriggerCriterion.defaultCriterion());

            String formatted = formatValueLine(attributeMeta, displayValue, isPercent);
            grouped.computeIfAbsent(criterion, key -> new ArrayList<>()).add(formatted);
        }

        List<String> lore = new ArrayList<>();
        if (!grouped.isEmpty()) {
            lore.add("");
        }

        boolean firstSection = true;
        for (Map.Entry<TriggerCriterion, List<String>> entry : grouped.entrySet()) {
            List<String> lines = entry.getValue();
            if (!firstSection) {
                lore.add("");
            }
            lore.addAll(lines);

            String fulfillment = fulfillmentMessage(entry.getKey(), slot);
            if (!fulfillment.isBlank()) {
                lore.add("");
                lore.add(fulfillment);
            }
            firstSection = false;
        }

        return lore;
    }

    private String formatValueLine(AttributeMeta attributeMeta, double value, boolean percent) {
        String sign = value >= 0 ? "+" : "-";
        double magnitude = Math.abs(value);
        String amount = decimalFormat.format(magnitude);
        boolean includeSignToken = valueFormat.contains("{sign}");
        String amountToken = includeSignToken ? amount : sign + amount;

        Map<String, String> placeholders = Map.of(
                "color", Objects.toString(attributeMeta.color(), ""),
                "name", applyPlaceholders(headerFormat, Map.of(
                        "color", Objects.toString(attributeMeta.color(), ""),
                        "name", Objects.toString(attributeMeta.name(), ""))),
                "amount", amountToken,
                "sign", sign,
                "unit", percent ? "%" : ""
        );
        return translateColors(applyPlaceholders(valueFormat, placeholders));
    }

    private String fulfillmentMessage(TriggerCriterion criterion, EquipmentSlot slot) {
        String template = switch (criterion) {
            case HELD -> mainHandMessage;
            case OFFHAND -> offHandMessage;
            case EQUIPPED -> otherSlotMessage;
            default -> anySlotMessage;
        };
        String slotName = slotNames.getOrDefault(slot, titleCase(slot.name()));
        return translateColors(applyPlaceholders(template, Map.of("slot", slotName)));
    }

    private String applyPlaceholders(String template, Map<String, String> values) {
        String result = template == null ? "" : template;
        if (values == null) {
            return result;
        }
        for (Map.Entry<String, String> entry : values.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", Objects.toString(entry.getValue(), ""));
        }
        return result;
    }

    private String translateColors(String raw) {
        return raw == null ? "" : ChatColor.translateAlternateColorCodes('&', raw);
    }

    private double computeMultiplierPercent(String normalizedId, double effectiveValue) {
        double adjusted = adjustMultiplierValue(normalizedId, effectiveValue);

        if (effectiveValue < 0d) {
            return (adjusted - 1d) * 100d;
        }

        if (adjusted < 1d) {
            return adjusted * 100d;
        }

        return (adjusted - 1d) * 100d;
    }

    private double adjustMultiplierValue(String normalizedId, double effectiveValue) {
        double adjustedValue = "SCALE".equals(normalizedId)
                ? Math.cbrt(effectiveValue)
                : effectiveValue;

        if (adjustedValue < 0) {
            adjustedValue = Math.max(0d, 1d + adjustedValue);
        }

        return adjustedValue;
    }

    private String titleCase(String raw) {
        String[] segments = raw.toLowerCase(Locale.ROOT).split("_");
        StringBuilder builder = new StringBuilder();
        for (String segment : segments) {
            if (segment.isBlank()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(segment.charAt(0))).append(segment.substring(1));
        }
        return builder.toString();
    }

    private EquipmentSlot parseSlot(String key) {
        try {
            return EquipmentSlot.valueOf(key.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            logger.warning("Unknown equipment slot in AttributeLore.yml: " + key);
            return null;
        }
    }

    private boolean isMultiplierStyle(AttributeDefinition attributeDefinition) {
        if (attributeDefinition == null) {
            return false;
        }
        double base = attributeDefinition.defaultBaseValue();
        double max = attributeDefinition.capConfig().globalMax();
        String normalized = normalizeAttributeId(attributeDefinition.id());
        return normalized.contains("MULTIPLIER")
                || normalized.contains("EFFICIENCY")
                || normalized.contains("SPEED")
                || (base > 0 && base <= 2 && max <= 10);
    }

    /**
     * Normalizes attribute ids by taking the last segment, replacing separators with underscores, and uppercasing it.
     */
    public String normalizeAttributeId(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String trimmed = raw.trim();
        String normalizedSeparators = trimmed
                .replace(':', '.')
                .replace('-', '_')
                .replace(' ', '_');
        int lastDelimiter = normalizedSeparators.lastIndexOf('.') + 1;
        String lastSegment = normalizedSeparators.substring(lastDelimiter);
        return lastSegment.toUpperCase(Locale.ROOT);
    }

    private record AttributeMeta(String name, String color) {
    }
}

