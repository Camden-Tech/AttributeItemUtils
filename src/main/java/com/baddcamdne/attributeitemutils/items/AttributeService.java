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
            Map.entry(Attribute.GENERIC_MAX_HEALTH, "Bulky "),
            Map.entry(Attribute.GENERIC_ARMOR, "Hardened "),
            Map.entry(Attribute.GENERIC_ARMOR_TOUGHNESS, "Tough "),
            Map.entry(Attribute.GENERIC_ATTACK_DAMAGE, "Strengthening "),
            Map.entry(Attribute.GENERIC_ATTACK_SPEED, "Speedster "),
            Map.entry(Attribute.GENERIC_ATTACK_KNOCKBACK, "Forceful "),
            Map.entry(Attribute.GENERIC_MOVEMENT_SPEED, "Fast "),
            Map.entry(Attribute.GENERIC_KNOCKBACK_RESISTANCE, "Immovable "),
            Map.entry(Attribute.GENERIC_LUCK, "Lucky "),
            Map.entry(Attribute.GENERIC_FOLLOW_RANGE, "Watchful "),
            Map.entry(Attribute.GENERIC_SCALE, "Giant "),
            Map.entry(Attribute.GENERIC_FALL_DAMAGE_MULTIPLIER, "Featherweight ")
    );

    private static final Map<Attribute, String> SUFFIXES = Map.ofEntries(
            Map.entry(Attribute.GENERIC_MAX_HEALTH, " of the Tank"),
            Map.entry(Attribute.GENERIC_ARMOR, " of the Bulwark"),
            Map.entry(Attribute.GENERIC_ARMOR_TOUGHNESS, " of Fortitude"),
            Map.entry(Attribute.GENERIC_ATTACK_DAMAGE, " of Power"),
            Map.entry(Attribute.GENERIC_ATTACK_KNOCKBACK, " of the Boxer"),
            Map.entry(Attribute.GENERIC_ATTACK_SPEED, " of Dexterity"),
            Map.entry(Attribute.GENERIC_MOVEMENT_SPEED, " of the Runner"),
            Map.entry(Attribute.GENERIC_KNOCKBACK_RESISTANCE, " of Obesity"),
            Map.entry(Attribute.GENERIC_LUCK, " of Fortune"),
            Map.entry(Attribute.GENERIC_FOLLOW_RANGE, " of the Lookout"),
            Map.entry(Attribute.GENERIC_SCALE, " of the Giant"),
            Map.entry(Attribute.GENERIC_FALL_DAMAGE_MULTIPLIER, " of Soft Landings")
    );

    private static final List<Attribute> CANDIDATE_ATTRIBUTES = List.of(
            Attribute.GENERIC_MAX_HEALTH,
            Attribute.GENERIC_ARMOR,
            Attribute.GENERIC_ARMOR_TOUGHNESS,
            Attribute.GENERIC_ATTACK_DAMAGE,
            Attribute.GENERIC_ATTACK_SPEED,
            Attribute.GENERIC_ATTACK_KNOCKBACK,
            Attribute.GENERIC_MOVEMENT_SPEED,
            Attribute.GENERIC_KNOCKBACK_RESISTANCE,
            Attribute.GENERIC_LUCK,
            Attribute.GENERIC_FOLLOW_RANGE,
            Attribute.GENERIC_SCALE,
            Attribute.GENERIC_FALL_DAMAGE_MULTIPLIER
    );

    private final AttributeConfig attributeConfig;
    private final EnchantmentConfig enchantmentConfig;
    private final Random random;
    public AttributeService(AttributeConfig attributeConfig, EnchantmentConfig enchantmentConfig) {
        this(attributeConfig, enchantmentConfig, new Random());
    }

    AttributeService(AttributeConfig attributeConfig, EnchantmentConfig enchantmentConfig, Random random) {
        this.attributeConfig = attributeConfig;
        this.enchantmentConfig = enchantmentConfig;
        this.random = random;
    }

    public ItemStack applyAttributes(ItemStack stack, Supplier<Integer> nightSupplier) {
        if (stack == null) return null;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;
        Map<Attribute, Double> bonuses = collectAttributeBonuses(nightSupplier);
        if (!bonuses.isEmpty()) {
            for (Map.Entry<Attribute, Double> entry : bonuses.entrySet()) {
                double amount = resolveAmount(entry.getKey(), entry.getValue());
                AttributeModifier modifier = new AttributeModifier(UUID.randomUUID(), "attributeitemutils", amount, AttributeModifier.Operation.MULTIPLY_SCALAR_1, EquipmentSlot.HAND);
                meta.addAttributeModifier(entry.getKey(), modifier);
            }
            Attribute decoratedAttribute = bonuses.keySet().iterator().next();
            decorateName(stack, meta, decoratedAttribute);
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
        return CANDIDATE_ATTRIBUTES.get(random.nextInt(CANDIDATE_ATTRIBUTES.size()));
    }

    Map<Attribute, Double> collectAttributeBonuses(Supplier<Integer> nightSupplier) {
        Map<Attribute, Double> bonuses = new LinkedHashMap<>();
        do {
            Attribute attribute = randomAttribute();
            bonuses.merge(attribute, attributeConfig.bonusPercent(), Double::sum);
        } while (roll(attributeConfig, nightSupplier.get()));
        return bonuses;
    }

    double resolveAmount(Attribute attribute, double baseAmount) {
        if (attribute == Attribute.GENERIC_SCALE) {
            return Math.cbrt(1 + baseAmount) - 1;
        }
        if (attribute == Attribute.GENERIC_FALL_DAMAGE_MULTIPLIER) {
            return -baseAmount;
        }
        return baseAmount;
    }

    private Enchantment randomEnchantment(ItemStack stack) {
        List<Enchantment> valid = Arrays.stream(Enchantment.values())
                .filter(e -> e.canEnchantItem(stack))
                .toList();
        if (valid.isEmpty()) return null;
        return valid.get(random.nextInt(valid.size()));
    }
}
