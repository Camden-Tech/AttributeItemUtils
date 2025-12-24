package com.baddcamden.attributeitemutils.hooks;

import com.baddcamden.attributeitemutils.config.AttributeConfig;
import com.baddcamden.attributeitemutils.config.EnchantmentConfig;
import org.bukkit.entity.EntityType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EntityChanceHooks {

    private final List<EntityChanceHook> hooks = new ArrayList<>();

    /**
     * Registers a new hook that can override attribute, enchantment, or drop chances for specific entity types.
     */
    public void register(EntityChanceHook hook) {
        hooks.add(hook);
    }

    /**
     * Resolves the first attribute config provided by registered hooks for the supplied entity type.
     */
    public Optional<AttributeConfig> attributeConfigFor(EntityType type) {
        return hooks.stream()
                .map(h -> h.attributeConfigFor(type))
                .flatMap(Optional::stream)
                .findFirst();
    }

    /**
     * Resolves the first enchantment config provided by registered hooks for the supplied entity type.
     */
    public Optional<EnchantmentConfig> enchantmentConfigFor(EntityType type) {
        return hooks.stream()
                .map(h -> h.enchantmentConfigFor(type))
                .flatMap(Optional::stream)
                .findFirst();
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
}
