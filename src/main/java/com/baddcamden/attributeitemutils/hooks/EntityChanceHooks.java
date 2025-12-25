package com.baddcamden.attributeitemutils.hooks;

import org.bukkit.entity.EntityType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EntityChanceHooks {

    private final List<EntityChanceHook> hooks = new ArrayList<>();

    /**
     * Registers a new hook that can override drop chances for specific entity types.
     */
    public void register(EntityChanceHook hook) {
        hooks.add(hook);
    }

    /**
     * Resolves the first drop chance provided by registered hooks for the supplied entity type.
     */
    public Optional<Double> dropChanceFor(EntityType type) {
        return hooks.stream()
                .map(h -> h.dropChanceFor(type))
                .flatMap(Optional::stream)
                .findFirst();
    }

    /**
     * Resolves the first attribute chance provided by registered hooks for the supplied entity type.
     */
    public Optional<Double> attributeChanceFor(EntityType type) {
        return hooks.stream()
                .map(h -> h.attributeChanceFor(type))
                .flatMap(Optional::stream)
                .findFirst();
    }

    /**
     * Resolves the first enchantment chance provided by registered hooks for the supplied entity type.
     */
    public Optional<Double> enchantChanceFor(EntityType type) {
        return hooks.stream()
                .map(h -> h.enchantChanceFor(type))
                .flatMap(Optional::stream)
                .findFirst();
    }
}
