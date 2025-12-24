package com.baddcamden.attributeutils;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlot;

import java.util.Objects;
import java.util.UUID;

public final class AttributeDefinition {
    /** Attribute being mutated by this definition. */
    private final Attribute attribute;
    /** Operation used when applying the modifier (e.g., add vs. multiply). */
    private final AttributeModifier.Operation operation;
    /** Maximum absolute magnitude allowed for the modifier; NaN disables capping. */
    private final double cap;

    /**
     * Creates a new definition describing how an attribute should be modified, including the allowed cap.
     *
     * @param attribute  attribute to target
     * @param operation  math operation to apply when modifying
     * @param cap        maximum absolute amount permitted (NaN for uncapped)
     */
    public AttributeDefinition(Attribute attribute, AttributeModifier.Operation operation, double cap) {
        this.attribute = Objects.requireNonNull(attribute, "attribute");
        this.operation = Objects.requireNonNull(operation, "operation");
        this.cap = cap;
    }

    /** @return attribute affected by this definition. */
    public Attribute attribute() {
        return attribute;
    }

    /** @return operation used when applying the modifier. */
    public AttributeModifier.Operation operation() {
        return operation;
    }

    /** @return configured cap for the modifier amount. */
    public double cap() {
        return cap;
    }

    /**
     * Namespaced key for registering attribute modifiers.
     *
     * @return stable namespaced identifier
     */
    public String name() {
        return "attributeitemutils:" + attribute.getKey().getKey();
    }

    /**
     * Restricts the amount to the configured cap while allowing negative values.
     *
     * @param amount requested modifier amount
     * @return bounded amount respecting the cap
     */
    public double applyCap(double amount) {
        // VAGUE/IMPROVEMENT NEEDED cap does not guard against negative values, so a negative cap will invert bounds.
        if (Double.isNaN(cap)) {
            return amount;
        }
        double limited = Math.min(cap, amount);
        return Math.max(-cap, limited);
    }

    /**
     * Creates a new modifier instance using a bounded amount for the provided equipment slot.
     *
     * @param amount requested modifier amount
     * @param slot   equipment slot the modifier applies to
     * @return new attribute modifier respecting the configured cap
     */
    public AttributeModifier newModifier(double amount, EquipmentSlot slot) {
        double bounded = applyCap(amount);
        return new AttributeModifier(UUID.randomUUID(), name(), bounded, operation, slot);
    }
}
