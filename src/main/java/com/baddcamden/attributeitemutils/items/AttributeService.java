package com.baddcamden.attributeitemutils.items;

import com.baddcamden.attributeitemutils.config.AttributeAffixConfig;
import com.baddcamden.attributeitemutils.config.AttributeAffixConfig.AttributeAffix;
import com.baddcamden.attributeitemutils.config.AttributeAffixConfig.PrefixSelection;
import com.baddcamden.attributeitemutils.config.AttributeBonus;
import com.baddcamden.attributeitemutils.config.AttributeConfig;
import com.baddcamden.attributeitemutils.config.AttributeLoreConfig;
import com.baddcamden.attributeitemutils.config.AttributePoolConfig;
import com.baddcamden.attributeutils.AttributeFacade;
import com.google.common.collect.Multimap;
import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

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
        String typeName = stack.getType().name();
        if (display == null || display.isEmpty()) {
            display = formatMaterialName(typeName);
        } else if (display.equalsIgnoreCase(typeName)) {
            display = formatMaterialName(display);
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
        meta.setDisplayName(ChatColor.RESET+decoratedName.toString());
    }

    private String formatMaterialName(String materialName) {
        return Arrays.stream(materialName.split("_"))
                .filter(part -> !part.isEmpty())
                .map(part -> part.substring(0, 1).toUpperCase(Locale.ENGLISH) + part.substring(1).toLowerCase(Locale.ENGLISH))
                .collect(Collectors.joining(" "));
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
