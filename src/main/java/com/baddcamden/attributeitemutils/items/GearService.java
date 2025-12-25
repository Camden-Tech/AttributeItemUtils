package com.baddcamden.attributeitemutils.items;

import com.baddcamden.attributeitemutils.config.DropChanceConfigSource;
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
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
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
                       EntityAttributeHandler entityAttributeHandler) {
        this(loader, dropChanceConfigSource, chanceHooks, attributeUtils, attributeFacade, itemAttributeHandler, entityAttributeHandler, Logger.getLogger(GearService.class.getName()));
    }

    public GearService(GearConfigLoader loader,
                       DropChanceConfigSource dropChanceConfigSource,
                       EntityChanceHooks chanceHooks,
                       AttributeUtilitiesPlugin attributeUtils,
                       AttributeFacade attributeFacade,
                       ItemAttributeHandler itemAttributeHandler,
                       EntityAttributeHandler entityAttributeHandler,
                       Logger logger) {
        this.loader = loader;
        this.dropChanceConfigSource = dropChanceConfigSource;
        this.chanceHooks = chanceHooks;
        this.attributeUtils = attributeUtils;
        this.attributeFacade = attributeFacade;
        this.itemAttributeHandler = itemAttributeHandler;
        this.entityAttributeHandler = entityAttributeHandler;
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
                stack = buildAttributedItem(stack.getType(), equipmentSlot, nights);
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

    private ItemStack buildAttributedItem(Material material, EquipmentSlot slot, long nights) {
        List<AttributeDefinition> definitions = attributeFacade.getDefinitions().stream().toList();
        if (definitions.isEmpty()) {
            return new ItemStack(material);
        }

        List<CommandParsingUtils.AttributeDefinition> rolls = randomRolls(definitions, slot, nights);
        if (rolls.isEmpty()) {
            return new ItemStack(material);
        }

        ItemAttributeHandler.ItemBuildResult result = itemAttributeHandler.buildAttributeItem(material, rolls);
        return result.itemStack();
    }

    private List<CommandParsingUtils.AttributeDefinition> randomRolls(List<AttributeDefinition> definitions, EquipmentSlot slot, long nights) {
        int clampedNights = (int) Math.max(0, Math.min(nights, Integer.MAX_VALUE));
        int rollCount = Math.max(1, Math.min(3, 1 + clampedNights % 3));
        String criterion = criterionForSlot(slot);
        return random.ints(rollCount, 0, definitions.size())
                .mapToObj(definitions::get)
                .map(definition -> new CommandParsingUtils.AttributeDefinition(resolveKey(definition.id()), randomAmount(), null, criterion))
                .collect(Collectors.toList());
    }

    private double randomAmount() {
        return 0.25d + (1.25d * random.nextDouble());
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
