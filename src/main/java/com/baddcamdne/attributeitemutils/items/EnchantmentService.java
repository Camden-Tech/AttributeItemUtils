package com.baddcamdne.attributeitemutils.items;

import com.baddcamdne.attributeitemutils.config.EnchantmentConfig;
import com.baddcamdne.attributeutils.AttributeFacade;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class EnchantmentService {
    private final AttributeFacade attributeFacade;
    private final Random random;

    public EnchantmentService(AttributeFacade attributeFacade) {
        this(attributeFacade, new Random());
    }

    EnchantmentService(AttributeFacade attributeFacade, Random random) {
        this.attributeFacade = attributeFacade;
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
        List<Enchantment> valid = Arrays.stream(Enchantment.values())
                .filter(e -> e.canEnchantItem(stack))
                .toList();
        if (valid.isEmpty()) return null;
        return valid.get(random.nextInt(valid.size()));
    }
}
