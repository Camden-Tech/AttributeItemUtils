package com.baddcamden.attributeitemutils.hooks;

import com.baddcamden.attributeitemutils.config.AttributeConfig;
import com.baddcamden.attributeitemutils.config.EnchantmentConfig;
import org.bukkit.entity.EntityType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Aggregates {@link EntityChanceHook} instances and surfaces the first non-empty response
 * for attribute, enchantment, or drop chance requests.
 */
public class EntityChanceHooks {

    /**
     * Registered hooks checked in insertion order.
     */
    private final List<EntityChanceHook> hooks = new ArrayList<>();

    /**
     * Registers an additional hook to consult.
     *
     * @param hook the hook to register
     */
    public void register(EntityChanceHook hook) {
        hooks.add(hook);
    }

    /**
     * Retrieves the first available attribute configuration for the provided entity type.
     *
     * @param type the entity being evaluated
     * @return the first non-empty attribute configuration provided by registered hooks
     */
    public Optional<AttributeConfig> attributeConfigFor(EntityType type) {
        return hooks.stream()
                .map(h -> h.attributeConfigFor(type))
                .flatMap(Optional::stream)
                .findFirst();
    }

    /**
     * Retrieves the first available enchantment configuration for the provided entity type.
     *
     * @param type the entity being evaluated
     * @return the first non-empty enchantment configuration provided by registered hooks
     */
    public Optional<EnchantmentConfig> enchantmentConfigFor(EntityType type) {
        return hooks.stream()
                .map(h -> h.enchantmentConfigFor(type))
                .flatMap(Optional::stream)
                .findFirst();
    }

    /**
     * Retrieves the first available drop chance for the provided entity type.
     *
     * @param type the entity being evaluated
     * @return the first non-empty drop chance provided by registered hooks
     */
    public Optional<Double> dropChanceFor(EntityType type) {
        return hooks.stream()
                .map(h -> h.dropChanceFor(type))
                .flatMap(Optional::stream)
                .findFirst();
    }
}
