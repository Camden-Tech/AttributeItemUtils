package com.baddcamden.attributeitemutils.items;

import com.baddcamden.attributeitemutils.config.EnchantmentConfig;
import com.baddcamden.attributeitemutils.config.EnchantmentPoolConfig;
import com.baddcamden.attributeutils.AttributeFacade;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Random;

public class EnchantmentService {
    private final AttributeFacade attributeFacade;
    private final Random random;
    private final EnchantmentPoolConfig enchantmentPool;

    public EnchantmentService(AttributeFacade attributeFacade, EnchantmentPoolConfig enchantmentPool) {
        this(attributeFacade, enchantmentPool, new Random());
    }

    EnchantmentService(AttributeFacade attributeFacade, EnchantmentPoolConfig enchantmentPool, Random random) {
        this.attributeFacade = attributeFacade;
        this.enchantmentPool = enchantmentPool;
        this.random = random;
    }

    public ItemStack applyEnchants(ItemStack stack, EnchantmentConfig config, EquipmentSlot slot, int nights) {
        if (stack == null) return null;
        while (roll(config, nights)) {
            Enchantment enchantment = randomEnchantment(stack);
            if (enchantment == null) break;
            int level = stack.getEnchantmentLevel(enchantment) + config.levelBonus();
            attributeFacade.applyEnchant(stack, enchantment, level);
        }

        return stack;
    }

    boolean roll(EnchantmentConfig config, int nights) {
        double chance = Math.min(config.maxChance(), config.baseChance() + (config.nightlyIncrease() * nights));
        return random.nextDouble() < chance;
    }

    private Enchantment randomEnchantment(ItemStack stack) {
        List<Enchantment> valid = enchantmentPool.enchantments()
                .stream()
                .filter(e -> e.canEnchantItem(stack))
                .toList();
        if (valid.isEmpty()) return null;
        return valid.get(random.nextInt(valid.size()));
    }
}
