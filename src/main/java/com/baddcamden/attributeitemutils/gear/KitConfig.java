package com.baddcamden.attributeitemutils.gear;

import java.util.List;
import java.util.Map;

/**
 * Complete definition of a gear kit including distribution controls and slot-specific weights.
 */
public record KitConfig(String name,
                        double targetWeight,
                        double steepness,
                        double range,
                        Map<GearSlot, List<WeightedItem>> items) {
}
