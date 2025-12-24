package com.baddcamden.attributeitemutils.hooks;

import com.baddcamden.attributeitemutils.config.AttributeConfig;
import com.baddcamden.attributeitemutils.config.EnchantmentConfig;
import org.bukkit.entity.EntityType;

import java.util.Optional;

public interface EntityChanceHook {
    /**
     * Provides an attribute configuration override for the specified entity type.
     *
     * @param type entity requesting attribute chances
     * @return configured attribute chances if present
     */
    default Optional<AttributeConfig> attributeConfigFor(EntityType type) {
        return Optional.empty();
    }

    /**
     * Provides an enchantment configuration override for the specified entity type.
     *
     * @param type entity requesting enchantment chances
     * @return configured enchantment chances if present
     */
    default Optional<EnchantmentConfig> enchantmentConfigFor(EntityType type) {
        return Optional.empty();
    }

    /**
     * Provides an equipment drop chance override for the specified entity type.
     *
     * @param type entity requesting drop chances
     * @return configured drop chance if present
     */
    default Optional<Double> dropChanceFor(EntityType type) {
        return Optional.empty();
    }
}
