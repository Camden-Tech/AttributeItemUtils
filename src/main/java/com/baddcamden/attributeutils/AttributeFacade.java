package com.baddcamden.attributeutils;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.google.common.collect.Multimap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class AttributeFacade {
    private static final String GENERIC_SCALE_KEY = "generic.scale";
    private static final String GENERIC_FALL_DAMAGE_MULTIPLIER_KEY = "generic.fall_damage_multiplier";

    private static final Attribute GENERIC_SCALE = findAttribute("GENERIC_SCALE");
    private static final Attribute GENERIC_FALL_DAMAGE_MULTIPLIER = findAttribute("GENERIC_FALL_DAMAGE_MULTIPLIER");

    private final Map<Attribute, AttributeDefinition> definitions = new HashMap<Attribute, AttributeDefinition>();
    private final Map<Attribute, AttributeBaseline> baselines = new HashMap<Attribute, AttributeBaseline>();
    private static final String ATTRIBUTEUTILS_PREFIX = "attributeutils:";
    private Logger logger = Logger.getLogger(AttributeFacade.class.getName());

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    public void registerDefinition(AttributeDefinition definition) {
        definitions.put(definition.attribute(), definition);
    }

    public void registerBaseline(AttributeBaseline baseline) {
        baselines.put(baseline.attribute(), baseline);
    }

    public double computeAmount(Attribute attribute, double baseAmount) {
        double amount = baseAmount;
        if (isAttributeMatch(attribute, GENERIC_SCALE, GENERIC_SCALE_KEY)) {
            amount = Math.cbrt(1 + baseAmount) - 1;
        } else if (isAttributeMatch(attribute, GENERIC_FALL_DAMAGE_MULTIPLIER, GENERIC_FALL_DAMAGE_MULTIPLIER_KEY)) {
            amount = -baseAmount;
        }

        AttributeDefinition definition = definitions.get(attribute);
        if (definition == null) {
            return amount;
        }
        return definition.applyCap(amount);
    }

    private static Attribute findAttribute(String name) {
        try {
            return Attribute.valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static boolean isAttributeMatch(Attribute attribute, Attribute optionalEnum, String key) {
        if (attribute == null) {
            return false;
        }

        if (optionalEnum != null) {
            return attribute == optionalEnum;
        }

        return attribute.getKey().getKey().equalsIgnoreCase(key);
    }

    public void refresh(ItemStack stack, EquipmentSlot slot) {
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return;
        }

        Multimap<Attribute, AttributeModifier> defaults = defaultModifiers(stack, slot);
        Multimap<Attribute, AttributeModifier> modifiers = meta.getAttributeModifiers();
        logger.info("[ATTR] Refreshing modifiers for " + stack.getType() + " slot=" + slot + " existing=" + summarize(modifiers));
        if (modifiers == null || modifiers.isEmpty()) {
            if (defaults != null) {
                defaults.forEach(meta::addAttributeModifier);
            }
            modifiers = meta.getAttributeModifiers();
        }

        Multimap<Attribute, AttributeModifier> initialModifiers = modifiers;
        definitions.keySet().forEach(attribute -> {
            if (initialModifiers == null) {
                return;
            }
            for (AttributeModifier modifier : List.copyOf(initialModifiers.get(attribute))) {
                if (isPluginModifier(modifier)) {
                    logger.info("[ATTR] Removing plugin modifier " + modifier + " for attribute " + attribute);
                    meta.removeAttributeModifier(attribute, modifier);
                }
            }
        });

        // Ensure vanilla baselines are reattached even when only plugin-authored modifiers are present.
        Multimap<Attribute, AttributeModifier> finalModifiers = meta.getAttributeModifiers();
        if (defaults != null) {
            defaults.forEach((attribute, modifier) -> {
                if (finalModifiers == null || !hasNonPluginModifier(finalModifiers.get(attribute))) {
                    meta.addAttributeModifier(attribute, modifier);
                }
            });
        }
        baselines.forEach((attribute, baseline) -> {
            AttributeDefinition definition = definitions.get(attribute);
            if (definition != null) {
                meta.addAttributeModifier(attribute, baseline.asModifier(definition, slot));
            }
        });
        logger.info(() -> "[ATTR] Final modifiers after refresh for " + stack.getType() + " slot=" + slot + " -> " + summarize(meta.getAttributeModifiers()));
        stack.setItemMeta(meta);
    }

    private Multimap<Attribute, AttributeModifier> defaultModifiers(ItemStack stack, EquipmentSlot slot) {
        ItemMeta vanillaMeta = new ItemStack(stack.getType()).getItemMeta();
        if (vanillaMeta != null) {
            Multimap<Attribute, AttributeModifier> defaultForSlot = vanillaMeta.getAttributeModifiers(slot);
            if (defaultForSlot != null && !defaultForSlot.isEmpty()) {
                return defaultForSlot;
            }

            Multimap<Attribute, AttributeModifier> allDefaults = vanillaMeta.getAttributeModifiers();
            if (allDefaults != null && !allDefaults.isEmpty()) {
                return allDefaults;
            }
        }

        // Fall back to any modifiers currently on the stack so we never strip out vanilla values when the
        // Bukkit item factory fails to expose built-in defaults for the material. This mirrors the vendor
        // behavior that always reattaches vanilla baselines before layering plugin modifiers.
        ItemMeta existingMeta = stack.getItemMeta();
        if (existingMeta != null) {
            Multimap<Attribute, AttributeModifier> existing = slot == null
                    ? existingMeta.getAttributeModifiers()
                    : existingMeta.getAttributeModifiers(slot);
            if (existing != null && !existing.isEmpty()) {
                return existing;
            }
        }

        return null;
    }

    public void mutate(ItemStack stack, Attribute attribute, double baseAmount, EquipmentSlot slot) {
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return;
        }

        AttributeDefinition definition = definitions.get(attribute);
        if (definition == null) {
            return;
        }

        Multimap<Attribute, AttributeModifier> defaults = defaultModifiers(stack, slot);
        Multimap<Attribute, AttributeModifier> existing = meta.getAttributeModifiers();
        final Multimap<Attribute, AttributeModifier> existingBefore = existing;

        // Ensure vanilla attributes stick around before we apply plugin modifiers, mirroring the merge
        // patterns recommended by the community to avoid NBT replacement wiping AttributeModifiers.
        if (defaults != null) {
            defaults.forEach((vanillaAttribute, modifier) -> {
                Iterable<AttributeModifier> current = existingBefore == null ? null : existingBefore.get(vanillaAttribute);
                if (!hasNonPluginModifier(current)) {
                    meta.addAttributeModifier(vanillaAttribute, modifier);
                }
            });
        }

        // Remove any previously-applied plugin modifiers for the same attribute/slot so the deterministic
        // UUIDs we generate do not collide and cause vanilla modifiers to be dropped.
        existing = meta.getAttributeModifiers();
        if (existing != null) {
            for (AttributeModifier modifier : List.copyOf(existing.get(attribute))) {
                if (isPluginModifier(modifier) && (slot == null || modifier.getSlot() == null || modifier.getSlot() == slot)) {
                    meta.removeAttributeModifier(attribute, modifier);
                }
            }
        }

        AttributeModifier modifier = definition.newModifier(computeAmount(attribute, baseAmount), slot);
        meta.addAttributeModifier(attribute, modifier);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        logger.info("[ATTR] Applied modifier " + modifier + " to " + stack.getType() + " for attribute " + attribute +
                " resulting modifiers=" + summarize(meta.getAttributeModifiers()));
        stack.setItemMeta(meta);
    }

    private boolean isPluginModifier(AttributeModifier modifier) {
        String name = modifier.getName();
        if (name == null) {
            return false;
        }
        String normalized = name.toLowerCase();
        return normalized.startsWith("attributeitemutils:") || normalized.startsWith(ATTRIBUTEUTILS_PREFIX);
    }

    private boolean hasNonPluginModifier(Iterable<AttributeModifier> modifiers) {
        if (modifiers == null) {
            return false;
        }
        for (AttributeModifier modifier : modifiers) {
            if (!isPluginModifier(modifier)) {
                return true;
            }
        }
        return false;
    }

    private String summarize(Multimap<Attribute, AttributeModifier> modifiers) {
        if (modifiers == null || modifiers.isEmpty()) {
            return "[]";
        }
        return modifiers.entries().stream()
                .map(entry -> entry.getKey().name() + "=" + entry.getValue().getAmount() + "@" + entry.getValue().getOperation() +
                        " slot=" + entry.getValue().getSlot())
                .collect(java.util.stream.Collectors.joining(", ", "[", "]"));
    }

    public void applyEnchant(ItemStack stack, Enchantment enchantment, int level) {
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return;
        }
        meta.addEnchant(enchantment, level, true);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        stack.setItemMeta(meta);
    }
}
