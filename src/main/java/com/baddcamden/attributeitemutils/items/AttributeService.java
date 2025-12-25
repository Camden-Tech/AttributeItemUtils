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
import com.baddcamden.attributeitemutils.util.NightCalculator;

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
import java.util.logging.Logger;

public class AttributeService {
    // Provides the configured prefix/suffix text that decorates attribute-heavy item names.
    private final AttributeAffixConfig affixConfig;
    // Supplies randomizable attribute options and rolling weights for item generation.
    private final AttributePoolConfig attributePool;
    // Applies attribute math and modifier wiring to item metadata.
    private final AttributeFacade attributeFacade;
    // Builds lore lines summarizing applied attributes for players.
    private final AttributeLoreConfig attributeLoreConfig;
    // Centralized RNG so seeded tests and production follow the same flow.
    private final Random random;
    // Logger used for temporary debugging around attribute application.
    private final Logger logger;
    /**
     * Creates an attribute service using the provided facade and configs with a new random number generator.
     */
    public AttributeService(AttributeFacade attributeFacade, AttributeAffixConfig affixConfig, AttributePoolConfig attributePool, AttributeLoreConfig attributeLoreConfig) {
        this(attributeFacade, affixConfig, attributePool, attributeLoreConfig, new Random(), Logger.getLogger(AttributeService.class.getName()));
    }

    public AttributeService(AttributeFacade attributeFacade, AttributeAffixConfig affixConfig, AttributePoolConfig attributePool, AttributeLoreConfig attributeLoreConfig, Logger logger) {
        this(attributeFacade, affixConfig, attributePool, attributeLoreConfig, new Random(), logger);
    }

    /**
     * Package-private constructor primarily used for testing that allows injecting a specific {@link Random} instance.
     */
    AttributeService(AttributeFacade attributeFacade, AttributeAffixConfig affixConfig, AttributePoolConfig attributePool, AttributeLoreConfig attributeLoreConfig, Random random) {
        this(attributeFacade, affixConfig, attributePool, attributeLoreConfig, random, Logger.getLogger(AttributeService.class.getName()));
    }

    AttributeService(AttributeFacade attributeFacade, AttributeAffixConfig affixConfig, AttributePoolConfig attributePool, AttributeLoreConfig attributeLoreConfig, Random random, Logger logger) {
        this.attributeFacade = attributeFacade;
        this.affixConfig = affixConfig;
        this.attributePool = attributePool;
        this.attributeLoreConfig = attributeLoreConfig;
        this.random = random;
        this.logger = logger;
    }

    /**
     * Alternative package-private constructor kept for backwards compatibility when parameter ordering differs.
     */
    AttributeService(AttributeFacade attributeFacade, AttributeAffixConfig affixConfig, AttributePoolConfig attributePool, Random random, AttributeLoreConfig attributeLoreConfig) {
        this(attributeFacade, affixConfig, attributePool, attributeLoreConfig, random);
    }

    /**
     * Applies randomly rolled attribute bonuses and related lore/decorations to the given item stack.
     */
    public ItemStack applyAttributes(ItemStack stack, AttributeConfig config, EquipmentSlot slot, long nights) {
        if (stack == null) {
            return null;
        }

        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return stack;
        }
        logger.info(() -> "[ATTR] Starting attribute application for " + describeStack(stack, slot) + " at night " + nights +
                " with base meta " + serialize(stack));
        Map<Attribute, Double> bonuses = collectAttributeBonuses(config, NightCalculator.clampNights(nights));
        if (bonuses.isEmpty()) {
            logger.info(() -> "[ATTR] No attribute bonuses rolled for " + describeStack(stack, slot));
            return stack;
        }
        logger.info(() -> "[ATTR] Rolled bonuses for " + describeStack(stack, slot) + ": " + bonuses);
        attributeFacade.refresh(stack, slot);
        logModifiers("[ATTR] After refresh", stack.getItemMeta());
        bonuses.forEach((attribute, amount) -> attributeFacade.mutate(stack, attribute, amount, slot));
        meta = stack.getItemMeta();
        if (meta == null) {
            return stack;
        }

        decorateName(stack, meta);
        decorateLore(meta);
        stack.setItemMeta(meta);
        logger.info("[ATTR] Completed attribute application for " + describeStack(stack, slot) + " with modifiers " +
                summarizeModifiers(meta) + " and NBT " + serialize(stack));
        return stack;
    }

    /**
     * Determines whether another attribute bonus should be applied based on nightly scaling chance.
     */
    boolean roll(AttributeConfig config, long nights) {
        long clampedNights = NightCalculator.clampNights(nights);
        double chance = Math.min(config.maxChance(), config.baseChance() + (config.nightlyIncrease() * clampedNights));
        double effectiveChance = Math.min(chance, Math.nextAfter(1.0d, 0.0d));
        return random.nextDouble() < effectiveChance;
    }

    /**
     * Adds prefix and suffix affixes to an item's display name when attributes have been applied.
     */
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
            //VAGUE/IMPROVEMENT NEEDED Exceptions are swallowed without logging; clarify expected failures or record the cause.
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
        meta.setDisplayName(ChatColor.RESET + decoratedName.toString());
    }

    /**
     * Formats an enum-style material name into a spaced and capitalized display string.
     */
    private String formatMaterialName(String materialName) {
        return Arrays.stream(materialName.split("_"))
                .filter(part -> !part.isEmpty())
                .map(part -> part.substring(0, 1).toUpperCase(Locale.ENGLISH) + part.substring(1).toLowerCase(Locale.ENGLISH))
                .collect(Collectors.joining(" "));
    }

    /**
     * Selects a random attribute bonus from the configured pool.
     */
    private Optional<AttributeBonus> randomAttribute() {
        return attributePool.random(random);
    }

    /**
     * Rolls for attribute bonuses until the chance fails and aggregates the resulting amounts by attribute.
     */
    Map<Attribute, Double> collectAttributeBonuses(AttributeConfig config, long nights) {
        Map<Attribute, Double> bonuses = new LinkedHashMap<>();
        if (attributePool.attributes().isEmpty() || !roll(config, nights)) {
            return bonuses;
        }

        int remainingApplications = Math.max(1, attributePool.attributes().size());
        //VAGUE/IMPROVEMENT NEEDED {Number of rolls scales with pool size without clear balance rationale}
        do {
            Optional<AttributeBonus> attributeBonus = randomAttribute();
            if (attributeBonus.isEmpty()) {
                break;
            }
            AttributeBonus bonus = attributeBonus.get();
            double amount = bonus.bonusPercent() == 0.0 ? config.bonusPercent() : bonus.bonusPercent();
            bonuses.merge(bonus.attribute(), amount, Double::sum);
        } while (--remainingApplications > 0 && roll(config, nights));
        return bonuses;
    }

    /**
     * Computes the final modifier amount for the given attribute using the attribute facade.
     */
    double resolveAmount(Attribute attribute, double baseAmount) {
        return attributeFacade.computeAmount(attribute, baseAmount);
    }

    /**
     * Extracts attributes already applied to the item meta that have a non-zero modifier.
     */
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

    /**
     * Appends lore entries describing applied attributes, including separators when lore already exists.
     */
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

    private String describeStack(ItemStack stack, EquipmentSlot slot) {
        return (slot == null ? "unknown" : slot.name()) + " stack " + (stack == null ? "null" : stack.getType().name());
    }

    private String summarizeModifiers(ItemMeta meta) {
        Multimap<Attribute, AttributeModifier> modifiers = meta == null ? null : meta.getAttributeModifiers();
        if (modifiers == null || modifiers.isEmpty()) {
            return "[]";
        }
        return modifiers.entries().stream()
                .map(entry -> entry.getKey().name() + "=" + entry.getValue().getAmount() + "@" + entry.getValue().getOperation()
                        + " slot=" + entry.getValue().getSlot())
                .collect(Collectors.joining(", ", "[", "]"));
    }

    private void logModifiers(String prefix, ItemMeta meta) {
        logger.info(() -> prefix + " modifiers " + summarizeModifiers(meta));
    }

    private String serialize(ItemStack stack) {
        return stack == null ? "null" : stack.serialize().toString();
    }

}
