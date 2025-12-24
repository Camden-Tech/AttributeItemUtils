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

public class AttributeFacade {
    private static final String GENERIC_SCALE_KEY = "generic.scale";
    private static final String GENERIC_FALL_DAMAGE_MULTIPLIER_KEY = "generic.fall_damage_multiplier";

    private static final Attribute GENERIC_SCALE = findAttribute("GENERIC_SCALE");
    private static final Attribute GENERIC_FALL_DAMAGE_MULTIPLIER = findAttribute("GENERIC_FALL_DAMAGE_MULTIPLIER");

    private final Map<Attribute, AttributeDefinition> definitions = new HashMap<Attribute, AttributeDefinition>();
    private final Map<Attribute, AttributeBaseline> baselines = new HashMap<Attribute, AttributeBaseline>();

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

        Multimap<Attribute, AttributeModifier> modifiers = meta.getAttributeModifiers();
        definitions.keySet().forEach(attribute -> {
            if (modifiers == null) {
                return;
            }
            for (AttributeModifier modifier : List.copyOf(modifiers.get(attribute))) {
                if (isPluginModifier(modifier)) {
                    meta.removeAttributeModifier(attribute, modifier);
                }
            }
        });
        baselines.forEach((attribute, baseline) -> {
            AttributeDefinition definition = definitions.get(attribute);
            if (definition != null) {
                meta.addAttributeModifier(attribute, baseline.asModifier(definition, slot));
            }
        });
        stack.setItemMeta(meta);
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

        AttributeModifier modifier = definition.newModifier(computeAmount(attribute, baseAmount), slot);
        meta.addAttributeModifier(attribute, modifier);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        stack.setItemMeta(meta);
    }

    private boolean isPluginModifier(AttributeModifier modifier) {
        String name = modifier.getName();
        return name != null && name.startsWith("attributeitemutils:");
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
