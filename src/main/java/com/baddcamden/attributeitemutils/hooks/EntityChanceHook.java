package com.baddcamden.attributeitemutils.hooks;

import org.bukkit.entity.EntityType;

import java.util.Optional;

/**
 * Extension point allowing integrations to override drop, attribute, and enchant chances for
 * specific entity types.
 */
public interface EntityChanceHook {
    /**
     * Provides an equipment drop chance override for the specified entity type.
     *
     * @param type entity requesting drop chances
     * @return configured drop chance if present
     */
    default Optional<Double> dropChanceFor(EntityType type) {
        return Optional.empty();
    }

    /**
     * Provides an attribute roll chance override for the specified entity type.
     *
     * @param type entity requesting attribute chance
     * @return configured attribute chance if present
     */
    default Optional<Double> attributeChanceFor(EntityType type) {
        return Optional.empty();
    }

    /**
     * Provides an enchantment roll chance override for the specified entity type.
     *
     * @param type entity requesting enchant chance
     * @return configured enchant chance if present
     */
    default Optional<Double> enchantChanceFor(EntityType type) {
        return Optional.empty();
    }
}
