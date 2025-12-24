package com.baddcamden.attributeitemutils.hooks;

import com.baddcamden.attributeitemutils.config.AttributeConfig;
import com.baddcamden.attributeitemutils.config.EnchantmentConfig;
import org.bukkit.entity.EntityType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EntityChanceHooks {

    private final List<EntityChanceHook> hooks = new ArrayList<>();

    public void register(EntityChanceHook hook) {
        hooks.add(hook);
    }

    public Optional<AttributeConfig> attributeConfigFor(EntityType type) {
        return hooks.stream()
                .map(h -> h.attributeConfigFor(type))
                .flatMap(Optional::stream)
                .findFirst();
    }

    public Optional<EnchantmentConfig> enchantmentConfigFor(EntityType type) {
        return hooks.stream()
                .map(h -> h.enchantmentConfigFor(type))
                .flatMap(Optional::stream)
                .findFirst();
    }

    public Optional<Double> dropChanceFor(EntityType type) {
        return hooks.stream()
                .map(h -> h.dropChanceFor(type))
                .flatMap(Optional::stream)
                .findFirst();
    }
}
