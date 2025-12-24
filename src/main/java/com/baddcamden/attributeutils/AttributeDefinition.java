package com.baddcamden.attributeutils;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlot;

import java.util.Objects;
import java.util.UUID;

public final class AttributeDefinition {
    private final Attribute attribute;
    private final AttributeModifier.Operation operation;
    private final double cap;

    public AttributeDefinition(Attribute attribute, AttributeModifier.Operation operation, double cap) {
        this.attribute = Objects.requireNonNull(attribute, "attribute");
        this.operation = Objects.requireNonNull(operation, "operation");
        this.cap = cap;
    }

    public Attribute attribute() {
        return attribute;
    }

    public AttributeModifier.Operation operation() {
        return operation;
    }

    public double cap() {
        return cap;
    }

    public String name() {
        return "attributeitemutils:" + attribute.getKey().getKey();
    }

    public double applyCap(double amount) {
        if (Double.isNaN(cap)) {
            return amount;
        }
        double limited = Math.min(cap, amount);
        return Math.max(-cap, limited);
    }

    public AttributeModifier newModifier(double amount, EquipmentSlot slot) {
        double bounded = applyCap(amount);
        return new AttributeModifier(UUID.randomUUID(), name(), bounded, operation, slot);
    }
}
