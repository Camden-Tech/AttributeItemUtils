package com.baddcamden.attributeitemutils.items;

import com.baddcamden.attributeitemutils.config.AttributeConfig;
import com.baddcamden.attributeitemutils.config.AttributeConfigSource;
import com.baddcamden.attributeitemutils.config.DropChanceConfigSource;
import com.baddcamden.attributeitemutils.config.EnchantmentConfig;
import com.baddcamden.attributeitemutils.config.EnchantmentConfigSource;
import com.baddcamden.attributeitemutils.gear.GearConfigLoader;
import com.baddcamden.attributeitemutils.gear.GearSlot;
import com.baddcamden.attributeitemutils.gear.KitConfig;
import com.baddcamden.attributeitemutils.gear.WeightedItem;
import com.baddcamden.attributeitemutils.util.BellCurveSelector;
import com.baddcamden.attributeitemutils.util.NightCalculator;
import com.baddcamden.attributeitemutils.hooks.EntityChanceHooks;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

public class GearService {
    // Loads configured kits and weighted item selections from disk.
    private final GearConfigLoader loader;
    // Applies attribute modifiers and related lore for generated gear.
    private final AttributeService attributeService;
    // Adds enchantments according to pool rules and configured bonuses.
    private final EnchantmentService enchantmentService;
    // Supplies attribute rolling parameters (chance, bonus, scaling) for entities.
    private final AttributeConfigSource attributeConfigSource;
    // Supplies enchantment rolling parameters for entities.
    private final EnchantmentConfigSource enchantmentConfigSource;
    // Provides drop chance defaults and overrides for entity equipment.
    private final DropChanceConfigSource dropChanceConfigSource;
    // Publishes hooks that may override attribute/enchant/drop settings per entity type.
    private final EntityChanceHooks chanceHooks;
    private final BellCurveSelector selector = new BellCurveSelector();
    private final Logger logger;
    /**
     * Constructs the gear service with all supporting services and config sources used when populating entity equipment.
     */
    public GearService(GearConfigLoader loader,
                       AttributeService attributeService,
                       EnchantmentService enchantmentService,
                       AttributeConfigSource attributeConfigSource,
                       EnchantmentConfigSource enchantmentConfigSource,
                       DropChanceConfigSource dropChanceConfigSource,
                       EntityChanceHooks chanceHooks) {
        this(loader, attributeService, enchantmentService, attributeConfigSource, enchantmentConfigSource, dropChanceConfigSource, chanceHooks, Logger.getLogger(GearService.class.getName()));
    }

    public GearService(GearConfigLoader loader,
                       AttributeService attributeService,
                       EnchantmentService enchantmentService,
                       AttributeConfigSource attributeConfigSource,
                       EnchantmentConfigSource enchantmentConfigSource,
                       DropChanceConfigSource dropChanceConfigSource,
                       EntityChanceHooks chanceHooks,
                       Logger logger) {
        this.loader = loader;
        this.attributeService = attributeService;
        this.enchantmentService = enchantmentService;
        this.attributeConfigSource = attributeConfigSource;
        this.enchantmentConfigSource = enchantmentConfigSource;
        this.dropChanceConfigSource = dropChanceConfigSource;
        this.chanceHooks = chanceHooks;
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
        AttributeConfig attributeConfig = chanceHooks.attributeConfigFor(entity.getType())
                .orElse(attributeConfigSource.defaultConfig());
        EnchantmentConfig enchantmentConfig = chanceHooks.enchantmentConfigFor(entity.getType())
                .orElse(enchantmentConfigSource.defaultConfig());
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
                logger.info("[GEAR] Pre-attribute modifiers for " + equipmentSlot + ": " + summarize(stack));
                stack = attributeService.applyAttributes(stack, attributeConfig, equipmentSlot, nights);
                logger.info("[GEAR] After attributes " + equipmentSlot + ": " + summarize(stack));
                stack = enchantmentService.applyEnchants(stack, enchantmentConfig, equipmentSlot, nights);
                logger.info("[GEAR] After enchants " + equipmentSlot + ": " + summarize(stack));
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
        if (entity instanceof Player player) {
            player.updateInventory();
        }
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
