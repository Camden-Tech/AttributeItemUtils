package com.baddcamden.attributeitemutils.items;

import com.baddcamden.attributeitemutils.config.AttributeChanceConfig;
import com.baddcamden.attributeitemutils.config.DropChanceConfigSource;
import com.baddcamden.attributeitemutils.config.EnchantChanceConfig;
import com.baddcamden.attributeitemutils.gear.GearConfigLoader;
import com.baddcamden.attributeitemutils.gear.GearSlot;
import com.baddcamden.attributeitemutils.gear.KitConfig;
import com.baddcamden.attributeitemutils.gear.WeightedItem;
import com.baddcamden.attributeitemutils.util.BellCurveSelector;
import com.baddcamden.attributeitemutils.util.NightCalculator;
import com.baddcamden.attributeitemutils.hooks.EntityChanceHooks;
import me.baddcamden.attributeutils.AttributeUtilitiesPlugin;
import me.baddcamden.attributeutils.api.AttributeFacade;
import me.baddcamden.attributeutils.command.CommandParsingUtils;
import me.baddcamden.attributeutils.handler.entity.EntityAttributeHandler;
import me.baddcamden.attributeutils.handler.item.ItemAttributeHandler;
import me.baddcamden.attributeutils.handler.item.TriggerCriterion;
import me.baddcamden.attributeutils.model.AttributeDefinition;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class GearService {
    // Loads configured kits and weighted item selections from disk.
    private final GearConfigLoader loader;
    // Provides drop chance defaults and overrides for entity equipment.
    private final DropChanceConfigSource dropChanceConfigSource;
    // Publishes hooks that may override attribute/enchant/drop settings per entity type.
    private final EntityChanceHooks chanceHooks;
    private final AttributeUtilitiesPlugin attributeUtils;
    private final AttributeFacade attributeFacade;
    private final ItemAttributeHandler itemAttributeHandler;
    private final EntityAttributeHandler entityAttributeHandler;
    private final AttributeAffixConfig attributeAffixConfig;
    private final AttributeLoreFormatter attributeLoreFormatter;
    private final AttributeChanceConfig attributeChanceConfig;
    private final EnchantChanceConfig enchantChanceConfig;
    private final EnchantmentPool enchantmentPool;
    private final Random random = new Random();
    private final BellCurveSelector selector = new BellCurveSelector();
    private final Logger logger;
    /**
     * Constructs the gear service with all supporting services and config sources used when populating entity equipment.
     */
    public GearService(GearConfigLoader loader,
                       DropChanceConfigSource dropChanceConfigSource,
                       EntityChanceHooks chanceHooks,
                       AttributeUtilitiesPlugin attributeUtils,
                       AttributeFacade attributeFacade,
                       ItemAttributeHandler itemAttributeHandler,
                       EntityAttributeHandler entityAttributeHandler,
                       AttributeAffixConfig attributeAffixConfig,
                       AttributeLoreFormatter attributeLoreFormatter,
                       AttributeChanceConfig attributeChanceConfig,
                       EnchantChanceConfig enchantChanceConfig,
                       EnchantmentPool enchantmentPool) {
        this(loader, dropChanceConfigSource, chanceHooks, attributeUtils, attributeFacade, itemAttributeHandler, entityAttributeHandler, attributeAffixConfig, attributeLoreFormatter, attributeChanceConfig, enchantChanceConfig, enchantmentPool, Logger.getLogger(GearService.class.getName()));
    }

    public GearService(GearConfigLoader loader,
                       DropChanceConfigSource dropChanceConfigSource,
                       EntityChanceHooks chanceHooks,
                       AttributeUtilitiesPlugin attributeUtils,
                       AttributeFacade attributeFacade,
                       ItemAttributeHandler itemAttributeHandler,
                       EntityAttributeHandler entityAttributeHandler,
                       AttributeAffixConfig attributeAffixConfig,
                       AttributeLoreFormatter attributeLoreFormatter,
                       AttributeChanceConfig attributeChanceConfig,
                       EnchantChanceConfig enchantChanceConfig,
                       EnchantmentPool enchantmentPool,
                       Logger logger) {
        this.loader = loader;
        this.dropChanceConfigSource = dropChanceConfigSource;
        this.chanceHooks = chanceHooks;
        this.attributeUtils = attributeUtils;
        this.attributeFacade = attributeFacade;
        this.itemAttributeHandler = itemAttributeHandler;
        this.entityAttributeHandler = entityAttributeHandler;
        this.attributeAffixConfig = attributeAffixConfig;
        this.attributeLoreFormatter = attributeLoreFormatter;
        this.attributeChanceConfig = attributeChanceConfig;
        this.enchantChanceConfig = enchantChanceConfig;
        this.enchantmentPool = enchantmentPool;
        this.logger = logger;
    }

    /**
     * Retrieves the configured kit definition by name if one exists.
     */
    public Optional<KitConfig> getKit(String name) {
        return loader.getKit(name);
    }

    /**
     * Populates the given entity's equipment using the provided kit and applies attributes, enchants, and drop chances.
     */
    public void applyKit(LivingEntity entity, KitConfig kit) {
        long nights = NightCalculator.nightsFromWorldTime(entity.getWorld().getFullTime());
        double dropChance = chanceHooks.dropChanceFor(entity.getType())
                .orElse(dropChanceConfigSource.defaultChance());
        Map<EquipmentSlot, ItemStack> equipment = new EnumMap<EquipmentSlot, ItemStack>(EquipmentSlot.class);
        for (GearSlot slot : GearSlot.values()) {
            EquipmentSlot equipmentSlot = mapSlot(slot);
            WeightedItem selection = selector.select(kit.items().getOrDefault(slot, java.util.List.of()), kit.targetWeight(), kit.steepness(), kit.range());
            Material material = selection == null ? Material.AIR : selection.material();
            ItemStack stack = material == Material.AIR ? null : new ItemStack(material);
            logger.info("[GEAR] Built base item for " + equipmentSlot + " -> " + (stack == null ? "none" : stack.getType().name()));
            if (stack != null) {
                logger.info("[GEAR] Pre-AttributeUtils build for " + equipmentSlot + ": " + summarize(stack));
                stack = buildAttributedItem(entity, stack.getType(), equipmentSlot, nights);
                logger.info("[GEAR] After AttributeUtils build " + equipmentSlot + ": " + summarize(stack));
            }
            equipment.put(equipmentSlot, stack);
        }
        EntityEquipment entityEquipment = entity.getEquipment();
        if (entityEquipment == null) {
            return;
        }
        equipment.forEach((slot, item) -> {
            entityEquipment.setItem(slot, item);
            if (item != null && item.getType() != Material.AIR) {
                setDropChance(entityEquipment, slot, (float) dropChance);
            }
        });
        itemAttributeHandler.applyPersistentAttributes(entity);
        if (entity instanceof Player player) {
            player.updateInventory();
        }
    }

    private ItemStack buildAttributedItem(LivingEntity entity, Material material, EquipmentSlot slot, long nights) {
        List<AttributeDefinition> definitions = attributeFacade.getDefinitions().stream().toList();
        double attributeChance = chanceHooks.attributeChanceFor(entity.getType())
                .orElse(attributeChanceConfig.chanceForNights(nights));
        double enchantChance = chanceHooks.enchantChanceFor(entity.getType())
                .orElse(enchantChanceConfig.chanceForNights(nights));

        List<AttributeRoll> rolls = randomRolls(definitions, slot, attributeChance);
        ItemStack baseItem = new ItemStack(material);

        if (!rolls.isEmpty()) {
            List<CommandParsingUtils.AttributeDefinition> parsedDefinitions = rolls.stream()
                    .map(AttributeRoll::parsedDefinition)
                    .toList();
            ItemAttributeHandler.ItemBuildResult result = itemAttributeHandler.buildAttributeItem(material, parsedDefinitions);

            LinkedHashSet<String> rolledAttributeIds = rolls.stream()
                    .map(AttributeRoll::attributeDefinition)
                    .map(AttributeDefinition::id)
                    .map(attributeLoreFormatter::normalizeAttributeId)
                    .map(attributeAffixConfig::normalizeAttributeKey)
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            ItemStack withAffixes = applyAffixes(result.itemStack(), rolledAttributeIds, material);
            baseItem = attributeLoreFormatter.rebuildLore(withAffixes, parsedDefinitions, attributeFacade, slot);
        }

        return applyEnchants(baseItem, enchantChance);
    }

    private List<AttributeRoll> randomRolls(List<AttributeDefinition> definitions, EquipmentSlot slot, double chance) {
        if (definitions.isEmpty() || chance <= 0d) {
            return List.of();
        }

        String criterion = criterionForSlot(slot);
        Map<String, AggregatedRoll> aggregatedRolls = new LinkedHashMap<>();

        while (rollChance(chance)) {
            AttributeDefinition definition = definitions.get(random.nextInt(definitions.size()));
            aggregatedRolls
                    .computeIfAbsent(definition.id(), ignored -> new AggregatedRoll(definition))
                    .addAmount(randomAmount());
        }

        return aggregatedRolls.values().stream()
                .map(roll -> roll.toAttributeRoll(resolveKey(roll.definition().id()), criterion))
                .toList();
    }

    private ItemStack applyEnchants(ItemStack itemStack, double enchantChance) {
        if (itemStack == null || enchantChance <= 0d) {
            return itemStack;
        }

        List<Enchantment> pool = enchantmentPool.enchantments();
        if (pool.isEmpty()) {
            return itemStack;
        }

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return itemStack;
        }

        boolean modified = false;
        while (rollChance(enchantChance)) {
            Enchantment enchantment = pool.get(random.nextInt(pool.size()));
            int level = rollEnchantmentLevel(enchantment);
            int existingLevel = meta.getEnchantLevel(enchantment);
            int appliedLevel = existingLevel > 0 ? Math.max(existingLevel, level) : level;
            meta.addEnchant(enchantment, appliedLevel, true);
            modified = true;
        }

        if (modified) {
            itemStack.setItemMeta(meta);
        }
        return itemStack;
    }

    private int rollEnchantmentLevel(Enchantment enchantment) {
        int maxLevel = enchantment.getMaxLevel();
        int maxRolledLevel = Math.min(maxLevel, Math.max(1, 1 + enchantChanceConfig.levelBonus()));
        return 1 + random.nextInt(maxRolledLevel);
    }

    private ItemStack applyAffixes(ItemStack itemStack, LinkedHashSet<String> attributeIds, Material material) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return itemStack;
        }

        String baseName = meta.hasDisplayName() && meta.getDisplayName() != null && !meta.getDisplayName().isBlank()
                ? meta.getDisplayName()
                : titleCase(material.name());

        List<AttributeAffixConfig.AffixEntry> matchedPrefixes = attributeAffixConfig.matchingPrefixes(attributeIds);
        List<AttributeAffixConfig.AffixEntry> prefixes = limitPrefixes(matchedPrefixes);
        if (prefixes.isEmpty()) {
            String defaultPrefix = attributeAffixConfig.defaultPrefix();
            if (defaultPrefix != null && !defaultPrefix.isBlank()) {
                prefixes = List.of(new AttributeAffixConfig.AffixEntry(Set.of(), defaultPrefix, Integer.MAX_VALUE));
            }
        }

        AttributeAffixConfig.AffixEntry suffix = attributeAffixConfig.bestSuffix(attributeIds);

        StringBuilder nameBuilder = new StringBuilder();
        prefixes.forEach(entry -> nameBuilder.append(entry.value()));
        nameBuilder.append(baseName);
        if (suffix != null) {
            nameBuilder.append(suffix.value());
        }

        meta.setDisplayName(nameBuilder.toString());
        itemStack.setItemMeta(meta);
        return itemStack;
    }

    private List<AttributeAffixConfig.AffixEntry> limitPrefixes(List<AttributeAffixConfig.AffixEntry> prefixes) {
        if (prefixes.size() <= 5) {
            return prefixes;
        }
        return prefixes.stream()
                .sorted(Comparator
                        .comparingInt((AttributeAffixConfig.AffixEntry entry) -> entry.attributes().size())
                        .reversed()
                        .thenComparingInt(AttributeAffixConfig.AffixEntry::order))
                .limit(5)
                .toList();
    }

    private String titleCase(String materialName) {
        String[] segments = materialName.toLowerCase(java.util.Locale.ROOT).split("_");
        return java.util.Arrays.stream(segments)
                .filter(segment -> !segment.isBlank())
                .map(segment -> Character.toUpperCase(segment.charAt(0)) + segment.substring(1))
                .collect(Collectors.joining(" "));
    }

    private static class AggregatedRoll {
        private final AttributeDefinition definition;
        private double amount = 0d;

        private AggregatedRoll(AttributeDefinition definition) {
            this.definition = definition;
        }

        private void addAmount(double additionalAmount) {
            amount += additionalAmount;
        }

        private AttributeDefinition definition() {
            return definition;
        }

        private AttributeRoll toAttributeRoll(CommandParsingUtils.NamespacedAttributeKey key, String criterion) {
            return new AttributeRoll(definition, new CommandParsingUtils.AttributeDefinition(key, amount, null, criterion));
        }
    }

    private record AttributeRoll(AttributeDefinition attributeDefinition, CommandParsingUtils.AttributeDefinition parsedDefinition) {
    }

    private double randomAmount() {
        return 0.25d + (1.25d * random.nextDouble());
    }

    private boolean rollChance(double chance) {
        double clampedChance = Math.max(0d, Math.min(chance, 0.999999d));
        if (clampedChance <= 0d) {
            return false;
        }
        return random.nextDouble() < clampedChance;
    }

    private CommandParsingUtils.NamespacedAttributeKey resolveKey(String attributeId) {
        String normalized = attributeId.toLowerCase(java.util.Locale.ROOT);
        if (normalized.contains(".")) {
            String[] segments = normalized.split("\\.", 2);
            return new CommandParsingUtils.NamespacedAttributeKey(segments[0], segments[1]);
        }
        return new CommandParsingUtils.NamespacedAttributeKey(attributeUtils.getName().toLowerCase(java.util.Locale.ROOT), normalized);
    }

    private String criterionForSlot(EquipmentSlot slot) {
        return switch (slot) {
            case HEAD, CHEST, LEGS, FEET -> TriggerCriterion.EQUIPPED.key();
            case OFF_HAND -> TriggerCriterion.OFFHAND.key();
            case HAND -> TriggerCriterion.HELD.key();
            default -> TriggerCriterion.defaultCriterion().key();
        };
    }

    /**
     * Sets the drop chance on the entity equipment for the provided slot.
     */
    private void setDropChance(EntityEquipment equipment, EquipmentSlot slot, float chance) {
        switch (slot) {
            case HEAD -> equipment.setHelmetDropChance(chance);
            case CHEST -> equipment.setChestplateDropChance(chance);
            case LEGS -> equipment.setLeggingsDropChance(chance);
            case FEET -> equipment.setBootsDropChance(chance);
            case HAND -> equipment.setItemInMainHandDropChance(chance);
            case OFF_HAND -> equipment.setItemInOffHandDropChance(chance);
        }
    }

    /**
     * Maps the internal gear slot type to the corresponding Bukkit equipment slot.
     */
    private EquipmentSlot mapSlot(GearSlot slot) {
        return switch (slot) {
            case HELMET -> EquipmentSlot.HEAD;
            case CHESTPLATE -> EquipmentSlot.CHEST;
            case LEGGINGS -> EquipmentSlot.LEGS;
            case BOOTS -> EquipmentSlot.FEET;
            case HAND -> EquipmentSlot.HAND;
            case OFF_HAND -> EquipmentSlot.OFF_HAND;
        };
    }

    private String summarize(ItemStack stack) {
        if (stack == null) {
            return "null";
        }
        return stack.serialize().toString();
    }
}
