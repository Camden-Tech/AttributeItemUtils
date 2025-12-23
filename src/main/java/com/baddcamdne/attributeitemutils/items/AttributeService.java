package com.baddcamdne.attributeitemutils.items;

import com.baddcamdne.attributeitemutils.config.AttributeConfig;
import com.baddcamdne.attributeitemutils.config.EnchantmentConfig;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.function.Supplier;

public class AttributeService {
    private static final Map<Attribute, String> PREFIXES = Map.ofEntries(
            Map.entry(Attribute.GENERIC_ARMOR, "Hardened "),
            Map.entry(Attribute.GENERIC_ARMOR_TOUGHNESS, "Tough "),
            Map.entry(Attribute.GENERIC_ATTACK_DAMAGE, "Strengthening "),
            Map.entry(Attribute.GENERIC_ATTACK_SPEED, "Speedster "),
            Map.entry(Attribute.GENERIC_MOVEMENT_SPEED, "Fast "),
            Map.entry(Attribute.GENERIC_LUCK, "Lucky "),
            Map.entry(Attribute.GENERIC_KNOCKBACK_RESISTANCE, "Immovable ")
    );

    private static final Map<Attribute, String> SUFFIXES = Map.ofEntries(
            Map.entry(Attribute.GENERIC_MAX_HEALTH, " of the Tank"),
            Map.entry(Attribute.GENERIC_FOLLOW_RANGE, " of the Lookout"),
            Map.entry(Attribute.GENERIC_ATTACK_KNOCKBACK, " of the Boxer"),
            Map.entry(Attribute.GENERIC_ATTACK_SPEED, " of Dexterity"),
            Map.entry(Attribute.GENERIC_MOVEMENT_SPEED, " of the Runner"),
            Map.entry(Attribute.GENERIC_KNOCKBACK_RESISTANCE, " of Obesity")
    );

    private final AttributeConfig attributeConfig;
    private final EnchantmentConfig enchantmentConfig;
    private final Random random = new Random();
    public AttributeService(AttributeConfig attributeConfig, EnchantmentConfig enchantmentConfig) {
        this.attributeConfig = attributeConfig;
        this.enchantmentConfig = enchantmentConfig;
    }

    public ItemStack applyAttributes(ItemStack stack, Supplier<Integer> nightSupplier) {
        if (stack == null) return null;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;
        boolean changed = false;
        do {
            Attribute attribute = randomAttribute();
            if (attribute == null) break;
            double amount = attributeConfig.bonusPercent();
            AttributeModifier modifier = new AttributeModifier(UUID.randomUUID(), "attributeitemutils", amount, AttributeModifier.Operation.MULTIPLY_SCALAR_1, EquipmentSlot.HAND);
            meta.addAttributeModifier(attribute, modifier);
            changed = true;
            decorateName(stack, meta, attribute);
        } while (roll(attributeConfig, nightSupplier.get()));

        if (changed) {
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public ItemStack applyEnchants(ItemStack stack, Supplier<Integer> nightSupplier) {
        if (stack == null) return null;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;
        boolean changed = false;
        do {
            Enchantment enchantment = randomEnchantment(stack);
            if (enchantment == null) break;
            int level = meta.getEnchantLevel(enchantment) + enchantmentConfig.levelBonus();
            meta.addEnchant(enchantment, level, true);
            changed = true;
        } while (roll(enchantmentConfig, nightSupplier.get()));

        if (changed) {
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private boolean roll(AttributeConfig config, int nights) {
        double chance = Math.min(config.maxChance(), config.baseChance() + (config.nightlyIncrease() * nights));
        return random.nextDouble() < chance;
    }

    private boolean roll(EnchantmentConfig config, int nights) {
        double chance = Math.min(config.maxChance(), config.baseChance() + (config.nightlyIncrease() * nights));
        return random.nextDouble() < chance;
    }

    private void decorateName(ItemStack stack, ItemMeta meta, Attribute attribute) {
        String display = Optional.ofNullable(meta.getDisplayName()).orElse(meta.getLocalizedName());
        if (display == null || display.isEmpty()) {
            display = stack.getType().name();
        }
        String prefix = PREFIXES.getOrDefault(attribute, "");
        String suffix = SUFFIXES.getOrDefault(attribute, "");
        meta.setDisplayName(prefix + display + suffix);
    }

    private Attribute randomAttribute() {
        Attribute[] candidates = new Attribute[]{
                Attribute.GENERIC_MAX_HEALTH,
                Attribute.GENERIC_MOVEMENT_SPEED,
                Attribute.GENERIC_ATTACK_SPEED,
                Attribute.GENERIC_ATTACK_DAMAGE,
                Attribute.GENERIC_KNOCKBACK_RESISTANCE,
                Attribute.GENERIC_LUCK,
                Attribute.GENERIC_FOLLOW_RANGE
        };
        return candidates[random.nextInt(candidates.length)];
    }

    private Enchantment randomEnchantment(ItemStack stack) {
        List<Enchantment> valid = Arrays.stream(Enchantment.values())
                .filter(e -> e.canEnchantItem(stack))
                .toList();
        if (valid.isEmpty()) return null;
        return valid.get(random.nextInt(valid.size()));
    }
}
