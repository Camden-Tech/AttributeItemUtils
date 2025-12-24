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
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GearServiceDropChanceTest {

    private static final AttributeConfigSource ATTRIBUTE_CONFIG = new AttributeConfigSource(new AttributeConfig(0, 0, 0, 0));
    private static final EnchantmentConfigSource ENCHANT_CONFIG = new EnchantmentConfigSource(new EnchantmentConfig(0, 0, 0, 0));

    @Test
    void setsDropChancesForEquippedItems() {
        DropChanceConfigSource dropChanceConfigSource = new DropChanceConfigSource(0.015);
        GearService service = createService(dropChanceConfigSource);
        LivingEntity entity = mockEntity(EntityType.SKELETON);
        EntityEquipment equipment = mock(EntityEquipment.class);
        when(entity.getEquipment()).thenReturn(equipment);

        KitConfig kit = kitWithGear(new WeightedItem(Material.DIAMOND_HELMET, 1), new WeightedItem(Material.IRON_SWORD, 1));

        service.applyKit(entity, kit);

        verify(equipment).setHelmetDropChance(0.015f);
        verify(equipment).setItemInMainHandDropChance(0.015f);
        verify(equipment, never()).setBootsDropChance(anyFloat());
    }

    @Test
    void appliesEntitySpecificDropChanceOverride() {
        DropChanceConfigSource dropChanceConfigSource = new DropChanceConfigSource(0.015, Map.of(EntityType.ZOMBIE, 0.03));
        GearService service = createService(dropChanceConfigSource);
        LivingEntity entity = mockEntity(EntityType.ZOMBIE);
        EntityEquipment equipment = mock(EntityEquipment.class);
        when(entity.getEquipment()).thenReturn(equipment);

        KitConfig kit = kitWithGear(new WeightedItem(Material.IRON_HELMET, 1), null);

        service.applyKit(entity, kit);

        verify(equipment).setHelmetDropChance(0.03f);
    }

    @Test
    void appliesAttributesToConfiguredEquipmentSlots() {
        DropChanceConfigSource dropChanceConfigSource = new DropChanceConfigSource(0.0);

        AttributeService attributeService = mock(AttributeService.class);
        when(attributeService.applyAttributes(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyInt()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        EnchantmentService enchantmentService = mock(EnchantmentService.class);
        when(enchantmentService.applyEnchants(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyInt()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        GearService service = new GearService(mock(GearConfigLoader.class), attributeService, enchantmentService, ATTRIBUTE_CONFIG, ENCHANT_CONFIG, dropChanceConfigSource);
        LivingEntity entity = mockEntity(EntityType.SKELETON);
        EntityEquipment equipment = mock(EntityEquipment.class);
        when(entity.getEquipment()).thenReturn(equipment);

        KitConfig kit = kitWithGear(new WeightedItem(Material.DIAMOND_HELMET, 1), new WeightedItem(Material.IRON_SWORD, 1), new WeightedItem(Material.SHIELD, 1));

        service.applyKit(entity, kit);

        verify(attributeService).applyAttributes(org.mockito.ArgumentMatchers.any(), eq(ATTRIBUTE_CONFIG.configFor(EntityType.SKELETON)), eq(org.bukkit.inventory.EquipmentSlot.HEAD), org.mockito.ArgumentMatchers.anyInt());
        verify(attributeService).applyAttributes(org.mockito.ArgumentMatchers.any(), eq(ATTRIBUTE_CONFIG.configFor(EntityType.SKELETON)), eq(org.bukkit.inventory.EquipmentSlot.HAND), org.mockito.ArgumentMatchers.anyInt());
        verify(attributeService).applyAttributes(org.mockito.ArgumentMatchers.any(), eq(ATTRIBUTE_CONFIG.configFor(EntityType.SKELETON)), eq(org.bukkit.inventory.EquipmentSlot.OFF_HAND), org.mockito.ArgumentMatchers.anyInt());
        verify(enchantmentService).applyEnchants(org.mockito.ArgumentMatchers.any(), eq(ENCHANT_CONFIG.configFor(EntityType.SKELETON)), eq(org.bukkit.inventory.EquipmentSlot.HEAD), org.mockito.ArgumentMatchers.anyInt());
        verify(enchantmentService).applyEnchants(org.mockito.ArgumentMatchers.any(), eq(ENCHANT_CONFIG.configFor(EntityType.SKELETON)), eq(org.bukkit.inventory.EquipmentSlot.HAND), org.mockito.ArgumentMatchers.anyInt());
        verify(enchantmentService).applyEnchants(org.mockito.ArgumentMatchers.any(), eq(ENCHANT_CONFIG.configFor(EntityType.SKELETON)), eq(org.bukkit.inventory.EquipmentSlot.OFF_HAND), org.mockito.ArgumentMatchers.anyInt());
    }

    private GearService createService(DropChanceConfigSource dropChanceConfigSource) {
        AttributeService attributeService = mock(AttributeService.class);
        when(attributeService.applyAttributes(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyInt()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        EnchantmentService enchantmentService = mock(EnchantmentService.class);
        when(enchantmentService.applyEnchants(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyInt()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        return new GearService(mock(GearConfigLoader.class), attributeService, enchantmentService, ATTRIBUTE_CONFIG, ENCHANT_CONFIG, dropChanceConfigSource);
    }

    private LivingEntity mockEntity(EntityType type) {
        LivingEntity entity = mock(LivingEntity.class);
        when(entity.getType()).thenReturn(type);

        World world = mock(World.class);
        when(world.getFullTime()).thenReturn(0L);
        when(entity.getWorld()).thenReturn(world);

        return entity;
    }

    private KitConfig kitWithGear(WeightedItem helmet, WeightedItem mainHand) {
        Map<GearSlot, List<WeightedItem>> items = new EnumMap<>(GearSlot.class);
        items.put(GearSlot.HELMET, helmet == null ? List.of() : List.of(helmet));
        items.put(GearSlot.CHESTPLATE, List.of());
        items.put(GearSlot.LEGGINGS, List.of());
        items.put(GearSlot.BOOTS, List.of());
        items.put(GearSlot.HAND, mainHand == null ? List.of() : List.of(mainHand));
        items.put(GearSlot.OFF_HAND, List.of());
        return new KitConfig("test", 0, 1, 1, items);
    }

    private KitConfig kitWithGear(WeightedItem helmet, WeightedItem mainHand, WeightedItem offHand) {
        Map<GearSlot, List<WeightedItem>> items = new EnumMap<>(GearSlot.class);
        items.put(GearSlot.HELMET, helmet == null ? List.of() : List.of(helmet));
        items.put(GearSlot.CHESTPLATE, List.of());
        items.put(GearSlot.LEGGINGS, List.of());
        items.put(GearSlot.BOOTS, List.of());
        items.put(GearSlot.HAND, mainHand == null ? List.of() : List.of(mainHand));
        items.put(GearSlot.OFF_HAND, offHand == null ? List.of() : List.of(offHand));
        return new KitConfig("test", 0, 1, 1, items);
    }
}
