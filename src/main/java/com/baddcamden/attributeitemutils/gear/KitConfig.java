package com.baddcamden.attributeitemutils.gear;

import me.baddcamden.attributeutils.model.ModifierOperation;

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
                        Map<String, ModifierOperation> attributeOperations) {
}
