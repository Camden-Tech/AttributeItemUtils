package com.baddcamdne.attributeitemutils.items;

import com.baddcamdne.attributeitemutils.config.AttributeAffixConfig;
import com.baddcamdne.attributeitemutils.config.AttributeAffixConfig.AttributeAffix;
import com.baddcamdne.attributeitemutils.config.AttributeAffixConfig.PrefixSelection;
import com.baddcamdne.attributeitemutils.config.AttributeBonus;
import com.baddcamdne.attributeitemutils.config.AttributeConfig;
import com.baddcamdne.attributeitemutils.config.AttributeLoreConfig;
import com.baddcamdne.attributeitemutils.config.AttributePoolConfig;
import com.baddcamdne.attributeutils.AttributeFacade;
import com.google.common.collect.Multimap;
import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

public class AttributeService {
    private final AttributeAffixConfig affixConfig;
    private final AttributePoolConfig attributePool;
    private final AttributeFacade attributeFacade;
    private final AttributeLoreConfig attributeLoreConfig;
    private final Random random;
    public AttributeService(AttributeFacade attributeFacade, AttributeAffixConfig affixConfig, AttributePoolConfig attributePool, AttributeLoreConfig attributeLoreConfig) {
        this(attributeFacade, affixConfig, attributePool, attributeLoreConfig, new Random());
    }

    AttributeService(AttributeFacade attributeFacade, AttributeAffixConfig affixConfig, AttributePoolConfig attributePool, AttributeLoreConfig attributeLoreConfig, Random random) {
        this.attributeFacade = attributeFacade;
        this.affixConfig = affixConfig;
        this.attributePool = attributePool;
        this.attributeLoreConfig = attributeLoreConfig;
        this.random = random;
    }

    AttributeService(AttributeFacade attributeFacade, AttributeAffixConfig affixConfig, AttributePoolConfig attributePool, Random random, AttributeLoreConfig attributeLoreConfig) {
        this(attributeFacade, affixConfig, attributePool, attributeLoreConfig, random);
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
            decorateLore(decoratedMeta);
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

        boolean fallback = false;
        Optional<AttributeAffix> suffix = Optional.empty();
        PrefixSelection prefixSelection = new PrefixSelection(List.of(), false);
        try {
            prefixSelection = affixConfig.matchingPrefixes(appliedAttributes);
            suffix = affixConfig.matchingSuffix(appliedAttributes);
        } catch (Exception ex) {
            fallback = true;
        }

        if (!fallback && prefixSelection.prefixes().isEmpty() && suffix.isEmpty()) {
            return;
        }

        String display = Optional.ofNullable(meta.getDisplayName()).orElse(meta.getLocalizedName());
        if (display == null || display.isEmpty()) {
            display = stack.getType().name();
        }

        StringBuilder decoratedName = new StringBuilder();
        if (fallback) {
            decoratedName.append(affixConfig.defaultPrefix());
        } else {
            prefixSelection.prefixes().stream()
                    .map(AttributeAffix::value)
                    .forEach(decoratedName::append);
        }
        decoratedName.append(display);
        if (!fallback) {
            suffix.map(AttributeAffix::value).ifPresent(decoratedName::append);
        }
        meta.setDisplayName(decoratedName.toString());
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

    private void decorateLore(ItemMeta meta) {
        if (meta.getAttributeModifiers() == null || meta.getAttributeModifiers().isEmpty()) {
            return;
        }

        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        List<String> attributeLore = attributeLoreConfig.buildLore(meta);
        if (attributeLore.isEmpty()) {
            return;
        }

        if (!lore.isEmpty()) {
            lore.add("");
            lore.add(ChatColor.DARK_GRAY + "―――――――――――――――――――――――――――――――――――――――");
        }
        lore.addAll(attributeLore);
        meta.setLore(lore);
    }

}
