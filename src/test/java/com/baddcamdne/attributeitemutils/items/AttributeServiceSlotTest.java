package com.baddcamdne.attributeitemutils.items;

import com.baddcamdne.attributeutils.AttributeFacade;
import org.bukkit.Material;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AttributeServiceSlotTest {
    private final AttributeService service = new AttributeService(new AttributeFacade(), Map.of(), Map.of(), new Random());

    @Test
    void assignsHelmetSlot() {
        assertEquals(EquipmentSlot.HEAD, service.determineSlot(new ItemStack(Material.DIAMOND_HELMET)));
    }

    @Test
    void assignsChestSlot() {
        assertEquals(EquipmentSlot.CHEST, service.determineSlot(new ItemStack(Material.IRON_CHESTPLATE)));
    }

    @Test
    void assignsLegSlot() {
        assertEquals(EquipmentSlot.LEGS, service.determineSlot(new ItemStack(Material.NETHERITE_LEGGINGS)));
    }

    @Test
    void assignsFeetSlot() {
        assertEquals(EquipmentSlot.FEET, service.determineSlot(new ItemStack(Material.LEATHER_BOOTS)));
    }

    @Test
    void assignsOffHandSlotForShieldLikeItems() {
        assertEquals(EquipmentSlot.OFF_HAND, service.determineSlot(new ItemStack(Material.SHIELD)));
    }

    @Test
    void defaultsToMainHand() {
        assertEquals(EquipmentSlot.HAND, service.determineSlot(new ItemStack(Material.DIAMOND_SWORD)));
    }
}
