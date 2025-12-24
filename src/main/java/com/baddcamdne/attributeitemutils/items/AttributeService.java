package com.baddcamdne.attributeitemutils.items;

import com.baddcamdne.attributeitemutils.config.AttributeBonus;
import com.baddcamdne.attributeitemutils.config.AttributeConfig;
import com.baddcamdne.attributeitemutils.config.AttributeAffixConfig;
import com.baddcamdne.attributeitemutils.config.AttributePoolConfig;
import com.baddcamdne.attributeutils.AttributeFacade;
import com.google.common.collect.Multimap;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

public class AttributeService {
    private final AttributeAffixConfig affixConfig;
    private final AttributePoolConfig attributePool;
    private final AttributeFacade attributeFacade;
    private final Random random;
    public AttributeService(AttributeFacade attributeFacade, AttributeAffixConfig affixConfig, AttributePoolConfig attributePool) {
        this(attributeFacade, affixConfig, attributePool, new Random());
    }

    AttributeService(AttributeFacade attributeFacade, AttributeAffixConfig affixConfig, AttributePoolConfig attributePool, Random random) {
        this.attributeFacade = attributeFacade;
        this.affixConfig = affixConfig;
        this.attributePool = attributePool;
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

    private Optional<AttributeBonus> randomAttribute() {
        return attributePool.random(random);
    }

    Map<Attribute, Double> collectAttributeBonuses(AttributeConfig config, int nights) {
        Map<Attribute, Double> bonuses = new LinkedHashMap<>();
        if (!roll(config, nights)) {
            return bonuses;
        }

        do {
            Optional<AttributeBonus> attributeBonus = randomAttribute();
            if (attributeBonus.isEmpty()) {
                break;
            }
            AttributeBonus bonus = attributeBonus.get();
            double amount = bonus.bonusPercent() == 0.0 ? config.bonusPercent() : bonus.bonusPercent();
            bonuses.merge(bonus.attribute(), amount, Double::sum);
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
