package com.baddcamden.attributeitemutils.items;

import com.baddcamden.attributeitemutils.config.EnchantmentConfig;
import com.baddcamden.attributeitemutils.config.EnchantmentPoolConfig;
import com.baddcamden.attributeutils.AttributeFacade;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EnchantmentServiceTest {

    @Test
    void terminatesWhenMaxChanceIsGuaranteed() {
        AttributeFacade facade = mock(AttributeFacade.class);
        Enchantment enchantment = mock(Enchantment.class);
        ItemStack stack = mock(ItemStack.class);
        when(enchantment.canEnchantItem(stack)).thenReturn(true);
        when(stack.getEnchantmentLevel(enchantment)).thenReturn(0);

        EnchantmentPoolConfig poolConfig = new EnchantmentPoolConfig(List.of(enchantment));
        EnchantmentConfig config = new EnchantmentConfig(1.0, 0.0, 1, 1.0);
        Random alwaysSuccessful = new Random() {
            @Override
            public double nextDouble() {
                return 0.0d;
            }

            @Override
            public int nextInt(int bound) {
                return 0;
            }
        };

        EnchantmentService service = new EnchantmentService(facade, poolConfig, alwaysSuccessful);

        service.applyEnchants(stack, config, EquipmentSlot.HAND, 0);

        verify(facade, times(1)).applyEnchant(stack, enchantment, 1);
    }

    @Test
    void stopsWhenNoFurtherChangesCanBeApplied() {
        AttributeFacade facade = mock(AttributeFacade.class);
        Enchantment enchantment = mock(Enchantment.class);
        ItemStack stack = mock(ItemStack.class);
        when(enchantment.canEnchantItem(stack)).thenReturn(true);
        when(stack.getEnchantmentLevel(enchantment)).thenReturn(2);

        EnchantmentPoolConfig poolConfig = new EnchantmentPoolConfig(List.of(enchantment));
        EnchantmentConfig config = new EnchantmentConfig(1.0, 0.0, 0, 1.0);
        Random alwaysSuccessful = new Random() {
            @Override
            public double nextDouble() {
                return 0.0d;
            }

            @Override
            public int nextInt(int bound) {
                return 0;
            }
        };

        EnchantmentService service = new EnchantmentService(facade, poolConfig, alwaysSuccessful);

        service.applyEnchants(stack, config, EquipmentSlot.HAND, 0);

        verify(facade, never()).applyEnchant(stack, enchantment, 2);
    }

    @Test
    void returnsEarlyWhenNoEnchantmentsAreApplicable() {
        AttributeFacade facade = mock(AttributeFacade.class);
        Enchantment enchantment = mock(Enchantment.class);
        ItemStack stack = mock(ItemStack.class);
        when(enchantment.canEnchantItem(stack)).thenReturn(false);

        EnchantmentPoolConfig poolConfig = new EnchantmentPoolConfig(List.of(enchantment));
        EnchantmentConfig config = new EnchantmentConfig(1.0, 0.0, 1, 1.0);
        Random alwaysSuccessful = new Random() {
            @Override
            public double nextDouble() {
                return 0.0d;
            }

            @Override
            public int nextInt(int bound) {
                return 0;
            }
        };

        EnchantmentService service = new EnchantmentService(facade, poolConfig, alwaysSuccessful);

        service.applyEnchants(stack, config, EquipmentSlot.HAND, 0);

        verify(facade, never()).applyEnchant(stack, enchantment, 1);
    }
}
