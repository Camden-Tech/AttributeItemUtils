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
    private final Map<Attribute, AttributeDefinition> definitions = new EnumMap<>(Attribute.class);
    private final Map<Attribute, AttributeBaseline> baselines = new EnumMap<>(Attribute.class);

    public void registerDefinition(AttributeDefinition definition) {
        definitions.put(definition.attribute(), definition);
    }

    public void registerBaseline(AttributeBaseline baseline) {
        baselines.put(baseline.attribute(), baseline);
    }

    public double computeAmount(Attribute attribute, double baseAmount) {
        double amount = switch (attribute) {
            case GENERIC_SCALE -> Math.cbrt(1 + baseAmount) - 1;
            case GENERIC_FALL_DAMAGE_MULTIPLIER -> -baseAmount;
            default -> baseAmount;
        };

        AttributeDefinition definition = definitions.get(attribute);
        if (definition == null) {
            return amount;
        }
        return definition.applyCap(amount);
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
