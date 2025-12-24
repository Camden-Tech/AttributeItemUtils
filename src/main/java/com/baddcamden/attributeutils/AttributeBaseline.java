package com.baddcamden.attributeutils;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlot;

import java.util.UUID;

/**
 * Represents the base modifier for an attribute before any runtime mutations are applied.
 */
public record AttributeBaseline(Attribute attribute, double baseAmount) {

    public AttributeModifier asModifier(AttributeDefinition definition, EquipmentSlot slot) {
        double bounded = definition.applyCap(baseAmount);
        UUID id = UUID.nameUUIDFromBytes((attribute.getKey().toString() + "-baseline").getBytes());
        return new AttributeModifier(id, definition.name(), bounded, definition.operation(), slot);
    }
}
