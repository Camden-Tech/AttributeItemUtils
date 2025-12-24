package com.baddcamdne.attributeutils;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.EnumMap;
import java.util.Map;

public class AttributeFacade {
    private static final Attribute GENERIC_SCALE = findAttribute("GENERIC_SCALE");
    private static final Attribute GENERIC_FALL_DAMAGE_MULTIPLIER = findAttribute("GENERIC_FALL_DAMAGE_MULTIPLIER");

    private final Map<Attribute, AttributeDefinition> definitions = new EnumMap<>(Attribute.class);
    private final Map<Attribute, AttributeBaseline> baselines = new EnumMap<>(Attribute.class);

    public void registerDefinition(AttributeDefinition definition) {
        definitions.put(definition.attribute(), definition);
    }

    public void registerBaseline(AttributeBaseline baseline) {
        baselines.put(baseline.attribute(), baseline);
    }

    public double computeAmount(Attribute attribute, double baseAmount) {
        double amount = baseAmount;
        if (attribute == GENERIC_SCALE) {
            amount = Math.cbrt(1 + baseAmount) - 1;
        } else if (attribute == GENERIC_FALL_DAMAGE_MULTIPLIER) {
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

    public void refresh(ItemStack stack, EquipmentSlot slot) {
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return;
        }

        definitions.keySet().forEach(meta::removeAttributeModifier);
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
