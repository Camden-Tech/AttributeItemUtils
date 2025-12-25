package com.baddcamden.attributeitemutils.hooks;

import org.bukkit.entity.EntityType;

import java.util.Optional;

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
}
