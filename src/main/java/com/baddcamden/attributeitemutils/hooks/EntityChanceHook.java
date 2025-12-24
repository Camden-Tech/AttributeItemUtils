package com.baddcamden.attributeitemutils.hooks;

import com.baddcamden.attributeitemutils.config.AttributeConfig;
import com.baddcamden.attributeitemutils.config.EnchantmentConfig;
import org.bukkit.entity.EntityType;

import java.util.Optional;

/**
 * Extension point for providing attribute and enchantment configuration by entity type.
 * Implementations may override the defaults to supply specialized behavior and should
 * return {@link Optional#empty()} when they do not handle a given entity.
 */
public interface EntityChanceHook {

    /**
     * Supplies attribute configuration for a given entity type.
     *
     * @param type the entity being evaluated
     * @return an optional configuration when the implementation supports the type
     */
    default Optional<AttributeConfig> attributeConfigFor(EntityType type) {
        return Optional.empty();
    }

    /**
     * Supplies enchantment configuration for a given entity type.
     *
     * @param type the entity being evaluated
     * @return an optional configuration when the implementation supports the type
     */
    default Optional<EnchantmentConfig> enchantmentConfigFor(EntityType type) {
        return Optional.empty();
    }

    /**
     * Supplies a drop chance adjustment for a given entity type.
     *
     * @param type the entity being evaluated
     * @return an optional drop chance when the implementation supports the type
     */
    default Optional<Double> dropChanceFor(EntityType type) {
        return Optional.empty();
    }
}
