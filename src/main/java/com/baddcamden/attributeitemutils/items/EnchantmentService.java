package com.baddcamden.attributeitemutils.items;

import com.baddcamden.attributeitemutils.config.EnchantmentConfig;
import com.baddcamden.attributeitemutils.config.EnchantmentPoolConfig;
import com.baddcamden.attributeutils.AttributeFacade;
import com.baddcamden.attributeitemutils.util.NightCalculator;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Random;

public class EnchantmentService {
    // Performs attribute-aware interactions such as applying enchantments that respect custom modifiers.
    private final AttributeFacade attributeFacade;
    // Centralized RNG so enchantment rolls can be deterministically tested when seeded.
    private final Random random;
    // Provides the list of allowed enchantments and their weighting for selection.
    private final EnchantmentPoolConfig enchantmentPool;

    /**
     * Creates an enchantment service with default randomness to apply enchants from the configured pool.
     */
    public EnchantmentService(AttributeFacade attributeFacade, EnchantmentPoolConfig enchantmentPool) {
        this(attributeFacade, enchantmentPool, new Random());
    }

    /**
     * Package-private constructor that accepts a seeded random instance for testing purposes.
     */
    EnchantmentService(AttributeFacade attributeFacade, EnchantmentPoolConfig enchantmentPool, Random random) {
        this.attributeFacade = attributeFacade;
        this.enchantmentPool = enchantmentPool;
        this.random = random;
    }

    /**
     * Applies random enchants to the given item stack while the roll succeeds, increasing existing levels with a bonus.
     */
    public ItemStack applyEnchants(ItemStack stack, EnchantmentConfig config, EquipmentSlot slot, long nights) {
        if (stack == null) {
            return null;
        }
        if (config.levelBonus() <= 0) {
            return stack;
        }

        List<Enchantment> validEnchantments = validEnchantments(stack);
        if (validEnchantments.isEmpty()) {
            return stack;
        }

        int remainingApplications = Math.max(1, validEnchantments.size());
        //VAGUE/IMPROVEMENT NEEDED {Number of potential enchant applications tied to available enchant count without design note}
        while (remainingApplications-- > 0 && roll(config, nights)) {
            Enchantment enchantment = validEnchantments.get(random.nextInt(validEnchantments.size()));
            int currentLevel = stack.getEnchantmentLevel(enchantment);
            int level = currentLevel + config.levelBonus();
            if (level <= currentLevel) break;
            attributeFacade.applyEnchant(stack, enchantment, level);
        }

        return stack;
    }

    /**
     * Determines whether to apply another enchantment based on nightly scaling probability.
     */
    boolean roll(EnchantmentConfig config, long nights) {
        long clampedNights = NightCalculator.clampNights(nights);
        double chance = Math.min(config.maxChance(), config.baseChance() + (config.nightlyIncrease() * clampedNights));
        double effectiveChance = Math.min(chance, Math.nextAfter(1.0d, 0.0d));
        return random.nextDouble() < effectiveChance;
    }

    private List<Enchantment> validEnchantments(ItemStack stack) {
        return enchantmentPool.enchantments()
                .stream()
                .filter(e -> e.canEnchantItem(stack))
                .toList();
    }
}
