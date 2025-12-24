package com.baddcamdne.attributeitemutils.items;

import com.baddcamdne.attributeitemutils.config.AttributeConfig;
import com.baddcamdne.attributeitemutils.config.AttributeConfigSource;
import com.baddcamdne.attributeitemutils.config.DropChanceConfigSource;
import com.baddcamdne.attributeitemutils.config.EnchantmentConfig;
import com.baddcamdne.attributeitemutils.config.EnchantmentConfigSource;
import com.baddcamdne.attributeitemutils.gear.GearConfigLoader;
import com.baddcamdne.attributeitemutils.gear.GearSlot;
import com.baddcamdne.attributeitemutils.gear.KitConfig;
import com.baddcamdne.attributeitemutils.gear.WeightedItem;
import com.baddcamdne.attributeitemutils.util.BellCurveSelector;
import com.baddcamdne.attributeitemutils.hooks.EntityChanceHooks;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

public class GearService {
    private final GearConfigLoader loader;
    private final AttributeService attributeService;
    private final EnchantmentService enchantmentService;
    private final AttributeConfigSource attributeConfigSource;
    private final EnchantmentConfigSource enchantmentConfigSource;
    private final DropChanceConfigSource dropChanceConfigSource;
    private final EntityChanceHooks chanceHooks;
    private final BellCurveSelector selector = new BellCurveSelector();
    public GearService(GearConfigLoader loader,
                       AttributeService attributeService,
                       EnchantmentService enchantmentService,
                       AttributeConfigSource attributeConfigSource,
                       EnchantmentConfigSource enchantmentConfigSource,
                       DropChanceConfigSource dropChanceConfigSource,
                       EntityChanceHooks chanceHooks) {
        this.loader = loader;
        this.attributeService = attributeService;
        this.enchantmentService = enchantmentService;
        this.attributeConfigSource = attributeConfigSource;
        this.enchantmentConfigSource = enchantmentConfigSource;
        this.dropChanceConfigSource = dropChanceConfigSource;
        this.chanceHooks = chanceHooks;
    }

    public Optional<KitConfig> getKit(String name) {
        return loader.getKit(name);
    }

    public void applyKit(LivingEntity entity, KitConfig kit) {
        int nights = (int) (entity.getWorld().getFullTime() / 24000L);
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
            if (stack != null) {
                stack = attributeService.applyAttributes(stack, attributeConfig, equipmentSlot, nights);
                stack = enchantmentService.applyEnchants(stack, enchantmentConfig, equipmentSlot, nights);
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
    }

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
}
