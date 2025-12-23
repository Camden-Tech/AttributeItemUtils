package com.baddcamdne.attributeitemutils.items;

import com.baddcamdne.attributeitemutils.config.EnchantmentConfig;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class EnchantmentService {
    private final Random random;

    public EnchantmentService() {
        this(new Random());
    }

    EnchantmentService(Random random) {
        this.random = random;
    }

    public ItemStack applyEnchants(ItemStack stack, EnchantmentConfig config, int nights) {
        if (stack == null) return null;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;

        boolean changed = false;
        while (roll(config, nights)) {
            Enchantment enchantment = randomEnchantment(stack);
            if (enchantment == null) break;
            int level = meta.getEnchantLevel(enchantment) + config.levelBonus();
            meta.addEnchant(enchantment, level, true);
            changed = true;
        }

        if (changed) {
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            stack.setItemMeta(meta);
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
