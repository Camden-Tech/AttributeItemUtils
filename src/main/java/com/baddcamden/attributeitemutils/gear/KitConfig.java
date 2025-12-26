package com.baddcamden.attributeitemutils.gear;

import org.bukkit.attribute.AttributeModifier;

import java.util.List;
import java.util.Map;

/**
 * Defines a weighted kit of gear entries and tuning parameters for bell-curve item selection.
 */
public record KitConfig(String name,
                        double targetWeight,
                        double steepness,
                        double range,
                        Map<GearSlot, List<WeightedItem>> items,
                        Map<String, AttributeModifier.Operation> attributeOperations) {
}
