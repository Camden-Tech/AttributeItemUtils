package com.baddcamden.attributeitemutils.hooks;

import com.baddcamden.attributeitemutils.config.AttributeConfig;
import com.baddcamden.attributeitemutils.config.EnchantmentConfig;
import org.bukkit.entity.EntityType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class EntityChanceHooks {

    /**
     * Holds registered hooks in the order they should be consulted when resolving values.
     * //VAGUE/IMPROVEMENT NEEDED {The intended priority model between hooks is unclear and currently depends on registration order.}
     */
    private final List<EntityChanceHook> hooks = new ArrayList<>();

    /**
     * Registers a new hook for later resolution.
     */
    public void register(EntityChanceHook hook) {
        hooks.add(hook);
    }

    /**
     * Returns the first attribute configuration provided by the registered hooks for the given entity type.
     */
    public Optional<AttributeConfig> attributeConfigFor(EntityType type) {
        return resolveFirst(h -> h.attributeConfigFor(type));
    }

    /**
     * Returns the first enchantment configuration provided by the registered hooks for the given entity type.
     */
    public Optional<EnchantmentConfig> enchantmentConfigFor(EntityType type) {
        return resolveFirst(h -> h.enchantmentConfigFor(type));
    }

    /**
     * Returns the first drop chance provided by the registered hooks for the given entity type.
     */
    public Optional<Double> dropChanceFor(EntityType type) {
        return resolveFirst(h -> h.dropChanceFor(type));
    }

    private <T> Optional<T> resolveFirst(Function<EntityChanceHook, Optional<T>> extractor) {
        for (EntityChanceHook hook : hooks) {
            Optional<T> result = extractor.apply(hook);
            if (result.isPresent()) {
                return result;
            }
        }

        return Optional.empty();
    }
}
