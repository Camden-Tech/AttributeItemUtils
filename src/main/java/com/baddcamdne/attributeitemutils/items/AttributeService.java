package com.baddcamdne.attributeitemutils.items;

import com.baddcamdne.attributeitemutils.config.AttributeConfig;
import com.baddcamdne.attributeitemutils.config.AttributeAffixConfig;
import com.baddcamdne.attributeutils.AttributeFacade;
import com.google.common.collect.Multimap;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

public class AttributeService {
    private final AttributeAffixConfig affixConfig;
    public static final List<Attribute> CANDIDATE_ATTRIBUTES = List.of(
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

    private final AttributeFacade attributeFacade;
    private final Random random;
    public AttributeService(AttributeFacade attributeFacade, AttributeAffixConfig affixConfig) {
        this(attributeFacade, affixConfig, new Random());
    }

    AttributeService(AttributeFacade attributeFacade, AttributeAffixConfig affixConfig, Random random) {
        this.attributeFacade = attributeFacade;
        this.affixConfig = affixConfig;
        this.random = random;
    }

    public ItemStack applyAttributes(ItemStack stack, AttributeConfig config, EquipmentSlot slot, int nights) {
        if (stack == null) return null;
        if (stack.getItemMeta() == null) return stack;
        Map<Attribute, Double> bonuses = collectAttributeBonuses(config, nights);
        if (bonuses.isEmpty()) {
            return stack;
        }

        attributeFacade.refresh(stack, slot);
        bonuses.forEach((attribute, amount) -> attributeFacade.mutate(stack, attribute, amount, slot));
        ItemMeta decoratedMeta = stack.getItemMeta();
        if (decoratedMeta != null) {
            decorateName(stack, decoratedMeta);
            stack.setItemMeta(decoratedMeta);
        }
        return stack;
    }

    boolean roll(AttributeConfig config, int nights) {
        double chance = Math.min(config.maxChance(), config.baseChance() + (config.nightlyIncrease() * nights));
        return random.nextDouble() < chance;
    }

    private void decorateName(ItemStack stack, ItemMeta meta) {
        Set<Attribute> appliedAttributes = appliedAttributes(meta);
        if (appliedAttributes.isEmpty()) {
            return;
        }

        Optional<String> prefix = affixConfig.prefixFor(appliedAttributes);
        Optional<String> suffix = affixConfig.suffixFor(appliedAttributes);

        if (prefix.isEmpty() && suffix.isEmpty()) {
            return;
        }

        String display = Optional.ofNullable(meta.getDisplayName()).orElse(meta.getLocalizedName());
        if (display == null || display.isEmpty()) {
            display = stack.getType().name();
        }
        meta.setDisplayName(prefix.orElse("") + display + suffix.orElse(""));
    }

    private Attribute randomAttribute() {
        return CANDIDATE_ATTRIBUTES.get(random.nextInt(CANDIDATE_ATTRIBUTES.size()));
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
        return attributeFacade.computeAmount(attribute, baseAmount);
    }

    private Set<Attribute> appliedAttributes(ItemMeta meta) {
        Multimap<Attribute, AttributeModifier> modifiers = meta.getAttributeModifiers();
        if (modifiers == null) {
            return Set.of();
        }

        Set<Attribute> attributes = new HashSet<>();
        for (Map.Entry<Attribute, AttributeModifier> entry : modifiers.entries()) {
            if (entry.getValue().getAmount() != 0.0) {
                attributes.add(entry.getKey());
            }
        }
        return attributes;
    }

}
