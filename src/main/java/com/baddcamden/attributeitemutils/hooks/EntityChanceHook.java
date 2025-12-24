package com.baddcamden.attributeitemutils.hooks;

import com.baddcamden.attributeitemutils.config.AttributeConfig;
import com.baddcamden.attributeitemutils.config.EnchantmentConfig;
import org.bukkit.entity.EntityType;

import java.util.Optional;

public interface EntityChanceHook {
    default Optional<AttributeConfig> attributeConfigFor(EntityType type) {
        return Optional.empty();
    }

    default Optional<EnchantmentConfig> enchantmentConfigFor(EntityType type) {
        return Optional.empty();
    }

    default Optional<Double> dropChanceFor(EntityType type) {
        return Optional.empty();
    }
}
