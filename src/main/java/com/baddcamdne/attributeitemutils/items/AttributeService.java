package com.baddcamdne.attributeitemutils.items;

import com.baddcamdne.attributeitemutils.config.AttributeConfig;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class AttributeService {
    private final Map<Attribute, String> prefixes;
    private final Map<Attribute, String> suffixes;
    private static final List<Attribute> CANDIDATE_ATTRIBUTES = List.of(
            Attribute.MAX_HEALTH,
            Attribute.GENERIC_ARMOR,
            Attribute.GENERIC_ARMOR_TOUGHNESS,
            Attribute.GENERIC_ATTACK_DAMAGE,
            Attribute.GENERIC_ATTACK_SPEED,
            Attribute.GENERIC_ATTACK_KNOCKBACK,
            Attribute.GENERIC_MOVEMENT_SPEED,
            Attribute.GENERIC_FLYING_SPEED,
            Attribute.GENERIC_KNOCKBACK_RESISTANCE,
            Attribute.GENERIC_LUCK,
            Attribute.GENERIC_FOLLOW_RANGE,
            Attribute.GENERIC_BLOCK_INTERACTION_RANGE,
            Attribute.GENERIC_ENTITY_INTERACTION_RANGE,
            Attribute.GENERIC_MINING_EFFICIENCY,
            Attribute.GENERIC_MAX_ABSORPTION,
            Attribute.GENERIC_STEP_HEIGHT,
            Attribute.GENERIC_SAFE_FALL_DISTANCE,
            Attribute.GENERIC_SCALE,
            Attribute.GENERIC_JUMP_STRENGTH,
            Attribute.GENERIC_GRAVITY,
            Attribute.GENERIC_FALL_DAMAGE_MULTIPLIER
    );

    private final Random random;
    public AttributeService(Map<Attribute, String> prefixes, Map<Attribute, String> suffixes) {
        this(prefixes, suffixes, new Random());
    }

    AttributeService(Map<Attribute, String> prefixes, Map<Attribute, String> suffixes, Random random) {
        this.prefixes = prefixes;
        this.suffixes = suffixes;
        this.random = random;
    }

    public ItemStack applyAttributes(ItemStack stack, AttributeConfig config, int nights) {
        if (stack == null) return null;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;
        Map<Attribute, Double> bonuses = collectAttributeBonuses(config, nights);
        if (!bonuses.isEmpty()) {
            EquipmentSlot slot = determineSlot(stack);
            for (Map.Entry<Attribute, Double> entry : bonuses.entrySet()) {
                double amount = resolveAmount(entry.getKey(), entry.getValue());
                AttributeModifier modifier = new AttributeModifier(UUID.randomUUID(), "attributeitemutils", amount, AttributeModifier.Operation.MULTIPLY_SCALAR_1, slot);
                meta.addAttributeModifier(entry.getKey(), modifier);
            }
            Attribute decoratedAttribute = bonuses.keySet().iterator().next();
            decorateName(stack, meta, decoratedAttribute);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    boolean roll(AttributeConfig config, int nights) {
        double chance = Math.min(config.maxChance(), config.baseChance() + (config.nightlyIncrease() * nights));
        return random.nextDouble() < chance;
    }

    private void decorateName(ItemStack stack, ItemMeta meta, Attribute attribute) {
        String display = Optional.ofNullable(meta.getDisplayName()).orElse(meta.getLocalizedName());
        if (display == null || display.isEmpty()) {
            display = stack.getType().name();
        }
        String prefix = prefixes.getOrDefault(attribute, "");
        String suffix = suffixes.getOrDefault(attribute, "");
        meta.setDisplayName(prefix + display + suffix);
    }

    private Attribute randomAttribute() {
        return CANDIDATE_ATTRIBUTES.get(random.nextInt(CANDIDATE_ATTRIBUTES.size()));
    }

    EquipmentSlot determineSlot(ItemStack stack) {
        Material type = stack.getType();
        String name = type.name();

        if (name.endsWith("_HELMET") || type == Material.TURTLE_HELMET || type == Material.CARVED_PUMPKIN) {
            return EquipmentSlot.HEAD;
        }
        if (name.endsWith("_CHESTPLATE") || type == Material.ELYTRA) {
            return EquipmentSlot.CHEST;
        }
        if (name.endsWith("_LEGGINGS")) {
            return EquipmentSlot.LEGS;
        }
        if (name.endsWith("_BOOTS")) {
            return EquipmentSlot.FEET;
        }
        if (type == Material.SHIELD || type == Material.TOTEM_OF_UNDYING) {
            return EquipmentSlot.OFF_HAND;
        }

        return EquipmentSlot.HAND;
    }

    Map<Attribute, Double> collectAttributeBonuses(AttributeConfig config, int nights) {
        Map<Attribute, Double> bonuses = new LinkedHashMap<>();
        if (!roll(config, nights)) {
            return bonuses;
        }

        do {
            Attribute attribute = randomAttribute();
            bonuses.merge(attribute, config.bonusPercent(), Double::sum);
        } while (roll(config, nights));
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

}
