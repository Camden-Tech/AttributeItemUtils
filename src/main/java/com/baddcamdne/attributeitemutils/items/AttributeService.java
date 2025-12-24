package com.baddcamdne.attributeitemutils.items;

import com.baddcamdne.attributeitemutils.config.AttributeConfig;
import com.baddcamdne.attributeutils.AttributeFacade;
import org.bukkit.attribute.Attribute;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

public class AttributeService {
    private final Map<Attribute, String> prefixes;
    private final Map<Attribute, String> suffixes;
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
    public AttributeService(AttributeFacade attributeFacade, Map<Attribute, String> prefixes, Map<Attribute, String> suffixes) {
        this(attributeFacade, prefixes, suffixes, new Random());
    }

    AttributeService(AttributeFacade attributeFacade, Map<Attribute, String> prefixes, Map<Attribute, String> suffixes, Random random) {
        this.attributeFacade = attributeFacade;
        this.prefixes = prefixes;
        this.suffixes = suffixes;
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
        Attribute decoratedAttribute = bonuses.keySet().iterator().next();
        ItemMeta decoratedMeta = stack.getItemMeta();
        if (decoratedMeta != null) {
            decorateName(stack, decoratedMeta, decoratedAttribute);
            stack.setItemMeta(decoratedMeta);
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

}
