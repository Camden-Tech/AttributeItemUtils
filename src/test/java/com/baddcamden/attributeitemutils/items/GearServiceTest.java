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
import com.baddcamden.attributeitemutils.hooks.EntityChanceHooks;
import com.baddcamden.attributeitemutils.util.NightCalculator;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GearServiceTest {

    @Test
    void clampsNightCountForLargeWorldTimes() {
        GearConfigLoader loader = mock(GearConfigLoader.class);
        AttributeService attributeService = mock(AttributeService.class);
        EnchantmentService enchantmentService = mock(EnchantmentService.class);
        AttributeConfigSource attributeConfigSource = new AttributeConfigSource(new AttributeConfig(0.1, 0.01, 0.05, 0.9));
        EnchantmentConfigSource enchantmentConfigSource = new EnchantmentConfigSource(new EnchantmentConfig(0.1, 0.01, 1, 0.9));
        DropChanceConfigSource dropChanceConfigSource = new DropChanceConfigSource(0.25);
        EntityChanceHooks chanceHooks = new EntityChanceHooks();

        GearService service = new GearService(loader, attributeService, enchantmentService, attributeConfigSource, enchantmentConfigSource, dropChanceConfigSource, chanceHooks);

        LivingEntity entity = mock(LivingEntity.class);
        World world = mock(World.class);
        EntityEquipment equipment = mock(EntityEquipment.class);
        when(entity.getWorld()).thenReturn(world);
        when(world.getFullTime()).thenReturn(Long.MAX_VALUE);
        when(entity.getType()).thenReturn(EntityType.ZOMBIE);
        when(entity.getEquipment()).thenReturn(equipment);

        KitConfig kit = new KitConfig("test", 0, 1, 1, Map.of(GearSlot.HELMET, List.of(new WeightedItem(Material.STONE, 1.0))));
        when(attributeService.applyAttributes(any(ItemStack.class), any(AttributeConfig.class), eq(EquipmentSlot.HEAD), anyLong()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(enchantmentService.applyEnchants(any(ItemStack.class), any(EnchantmentConfig.class), eq(EquipmentSlot.HEAD), anyLong()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.applyKit(entity, kit);

        ArgumentCaptor<Long> attributeNights = ArgumentCaptor.forClass(Long.class);
        verify(attributeService).applyAttributes(any(ItemStack.class), any(AttributeConfig.class), eq(EquipmentSlot.HEAD), attributeNights.capture());

        ArgumentCaptor<Long> enchantmentNights = ArgumentCaptor.forClass(Long.class);
        verify(enchantmentService).applyEnchants(any(ItemStack.class), any(EnchantmentConfig.class), eq(EquipmentSlot.HEAD), enchantmentNights.capture());

        long expectedNights = NightCalculator.clampNights(Long.MAX_VALUE / 24000L);
        assertEquals(expectedNights, attributeNights.getValue());
        assertEquals(expectedNights, enchantmentNights.getValue());
    }
}
